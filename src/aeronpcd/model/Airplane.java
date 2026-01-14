package aeronpcd.model;

import aeronpcd.resources.Gate;
import aeronpcd.resources.Runway;
import aeronpcd.tower.ControlTower;
import aeronpcd.util.Logger;

// PRÁCTICA 7: Extendemos de Thread para convertir al avión en un proceso independiente
public class Airplane extends Thread {

    // PRÁCTICA 7: Cambiamos 'id' a 'flightId' porque la clase Thread ya tiene un
    // método getId()
    // que devuelve un long, y entraban en conflicto.
    private String flightId;

    private ControlTower tower;
    private FlightStatus status;

    // Recursos asignados por la torre
    private Runway currentRunway;
    private Gate currentGate;

    // Tiempos para estadísticas (CSV de la Práctica 6 y 7)
    private long startTime;
    private long endTime;

    public Airplane(String id, ControlTower tower) {
        this.flightId = id;
        this.tower = tower;
        this.status = FlightStatus.IN_FLIGHT;

        // PRÁCTICA 7: Asignamos el nombre al Hilo para verlo bien en los logs
        this.setName(id);
    }

    // PRÁCTICA 7: El método run() es el punto de entrada cuando hacemos
    // airplane.start()
    @Override
    public void run() {
        // Ejecutamos la lógica del ciclo de vida del avión
        runSequential();
    }

    // Lógica completa del ciclo de vida del avión
    public void runSequential() {
        this.startTime = System.currentTimeMillis();
        Logger.log("Avión [" + flightId + " - " + status + "] Inicia ciclo");

        // --- FASE 1: ATERRIZAJE ---
        this.status = FlightStatus.LANDING_REQUEST;
        Logger.log("Avión [" + flightId + "] Solicita aterrizaje y ESPERA...");

        // Enviamos petición y nos dormimos (wait) hasta que un operario nos atienda
        sendRequestAndWait(Request.Type.LANDING);

        // Si la torre nos ha asignado recursos (estado cambiado a LANDING_ASSIGNED)
        if (this.status == FlightStatus.LANDING_ASSIGNED) {
            Logger.log("Avión [" + flightId + "] Recibe luz verde. Asignado a " + currentRunway.getId());

            // Simulamos la maniobra de aterrizaje
            performAction(100, "Aterrizando", FlightStatus.LANDING);

            // Avisamos de que hemos aterrizado (libera Pista)
            sendRequestAndWait(Request.Type.LANDED);

            // --- FASE 2: EMBARQUE ---
            performAction(50, "Embarcando", FlightStatus.BOARDING);
            sendRequestAndWait(Request.Type.BOARDING); // Solo actualiza panel

            // Fin de embarque (libera Puerta)
            sendRequestAndWait(Request.Type.BOARDED);

            // --- FASE 3: DESPEGUE ---
            this.status = FlightStatus.TAKEOFF_REQUESTED;
            Logger.log("Avión [" + flightId + "] Solicita despegue y ESPERA...");

            // Pedimos pista de despegue y esperamos
            sendRequestAndWait(Request.Type.TAKEOFF);

            if (this.status == FlightStatus.TAKEOFF_ASSIGNED) {
                Logger.log("Avión [" + flightId + "] Recibe luz verde. Despegue en " + currentRunway.getId());

                // Simulamos maniobra de despegue
                performAction(100, "Despegando", FlightStatus.DEPARTING);

                // Avisamos de que nos hemos ido (libera Pista)
                sendRequestAndWait(Request.Type.DEPARTED);
            }
        } else {
            // PRÁCTICA 6: Gestión de errores si algo falló en la asignación
            Logger.log("Avión [" + flightId + "] ERROR: No pudo aterrizar.");
        }

        this.endTime = System.currentTimeMillis();
        Logger.log("Avión [" + flightId + " - DEPARTED] Ciclo completado en " + getTotalTime() + " ms");
    }

    // Método auxiliar para enviar petición y bloquear el hilo (wait)
    private void sendRequestAndWait(Request.Type type) {
        synchronized (this) {
            // Añadimos la petición a la cola de la torre
            tower.addRequest(new Request(this, type));
            try {
                // El avión se duerme aquí. Despertará cuando el Operario haga plane.notify()
                this.wait();
            } catch (InterruptedException e) {
                Logger.log("Avión [" + flightId + "] Interrumpido mientras esperaba.");
            }
        }
    }

    // Método auxiliar para simular tiempo de acción (sleep) sin bloquear monitores
    // críticos
    private void performAction(int time, String actionName, FlightStatus tempStatus) {
        this.status = tempStatus;
        Logger.log("Avión [" + flightId + " - " + status + "] " + actionName + " (" + time + "ms)...");
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
        }
    }

    // --- GETTERS Y SETTERS ---

    // PRÁCTICA 7: Getter renombrado. Úsalo siempre en lugar de getId()
    public String getFlightId() {
        return flightId;
    }

    public long getTotalTime() {
        return endTime - startTime;
    }

    public FlightStatus getStatus() {
        return status;
    }

    public void setStatus(FlightStatus status) {
        this.status = status;
    }

    public void setResources(Runway r, Gate g) {
        this.currentRunway = r;
        this.currentGate = g;
    }

    public void setTakeoffRunway(Runway r) {
        this.currentRunway = r;
    }

    public Runway getCurrentRunway() {
        return currentRunway;
    }

    public Gate getCurrentGate() {
        return currentGate;
    }
}