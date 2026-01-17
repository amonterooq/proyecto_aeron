package aeronpcd.tower;

import aeronpcd.model.Airplane;
import aeronpcd.model.FlightStatus;
import aeronpcd.model.Request;
import aeronpcd.resources.Gate;
import aeronpcd.resources.Runway;
import aeronpcd.util.AirportState;
import aeronpcd.util.FlightPanel;
import aeronpcd.util.Logger;
import aeronpcd.gui.AirportWindow;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * ControlTower gestiona la coordinación central del aeropuerto usando
 * Monitores.
 * 
 * ESTRATEGIA DE SINCRONIZACIÓN:
 * - Monitor Único: synchronized(this) para proteger estado de recursos y colas.
 * - Dos Colas:
 * 1. requestQueue: Nuevas peticiones que llegan de los aviones.
 * 2. waitingList: Peticiones que fueron atendidas pero no había recursos.
 * 
 * PREVENCIÓN DE DEADLOCK Y NOTIFICACIÓN PERDIDA:
 * - Asignación atómica: LANDING requiere Pista Y Puerta. Si no están ambas, no
 * se toma ninguna.
 * - Waiting List: En lugar de "wait/notify" ciegos o reencolar al final, las
 * peticiones fallidas
 * esperan en una lista separada.
 * - Reactivación: Cada vez que se libera un recurso (LANDED, DEPARTED,
 * BOARDED),
 * se escanea la waitingList para ver si alguien puede proceder.
 */
public class ControlTower {
    private List<Runway> runways;
    private List<Gate> gates;
    private AirportWindow window;

    // Colas
    private LinkedList<Request> requestQueue;
    private LinkedList<Request> waitingList; // Peticiones pausadas esperando recursos

    private Operator[] operators;

    // Flag para cierre ordenado (aunque usaremos Daemon threads)
    private boolean shutdown = false;

    public ControlTower(List<Runway> runways, List<Gate> gates, int operatorCount) {
        this.runways = runways;
        this.gates = gates;
        this.requestQueue = new LinkedList<>();
        this.waitingList = new LinkedList<>();

        this.operators = new Operator[operatorCount];
        for (int i = 0; i < operatorCount; i++) {
            operators[i] = new Operator("OP-" + (i + 1));
            operators[i].start();
        }
    }

    public void setWindow(AirportWindow window) {
        this.window = window;
        updateVisualization();
    }

    /**
     * Entrada de peticiones desde los Aviones (Productores).
     */
    public synchronized void addRequest(Request request) {
        // Prioridad: Las peticiones que LIBERAN recursos deben procesarse antes
        // para evitar atascos (Starvation por saturación de cola).
        if (isFreeingRequest(request)) {
            requestQueue.addFirst(request);
        } else {
            requestQueue.addLast(request);
        }

        log("Torre", "Recibida solicitud " + request.getType() + " de " + request.getPlane().getFlightId());
        updateVisualization();
        notifyAll(); // Despierta a operarios esperando en wait()
    }

    private boolean isFreeingRequest(Request r) {
        return r.getType() == Request.Type.LANDED ||
                r.getType() == Request.Type.BOARDED ||
                r.getType() == Request.Type.DEPARTED;
    }

    /**
     * Hilo Operario (Consumidor).
     */
    private class Operator extends Thread {
        public Operator(String name) {
            setName(name);
            setDaemon(true); // Se cierran al terminar el main
        }

