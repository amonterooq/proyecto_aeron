package aeronpcd.model;

import aeronpcd.resources.Gate;
import aeronpcd.resources.Runway;
import aeronpcd.tower.ControlTower;
import aeronpcd.util.Logger;

import java.util.concurrent.ThreadLocalRandom;

// PRÁCTICA 7: Extendemos de Thread para convertir al avión en un proceso independiente
public class Airplane extends Thread {

    // PRÁCTICA 7: Cambiamos 'id' a 'flightId' porque la clase Thread ya tiene un
    // método getId()
    // que devuelve un long, y entraban en conflicto.
    private String flightId;

    private ControlTower tower;
    private volatile FlightStatus status;

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

    /**
     * Método principal que ejecuta la lógica concurrente del avión (Práctica 7).
     * Es el punto de entrada del Hilo cuando se llama a this.start().
     */
    @Override
    public void run() {
        try {
            // Desfase aleatorio inicial para evitar el efecto "avalancha" (Thundering Herd)
            // en la simulación concurrente.
            try {
                Thread.sleep(ThreadLocalRandom.current().nextInt(100, 500));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // Ejecutamos la lógica del ciclo de vida del avión
            runSequential();
        } catch (Throwable t) {
            Logger.log("ERROR FATAL en Avión [" + flightId + "]: " + t.getMessage());
            t.printStackTrace();
        }
    }

    /**
     * Define el ciclo de vida completo del avión: Aterrizaje -> Embarque ->
     * Despegue.
     * Este método contiene la secuencia lógica, pero las esperas son gestionadas
     * por monitores.
     */
    /**
     * Define el ciclo de vida completo del avión: IN_FLIGHT -> LANDING -> BOARDING
     * -> TAKEOFF -> DEPARTED.
     */
    public void runSequential() {
        this.startTime = System.currentTimeMillis();

        // 1. IN_FLIGHT
        this.status = FlightStatus.IN_FLIGHT;
        Logger.log("Avión [" + flightId + " - IN_FLIGHT] Inicia ciclo"); // #1
        Logger.log("Avión [" + flightId + " - IN_FLIGHT] El avión está en vuelo");

        // --- FASE 2: SOLICITUD ATERRIZAJE ---
        this.status = FlightStatus.LANDING_REQUEST;
        Logger.log("Avión [" + flightId + " - IN_FLIGHT] Solicita aterrizaje a torre de control");
        Logger.log("Avión [" + flightId + " - LANDING_REQUESTED] Solicitud de aterrizaje en cola");
        Logger.log("Avión [" + flightId + " - LANDING_REQUEST] Espera autorización de aterrizaje");

        sendRequestAndWait(Request.Type.LANDING);

        // --- FASE 3: ATERRIZAJE ASIGNADO ---
        if (this.status == FlightStatus.LANDING_ASSIGNED) {
            // Logs requeridos en punto 3
            Logger.log("Avión [" + flightId + " - LANDING_ASSIGNED] Aterrizaje autorizado");
            Logger.log("Avión [" + flightId + " - LANDING_ASSIGNED] Me ha tocado aterrizar en la Pista ["
                    + currentRunway.getId() + "]");
            Logger.log("Avión [" + flightId + " - LANDING_ASSIGNED] Me ha tocado embarcar en la Puerta ["
                    + currentGate.getId() + "]");

            // --- FASE 4: ATERRIZANDO ---
            Logger.log("Avión [" + flightId + " - LANDING_ASSIGNED] ha notificado que va a empezar a aterrizar");
            performAction(randomTime(100, 150), "Aterrizando", FlightStatus.LANDING);
            // performAction ya imprime "Avión [ID - LANDING] Aterrizando..."

            // --- FASE 5: ATERRIZADO ---
            // Logs requeridos punto 5
            Logger.log("Avión [" + flightId + " - LANDING] ha notificado que ha aterrizado");
            sendRequestAndWait(Request.Type.LANDED);
            Logger.log("Avión [" + flightId + " - LANDED] Aterrizado");

            // --- FASE 6: BOARDING ---
            Logger.log("Avión [" + flightId + " - LANDED] ha notificado que ha empezado el embarque");
            performAction(randomTime(50, 80), "Embarcando", FlightStatus.BOARDING);
            int passengers = randomTime(100, 350); // Simulado con randomTime
            Logger.log(passengers + " pasajeros del avión [" + flightId + "] han abordado");

            sendRequestAndWait(Request.Type.BOARDING); // Solo para actualizar panel/estado si necesario

            // --- FASE 7: BOARDED ---
            Logger.log("Avión [" + flightId + " - BOARDING] ha notificado que ha finalizado el embarque");
            sendRequestAndWait(Request.Type.BOARDED);
            Logger.log("Avión [" + flightId + " - BOARDED] Embarcado");

            // --- FASE 8: TAKEOFF REQUEST ---
            this.status = FlightStatus.TAKEOFF_REQUESTED;
            Logger.log("Avión [" + flightId + " - BOARDED] ha notificado que quiere despegar");
            Logger.log("Avión [" + flightId + " - TAKEOFF_REQUESTED] Solicitud de despegue en cola");
            Logger.log("Avión [" + flightId + " - TAKEOFF_REQUESTED] Espera autorización de despegue");

            sendRequestAndWait(Request.Type.TAKEOFF);

            if (this.status == FlightStatus.TAKEOFF_ASSIGNED) {
                // --- FASE 9: TAKEOFF ASSIGNED ---
                Logger.log("Avión [" + flightId + " - TAKEOFF_ASSIGNED] Despegue autorizado");
                Logger.log("Avión [" + flightId + " - TAKEOFF_ASSIGNED] Me ha tocado despegar en la Pista ["
                        + currentRunway.getId() + "]");

                // --- FASE 10: DEPARTING ---
                Logger.log("Avión [" + flightId + " - TAKEOFF_ASSIGNED] ha notificado que va a despegar");
                performAction(randomTime(80, 150), "Despegando", FlightStatus.DEPARTING);

                // --- FASE 11: DEPARTED ---
                Logger.log("Avión [" + flightId + " - DEPARTING] ha notificado que ha despegado");
                // NOTA: Cambiamos estado antes de avisar para que el log del operario sea
                // consistente
                this.status = FlightStatus.DEPARTED;
                sendRequestAndWait(Request.Type.DEPARTED);

                Logger.log("Avión [" + flightId + " - DEPARTED] El avión ha despegado");
            }
        } else {
            Logger.log("Avión [" + flightId + "] ERROR: No pudo aterrizar.");
        }

        this.endTime = System.currentTimeMillis();
        // Log final con icono requerido
        Logger.log("Avión [" + flightId + " - DEPARTED] 🏁 Completó ciclo en " + getTotalTime() + " ms");
    }

    private volatile boolean resourceGranted = false;

    /**
     * Método para despertar al avión de forma segura.
     * Invocado por la Torre de Control (Operario).
     */
    public synchronized void grantPermission() {
        this.resourceGranted = true;
        this.notifyAll(); // Usamos notifyAll para máxima seguridad
    }

    /**
     * Envía una petición a la Torre de Control y bloquea el hilo del avión actual.
     * Patrón de sincronización: Wait-Notify con Flag (Guarded Block).
     * 
     * CORRECCIÓN DEADLOCK: Se ha extraído la llamada a tower.addRequest() FUERA
     * del bloque synchronized(this). Esto evita que el avión retenga su propio lock
     * mientras intenta adquirir el lock de la Torre, previniendo el abrazo mortal
     * si la Torre intenta adquirir el lock del Avión al mismo tiempo.
     */
    private void sendRequestAndWait(Request.Type type) {
        // 1. Resetear flag (Sección crítica pequeña para asegurar memoria)
        synchronized (this) {
            this.resourceGranted = false;
        }

        // 2. Publicar petición (SIN lock del avión)
        // Aquí el hilo adquiere Lock(Torre) pero NO tiene Lock(Avión), rompiendo el
        // ciclo.
        tower.addRequest(new Request(this, type));

        // 3. Esperar respuesta (Guarded Block)
        synchronized (this) {
            while (!this.resourceGranted) {
                try {
                    this.wait();
                } catch (InterruptedException e) {
                    Logger.log("Avión [" + flightId + "] Interrumpido mientras esperaba.");
                }
            }
        }
    }

    // Método auxiliar para simular tiempo de acción (sleep) sin bloquear monitores
    // críticos
    private void performAction(int time, String actionName, FlightStatus tempStatus) {
        this.status = tempStatus;
        Logger.log("Avión [" + flightId + " - " + status + "] " + actionName + " (" + time + "ms)...");
        try {
            // Simulamos la duración de la operación (aterrizaje/despegue)
            Thread.sleep(time);
        } catch (InterruptedException e) {
            Logger.log("Avión [" + flightId + "] Interrumpido durante acción: " + actionName);
            // Reestablecemos el estado de interrupción por buenas prácticas
            Thread.currentThread().interrupt();
        }
    }

    private int randomTime(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
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