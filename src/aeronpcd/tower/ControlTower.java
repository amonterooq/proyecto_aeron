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

/**
 * ControlTower gestiona la coordinación central del aeropuerto.
 * Implementa el patrón Productor-Consumidor:
 * - Productores: Los Aviones (Airplane) añaden peticiones a la cola.
 * - Consumidores: Los Operadores (Operator) procesan las peticiones
 * concurrentemente.
 *
 * Utiliza estructuras Thread-Safe y bloques sincronizados para gestionar
 * el acceso a recursos compartidos (Pistas y Puertas).
 */
public class ControlTower {
    // Recursos del aeropuerto
    private List<Runway> runways;
    private List<Gate> gates;
    private AirportWindow window;

    // Cola de peticiones compartida (Monitor)
    private LinkedList<Request> requestQueue;

    // Pool de hilos trabajadores
    private Operator[] operators;

    // Límite de capacidad de la cola para simular saturación
    private static final int MAX_QUEUE_SIZE = 15;

    /**
     * @param runways Lista de pistas disponibles.
     * @param gates   Lista de puertas de embarque disponibles.
     */
    public ControlTower(List<Runway> runways, List<Gate> gates) {
        this.runways = runways;
        this.gates = gates;
        this.requestQueue = new LinkedList<>();

        // Inicializamos y arrancamos los 5 operarios (Threads Daemon)
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

    /**
     * Añade una nueva petición a la cola de procesamiento.
     * Implementa lógica de Prioridad para evitar inanición (Starvation).
     * 
     * @param request La solicitud enviada por un Avión.
     */
    public void addRequest(Request request) {
        synchronized (requestQueue) {
            // Verificación de saturación (informativa)
            if (requestQueue.size() >= MAX_QUEUE_SIZE) {
                System.err.println("ALERTA: Cola de torre saturada al recibir "
                        + request.getType() + " del vuelo " + request.getPlane().getFlightId());
            }

            // --- LÓGICA DE PRIORIDAD ---
            // Las peticiones que LIBERAN recursos (Landing realizado, Embarque finalizado,
            // Salida)
            // se insertan al PRINCIPIO de la cola. Esto es crítico para evitar bloqueos:
            // si la cola se llena de peticiones que piden recursos (LANDING/TAKEOFF) y no
            // procesamos las que liberan, el sistema se detiene (Deadlock).
            if (request.getType() == Request.Type.LANDED ||
                    request.getType() == Request.Type.BOARDED ||
                    request.getType() == Request.Type.DEPARTED) {

                requestQueue.addFirst(request); // Prioridad Alta (LIFO para liberaciones)
            } else {
                requestQueue.addLast(request); // Prioridad Normal (FIFO para solicitudes)
            }

            log("Torre de control",
                    "Recibida solicitud " + request.getType() + " de Avión [" + request.getPlane().getFlightId() + "]");

            updateVisualization();

            // Despertar a cualquier operario que esté esperando trabajo
            requestQueue.notifyAll();
        }
    }

    /**
     * Clase interna que representa un hilo trabajador de la torre.
     * Consume peticiones de la cola y gestiona la asignación de recursos.
     */
    private class Operator extends Thread {
        public Operator(String name) {
            setName(name);
            setDaemon(true); // Permite finalizar la JVM cuando acabe el main
        }

        @Override
        public void run() {
            while (true) {
                Request req;

                // 1. OBTENCIÓN DE TAREA (Sección Crítica)
                synchronized (requestQueue) {
                    while (requestQueue.isEmpty()) {
                        try {
                            requestQueue.wait(); // Espera pasiva hasta recibir notificaciones
                        } catch (InterruptedException e) {
                            return; // Salida limpia
                        }
                    }
                    req = requestQueue.poll(); // Extraer petición (según orden de prioridad)
                    updateVisualization();
                }

                log("Torre de control", "Operario [" + getName() + "] atendiendo " + req.getType() + " de vuelo "
                        + req.getPlane().getFlightId());

                // Simulación de tiempo administrativo
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                // 2. PROCESAMIENTO Y ASIGNACIÓN DE RECURSOS
                boolean success = processRequestLogic(req);

                // 3. RESPUESTA AL ESTADO DEL RECURSO
                if (success) {
                    log("Torre de control", "Operario [" + getName() + "] VALIDÓ " + req.getType() + " para "
                            + req.getPlane().getFlightId());

                    // Notificar al Avión específico que su petición ha sido procesada
                    synchronized (req.getPlane()) {
                        req.getPlane().notify();
                    }
                } else {
                    // Si no hay recursos disponibles, reencolamos la petición
                    // para intentarlo más tarde (Busy-Wait con delay)
                    log("Torre de control", "Operario [" + getName() + "] POSPONE " + req.getType()
                            + " (Recursos ocupados) para " + req.getPlane().getFlightId());

                    synchronized (requestQueue) {
                        requestQueue.add(req);
                        updateVisualization();
                        requestQueue.notifyAll(); // Notificar a otros operarios por si pueden coger otra
                    }

                    try {
                        Thread.sleep(200); // Pequeña espera para evitar saturación de CPU
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
    }

    /**
     * Lógica central de gestión de recursos.
     * Sincronizado sobre 'this' (la Torre) para garantizar exclusión mutua
     * al consultar/modificar el estado de Pistas y Puertas.
     * 
     * @param req Petición a procesar.
     * @return true si la operación se pudo realizar (hay recursos), false si debe
     *         esperar.
     */
    private boolean processRequestLogic(Request req) {
        Airplane plane = req.getPlane();
        String planeId = plane.getFlightId();

        synchronized (this) {
            switch (req.getType()) {
                case LANDING:
                    // Requiere: 1 Pista libre AND 1 Puerta libre
                    Runway r = getFreeRunway();
                    Gate g = getFreeGate();

                    if (r != null && g != null) {
                        r.occupy(planeId);
                        g.occupy(planeId);

                        plane.setStatus(FlightStatus.LANDING_ASSIGNED);
                        plane.setResources(r, g);
                        FlightPanel.updateStatus(planeId, "LANDING_ASSIGNED");

                        log("Torre de control",
                                "Asignados: Pista " + r.getId() + " y Puerta " + g.getId() + " a " + planeId);
                        updateVisualization();
                        return true;
                    }
                    return false; // Recursos no disponibles

                case LANDED:
                    // Libera Pista de aterrizaje
                    if (plane.getCurrentRunway() != null) {
                        plane.getCurrentRunway().free();
                    }
                    plane.setStatus(FlightStatus.LANDED);
                    FlightPanel.updateStatus(planeId, "LANDED");
                    updateVisualization();
                    return true;

                case BOARDING:
                    // Cambio de estado
                    plane.setStatus(FlightStatus.BOARDING);
                    FlightPanel.updateStatus(planeId, "BOARDING");
                    return true;

                case BOARDED:
                    // Libera Puerta de embarque
                    if (plane.getCurrentGate() != null) {
                        plane.getCurrentGate().free();
                    }
                    plane.setStatus(FlightStatus.BOARDED);
                    FlightPanel.updateStatus(planeId, "BOARDED");
                    updateVisualization();
                    return true;

                case TAKEOFF:
                    // Requiere: 1 Pista libre
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
                    return false; // Pista no disponible

                case DEPARTED:
                    // Libera Pista de despegue
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

    private void updateVisualization() {
        if (window != null) {
            String htmlState;
            // Sincronizamos para obtener una instantánea consistente de la cola
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
        String fullMsg = String.format("[%s] %s", section, msg);
        Logger.log(fullMsg);
        if (window != null)
            window.appendLog(fullMsg);
    }
}