        @Override
        public void run() {
            while (true) {
                Request req;

                // 1. Obtener trabajo (Sección Crítica - Monitor de Torre)
                synchronized (ControlTower.this) {
                    while (requestQueue.isEmpty() && !shutdown) {
                        try {
                            ControlTower.this.wait();
                        } catch (InterruptedException e) {
                            return;
                        }
                    }
                    req = requestQueue.poll();
                }

                if (req == null)
                    continue;

                // LOGS REQUERIDOS: "ha cogido una petición..." y "esta procesando..."
                Logger.log("Operario [" + getName() + "] ha cogido una petición de tipo " + req.getType());
                Logger.log("Operario [" + getName() + "] esta procesando la petición del AVION ["
                        + req.getPlane().getFlightId() + "]");

                // Simular trabajo administrativo fuera del lock (permite concurrencia real)
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                }

                // 2. Procesar solicitud (Sección Crítica y Lógica de Asignación)
                processRequest(req);
            }
        }
    }

    /**
     * Lógica principal de asignación de recursos.
     * Es synchronized para asegurar exclusión mutua completa durante la
     * comprobación de estado.
     */
    private synchronized void processRequest(Request req) {
        try {
            boolean processed = tryAssignResources(req);

            if (processed) {
                // CASO 1: ÉXITO. Recursos asignados o liberados.
                req.getPlane().grantPermission();

                Logger.log("Operario [" + Thread.currentThread().getName()
                        + "] ha procesado correctamente la petición del AVION [" + req.getPlane().getFlightId() + "]");

                // CRÍTICO: Si liberamos recursos, revisamos inmediatamente la lista de espera
                if (isFreeingRequest(req)) {
                    checkWaitingList();
                }
            } else {
                // CASO 2: FALLO. No hay recursos. Mover a lista de espera.
                // NO notificamos al avión, este sigue durmiendo.
                waitingList.add(req);
                Logger.log("Operario [" + Thread.currentThread().getName() + "] ha movido la petición del AVION ["
                        + req.getPlane().getFlightId() + "] a la lista de espera");
            }
            updateVisualization();
        } catch (Exception e) {
            log("ERROR", "Excepción procesando request: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Revisa la lista de espera para ver si alguna petición antigua ahora puede
     * procesarse.
     * Se llama siempre que se libera un recurso.
     */
    private void checkWaitingList() {
        if (waitingList.isEmpty())
            return;

        // Iteramos sobre la lista de espera. Usamos Iterator para poder borrar
        // elementos de forma segura.
        Iterator<Request> it = waitingList.iterator();
        while (it.hasNext()) {
            Request pendingReq = it.next();

            // Intentamos asignar recursos SIN quitarlo de la lista todavía
            if (tryAssignResources(pendingReq)) {
                // Si tiene éxito, sacamos de la lista y despertamos al avión
                it.remove();
                pendingReq.getPlane().grantPermission();
                Logger.log("Operario [" + Thread.currentThread().getName()
                        + "] ha recuperado y procesado la petición del AVION [" + pendingReq.getPlane().getFlightId()
                        + "] de la lista de espera");
            }
            // Si falla, se queda en la lista manteniendo su turno (FIFO relativo) y
            // probamos con el siguiente
            // (Esto evita bloqueo: si el primero necesita PUERTA y no hay, pero el segundo
            // necesita solo PISTA, el segundo pasa).
        }
    }

    /**
     * Intenta asignar o liberar recursos. Devuelve true si la operación se
     * completó,
     * false si faltan recursos.
     */
    private boolean tryAssignResources(Request req) {
        Airplane plane = req.getPlane();
        String id = plane.getFlightId();

        switch (req.getType()) {
            case LANDING:
                // ATÓMICO: Necesitamos Pista Libre AND Puerta Libre
                Runway r = getFreeRunway();
                Gate g = getFreeGate();
                if (r != null && g != null) {
                    r.occupy(id);
                    g.occupy(id);
                    plane.setResources(r, g);
                    plane.setStatus(FlightStatus.LANDING_ASSIGNED);
                    FlightPanel.updateStatus(id, "LANDING_ASSIGNED");
                    return true;
                }
                return false;

            case LANDED:
                if (plane.getCurrentRunway() != null)
                    plane.getCurrentRunway().free();
                plane.setStatus(FlightStatus.LANDED);
                FlightPanel.updateStatus(id, "LANDED");
                return true;

            case BOARDING:
                // Solo cambio de estado
                plane.setStatus(FlightStatus.BOARDING);
                FlightPanel.updateStatus(id, "BOARDING");
                return true;

            case BOARDED:
                // Libera Puerta
                if (plane.getCurrentGate() != null)
                    plane.getCurrentGate().free();
                plane.setStatus(FlightStatus.BOARDED);
                FlightPanel.updateStatus(id, "BOARDED");
                return true;

            case TAKEOFF:
                Runway rTakeoff = getFreeRunway();
                if (rTakeoff != null) {
                    rTakeoff.occupy(id);
                    plane.setTakeoffRunway(rTakeoff);
                    plane.setStatus(FlightStatus.TAKEOFF_ASSIGNED);
                    FlightPanel.updateStatus(id, "TAKEOFF_ASSIGNED");
                    return true;
                }
                return false;

            case DEPARTED:
                if (plane.getCurrentRunway() != null)
                    plane.getCurrentRunway().free();
                plane.setStatus(FlightStatus.DEPARTED);
                FlightPanel.updateStatus(id, "DEPARTED");
                return true;

            default:
                return true;
        }
    }

    // Helpers
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

    private synchronized void updateVisualization() {
        if (window != null) {
            // Creamos una lista combinada para la visualización (Informativo)
            LinkedList<Request> allPending = new LinkedList<>(requestQueue);
            // Añadimos las de waitingList al final para que se vean
            allPending.addAll(waitingList);

            String htmlState = AirportState.getHTMLState(runways, gates, allPending);
            window.updateState(htmlState);
        }
    }

    private void log(String actor, String msg) {
        String fullMsg = "[" + String.format("%-10s", actor) + "] " + msg;
        Logger.log(fullMsg);
        if (window != null) {
            window.appendLog(fullMsg);
        }
    }
}
