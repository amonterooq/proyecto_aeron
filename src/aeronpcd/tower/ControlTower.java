package aeronpcd.tower;

import aeronpcd.model.Airplane;
import aeronpcd.model.FlightStatus;
import aeronpcd.model.Request;
import aeronpcd.resources.Gate;
import aeronpcd.resources.Runway;
import aeronpcd.util.AirportState;
import aeronpcd.util.FlightPanel; // <--- Importamos el nuevo panel
import aeronpcd.util.Logger;
import aeronpcd.gui.AirportWindow;

import java.util.LinkedList;
import java.util.List;

public class ControlTower {
    private List<Runway> runways;
    private List<Gate> gates;
    private AirportWindow window;

    private LinkedList<Request> requestQueue;
    private Operator[] operators;

    public ControlTower(List<Runway> runways, List<Gate> gates) {
        this.runways = runways;
        this.gates = gates;
        this.requestQueue = new LinkedList<>();

        this.operators = new Operator[5];
        for (int i = 0; i < 5; i++) {
            operators[i] = new Operator("OP-" + (i + 1));
            operators[i].start();
        }
    }

    public void setWindow(AirportWindow window) {
        this.window = window;
        updateVisualization();
    }

    public void addRequest(Request request) {
        synchronized (requestQueue) {
            requestQueue.add(request);
            log("Torre de control",
                    "Recibida solicitud " + request.getType() + " de Avión [" + request.getPlane().getId() + "]");
            updateVisualization();
            requestQueue.notifyAll();
        }
    }

    private class Operator extends Thread {
        public Operator(String name) {
            setName(name);
            setDaemon(true);
        }

        @Override
        public void run() {
            while (true) {
                Request req;
                synchronized (requestQueue) {
                    while (requestQueue.isEmpty()) {
                        try {
                            requestQueue.wait();
                        } catch (InterruptedException e) {
                            return;
                        }
                    }
                    req = requestQueue.poll();
                    updateVisualization();
                }

                log("Torre de control",
                        "Operario [" + getName() + "] atiende " + req.getType() + " para " + req.getPlane().getId());

                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                }

                boolean success = processRequestLogic(req);

                if (success) {
                    log("Torre de control", "Operario [" + getName() + "] FINALIZÓ " + req.getType() + " para "
                            + req.getPlane().getId());
                    synchronized (req.getPlane()) {
                        req.getPlane().notify();
                    }
                } else {
                    log("Torre de control", "Operario [" + getName() + "] RE-ENCOLA " + req.getType() + " para "
                            + req.getPlane().getId() + " (Sin Recursos)");
                    synchronized (requestQueue) {
                        requestQueue.add(req);
                        updateVisualization();
                        requestQueue.notifyAll();
                    }
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                    }
                }
            }
        }
    }

    // --- LÓGICA CON ACTUALIZACIÓN DEL JSON ---
    private boolean processRequestLogic(Request req) {
        Airplane plane = req.getPlane();
        String planeId = plane.getId();

        synchronized (this) {
            switch (req.getType()) {
                case LANDING:
                    Runway r = getFreeRunway();
                    Gate g = getFreeGate();

                    if (r != null && g != null) {
                        r.occupy(planeId);
                        g.occupy(planeId);

                        plane.setStatus(FlightStatus.LANDING_ASSIGNED);
                        plane.setResources(r, g);

                        // ACTUALIZAR JSON
                        FlightPanel.updateStatus(planeId, "LANDING_ASSIGNED");

                        log("Torre de control",
                                "Asignados: Pista " + r.getId() + " y Puerta " + g.getId() + " a " + planeId);
                        updateVisualization();
                        return true;
                    } else {
                        return false;
                    }

                case LANDED:
                    if (plane.getCurrentRunway() != null) {
                        plane.getCurrentRunway().free();
                        log("Torre de control",
                                "Pista [" + plane.getCurrentRunway().getId() + "] liberada por " + planeId);
                    }
                    plane.setStatus(FlightStatus.LANDED);

                    // ACTUALIZAR JSON
                    FlightPanel.updateStatus(planeId, "LANDED");

                    updateVisualization();
                    return true;

                case BOARDING:
                    plane.setStatus(FlightStatus.BOARDING);

                    // ACTUALIZAR JSON
                    FlightPanel.updateStatus(planeId, "BOARDING");

                    return true;

                case BOARDED:
                    if (plane.getCurrentGate() != null) {
                        plane.getCurrentGate().free();
                        log("Torre de control",
                                "Puerta [" + plane.getCurrentGate().getId() + "] liberada por " + planeId);
                    }
                    plane.setStatus(FlightStatus.BOARDED);

                    // ACTUALIZAR JSON
                    FlightPanel.updateStatus(planeId, "BOARDED");

                    updateVisualization();
                    return true;

                case TAKEOFF:
                    Runway rTakeoff = getFreeRunway();
                    if (rTakeoff != null) {
                        rTakeoff.occupy(planeId);
                        plane.setStatus(FlightStatus.TAKEOFF_ASSIGNED);
                        plane.setTakeoffRunway(rTakeoff);

                        // ACTUALIZAR JSON
                        FlightPanel.updateStatus(planeId, "TAKEOFF_ASSIGNED");

                        log("Torre de control", "Asignada Pista " + rTakeoff.getId() + " para despegue de " + planeId);
                        updateVisualization();
                        return true;
                    } else {
                        return false;
                    }

                case DEPARTED:
                    if (plane.getCurrentRunway() != null) {
                        plane.getCurrentRunway().free();
                        log("Torre de control", "Pista [" + plane.getCurrentRunway().getId() + "] liberada (Fin ciclo "
                                + planeId + ")");
                    }
                    plane.setStatus(FlightStatus.DEPARTED);

                    // ACTUALIZAR JSON
                    FlightPanel.updateStatus(planeId, "DEPARTED");

                    updateVisualization();
                    return true;
            }
        }
        return true;
    }

    private void updateVisualization() {
        if (window != null) {
            String htmlState;
            synchronized (requestQueue) {
                htmlState = AirportState.getHTMLState(runways, gates, requestQueue);
            }
            window.updateState(htmlState);
        }
    }

    private Runway getFreeRunway() {
        for (Runway r : runways)
            if (r.isAvailable())
                return r;
        return null;
    }

    private Gate getFreeGate() {
        for (Gate g : gates)
            if (!g.isOccupied())
                return g;
        return null;
    }

    private void log(String section, String msg) {
        String fullMsg = section + "\n  " + msg;
        Logger.log(fullMsg);
        if (window != null)
            window.appendLog(fullMsg);
    }
}