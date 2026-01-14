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

import java.util.LinkedList;
import java.util.List;

public class ControlTower {
    // Recursos del aeropuerto
    private List<Runway> runways;
    private List<Gate> gates;
    private AirportWindow window;

    // Cola de peticiones compartida
    private LinkedList<Request> requestQueue;

    // PRÁCTICA 3: Array de hilos operarios
    private Operator[] operators;

    // PRÁCTICA 6: Límite de cola para detectar saturación
    private static final int MAX_QUEUE_SIZE = 15;

    public ControlTower(List<Runway> runways, List<Gate> gates) {
        this.runways = runways;
        this.gates = gates;
        this.requestQueue = new LinkedList<>();

        // Inicializamos y arrancamos los 5 operarios al crear la torre
        this.operators = new Operator[5];
        for (int i = 0; i < 5; i++) {
            operators[i] = new Operator("OP-" + (i + 1));
            operators[i].start();
        }
    }

    public void setWindow(AirportWindow window) {
        this.window = window;
        updateVisualization(); // Pintar estado inicial
    }

    // --- MÉTODO DE ENTRADA DE PETICIONES (Thread-Safe) ---
    /**
     * Método público para añadir una nueva petición a la cola.
     * Actúa como Productor en el patrón Productor-Consumidor.
     * Es invocado por los hilos de los Aviones.
     * 
     * @param request La solicitud que envía el avión
     */
    public void addRequest(Request request) {
        synchronized (requestQueue) {
            // PRÁCTICA 6: Comprobación de saturación
            if (requestQueue.size() >= MAX_QUEUE_SIZE) {
                System.err.println("Cola de peticiones completa, reintentando más tarde petición "
                        + request.getType() + " del avión " + request.getPlane().getFlightId());
            }

            // OPTIMIZACIÓN PRÁCTICA 7: Prioridad de mensajes
            // Las peticiones que liberan recursos deben procesarse ANTES para evitar atascos (Starvation)
            // LANDED -> Libera Pista
            // BOARDED -> Libera Puerta
            // DEPARTED -> Libera Pista
            if (request.getType() == Request.Type.LANDED || 
                request.getType() == Request.Type.BOARDED || 
                request.getType() == Request.Type.DEPARTED) {
                
                requestQueue.addFirst(request); // Prioridad Alta
                // log("Torre (Debug)", "Priorizando " + request.getType());
            } else {
                requestQueue.addLast(request); // Prioridad Normal (LANDING, TAKEOFF, etc.)
            }

            log("Torre de control",
                    "Recibida solicitud " + request.getType() + " de Avión [" + request.getPlane().getFlightId() + "]");

            // Actualizamos la ventana gráfica
            updateVisualization();

            // NotifyAll despierta a los operarios que estén esperando (wait) en la cola vacía
            requestQueue.notifyAll();
        }
    }

    // --- CLASE INTERNA: EL OPERARIO (Hilo Trabajador) ---
    /**
     * Clase que representa a un Operario de la torre.
     * Funciona como un consumidor en el patrón Productor-Consumidor.
     * Consume peticiones de la cola compartida 'requestQueue' y las procesa.
     */
    private class Operator extends Thread {
        public Operator(String name) {
            setName(name);
            setDaemon(true); // Se marca como Daemon para que no impida que la JVM termine si el main acaba
        }

        @Override
        public void run() {
            while (true) {
                Request req;

                // 1. INTENTAR COGER PETICIÓN DE LA COLA (Sección Crítica)
                synchronized (requestQueue) {
                    // Si la cola está vacía, el operario espera (wait)
                    while (requestQueue.isEmpty()) {
                        try {
                            // Si no hay trabajo, dormimos esperando notificación de nueva petición
                            requestQueue.wait();
                        } catch (InterruptedException e) {
                            return; // Salir limpio si interrumpen el hilo
                        }
                    }
                    // Sacamos la primera petición (FIFO) y actualizamos vista
                    req = requestQueue.poll();
                    updateVisualization();
                }

                log("Torre de control", "Operario [" + getName() + "] atiende " + req.getType() + " para "
                        + req.getPlane().getFlightId());

                // Simulamos tiempo de gestión (papeleo)
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                }

                // 2. PROCESAR LA PETICIÓN Y ASIGNAR RECURSOS
                boolean success = processRequestLogic(req);

                // 3. RESPUESTA
                if (success) {
                    log("Torre de control", "Operario [" + getName() + "] FINALIZÓ " + req.getType() + " para "
                            + req.getPlane().getFlightId());

                    // Despertamos al avión específico que hizo la petición
                    synchronized (req.getPlane()) {
                        req.getPlane().notify();
                    }
                } else {
                    // PRÁCTICA 4/6: Si no hay recursos, no descartamos, re-encolamos
                    log("Torre de control", "Operario [" + getName() + "] RE-ENCOLA " + req.getType()
                            + " (Sin Recursos) para " + req.getPlane().getFlightId());

                    synchronized (requestQueue) {
                        requestQueue.add(req);
                        updateVisualization();
                        requestQueue.notifyAll();
                    }
                    // Pequeña espera para no saturar la CPU rebotando la petición
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                    }
                }
            }
        }
    }

    // --- LÓGICA DE ASIGNACIÓN DE RECURSOS ---
    /**
     * Procesa la lógica de negocio de una petición.
     * Gestiona la reserva y liberación de recursos compartidos (Pistas y Puertas) de forma atómica.
     * 
     * @param req La petición a procesar
     * @return true si la petición fue satisfecha exitosamente, false si faltan recursos
     */
    private boolean processRequestLogic(Request req) {
        Airplane plane = req.getPlane();
        String planeId = plane.getFlightId(); // Usamos getFlightId corregido

        // Sincronizamos la Torre (monitor objeto 'this') para garantizar exclusión mutua
        // al consultar y modificar el estado de pistas y puertas.
        // Evita condiciones de carrera donde dos aviones cojan la misma pista.
        synchronized (this) {
            switch (req.getType()) {
                case LANDING:
                    // REQUISITO: Necesita Pista Y Puerta libres
                    Runway r = getFreeRunway();
                    Gate g = getFreeGate();

                    if (r != null && g != null) {
                        r.occupy(planeId);
                        g.occupy(planeId);

                        plane.setStatus(FlightStatus.LANDING_ASSIGNED);
                        plane.setResources(r, g);

                        // PRÁCTICA 5: Actualizar JSON persistente
                        FlightPanel.updateStatus(planeId, "LANDING_ASSIGNED");

                        log("Torre de control",
                                "Asignados: Pista " + r.getId() + " y Puerta " + g.getId() + " a " + planeId);
                        updateVisualization();
                        return true;
                    }
                    return false; // Fallo por falta de recursos

                case LANDED:
                    // El avión ha aterrizado, liberamos la Pista
                    if (plane.getCurrentRunway() != null) {
                        plane.getCurrentRunway().free();
                    }
                    plane.setStatus(FlightStatus.LANDED);
                    FlightPanel.updateStatus(planeId, "LANDED");
                    updateVisualization();
                    return true;

                case BOARDING:
                    // Solo cambio de estado
                    plane.setStatus(FlightStatus.BOARDING);
                    FlightPanel.updateStatus(planeId, "BOARDING");
                    return true;

                case BOARDED:
                    // El avión ha terminado el embarque, liberamos la Puerta
                    if (plane.getCurrentGate() != null) {
                        plane.getCurrentGate().free();
                    }
                    plane.setStatus(FlightStatus.BOARDED);
                    FlightPanel.updateStatus(planeId, "BOARDED");
                    updateVisualization();
                    return true;

                case TAKEOFF:
                    // REQUISITO: Necesita una pista nueva para despegar
                    Runway rTakeoff = getFreeRunway();
                    if (rTakeoff != null) {
                        rTakeoff.occupy(planeId);
                        plane.setStatus(FlightStatus.TAKEOFF_ASSIGNED);
                        plane.setTakeoffRunway(rTakeoff);

                        FlightPanel.updateStatus(planeId, "TAKEOFF_ASSIGNED");
                        log("Torre de control", "Asignada Pista " + rTakeoff.getId() + " para despegue de " + planeId);
                        updateVisualization();
                        return true;
                    }
                    return false; // Fallo por falta de pista

                case DEPARTED:
                    // El avión se ha ido, liberamos la pista de despegue
                    if (plane.getCurrentRunway() != null) {
                        plane.getCurrentRunway().free();
                    }
                    plane.setStatus(FlightStatus.DEPARTED);
                    FlightPanel.updateStatus(planeId, "DEPARTED");
                    updateVisualization();
                    return true;
            }
        }
        return true;
    }

    // Actualiza la ventana gráfica llamando al generador de HTML
    private void updateVisualization() {
        if (window != null) {
            String htmlState;
            synchronized (requestQueue) {
                htmlState = AirportState.getHTMLState(runways, gates, requestQueue);
            }
            window.updateState(htmlState);
        }
    }

    // Helpers para buscar recursos libres
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
        // Formato simplificado en una sola línea
        String fullMsg = String.format("[%s] %s", section, msg);
        Logger.log(fullMsg);
        if (window != null)
            window.appendLog(fullMsg);
    }
}