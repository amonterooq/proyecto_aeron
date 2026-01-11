package aeronpcd.model;

import aeronpcd.resources.Gate;
import aeronpcd.resources.Runway;
import aeronpcd.tower.ControlTower;
import aeronpcd.util.Logger;

public class Airplane {
    private String id;
    private ControlTower tower;
    private FlightStatus status;
    private Passenger passenger; // Representación de pasajeros

    // Recursos asignados temporalmente
    private Runway currentRunway;
    private Gate currentGate;

    // Tiempos para estadísticas
    private long startTime;
    private long endTime;

    public Airplane(String id, ControlTower tower) {
        this.id = id;
        this.tower = tower;
        this.status = FlightStatus.IN_FLIGHT;
        this.passenger = new Passenger("Pasajero-" + id);
    }

    public String getId() {
        return id;
    }

    public FlightStatus getStatus() {
        return status;
    }

    public void setStatus(FlightStatus status) {
        this.status = status;
    }

    // Métodos para recibir recursos de la torre
    public void assignResources(Runway runway, Gate gate) {
        this.currentRunway = runway;
        this.currentGate = gate;
        this.status = FlightStatus.LANDING_ASSIGNED;
        Logger.log("Avión [" + id + " - " + status + "] Asignado a " + runway.getId() + " y " + gate.getId());
    }

    public void assignTakeoffRunway(Runway runway) {
        this.currentRunway = runway;
        this.status = FlightStatus.TAKEOFF_ASSIGNED;
        Logger.log("Avión [" + id + " - " + status + "] Asignado a " + runway.getId() + " para despegue.");
    }

    // --- Lógica del Ciclo de Vida (Secuencial) ---
    // Este método ejecuta todo el flujo de golpe (para el modo secuencial)
    public void runSequential() {
        this.startTime = System.currentTimeMillis();
        Logger.log("Avión [" + id + " - " + status + "] Inicia ciclo.");

        // 1. Solicitar Aterrizaje
        tower.requestLanding(this);

        if (currentRunway != null && currentGate != null) {
            // 2. Aterrizar
            performLanding();

            // 3. Ir a puerta y estacionar
            performDocking();

            // 4. Embarque
            performBoarding();

            // 5. Salir de puerta y pedir despegue
            performUndocking();
            tower.requestTakeoff(this);

            if (currentRunway != null) {
                // 6. Despegar
                performTakeoff();
            }
        }

        this.endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        Logger.log("Avión [" + id + " - " + status + "] Ciclo completado en " + duration + " ms.");
    }

    // --- Acciones individuales simuladas ---

    private void performLanding() {
        this.status = FlightStatus.LANDING;
        currentRunway.occupy(this.id);
        Logger.log("Avión [" + id + " - " + status + "] Aterrizando en " + currentRunway.getId() + " (100ms)...");
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
        } // Simula tiempo

        this.status = FlightStatus.LANDED;
        currentRunway.free(); // Libera pista al terminar aterrizaje
        tower.notifyLandingCompleted(this);
        Logger.log("Avión [" + id + " - " + status + "] Aterrizado.");
    }

    private void performDocking() {
        currentGate.occupy(this.id);
        Logger.log("Avión [" + id + "] Ocupa " + currentGate.getId());
    }

    private void performBoarding() {
        this.status = FlightStatus.BOARDING;
        Logger.log("Avión [" + id + " - " + status + "] Pasajeros subiendo...");
        // Simulación breve de embarque
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
        }

        this.status = FlightStatus.BOARDED;
        Logger.log("Avión [" + id + " - " + status + "] Embarque completado.");
    }

    private void performUndocking() {
        currentGate.free();
        Logger.log("Avión [" + id + "] Libera " + currentGate.getId());
    }

    private void performTakeoff() {
        this.status = FlightStatus.DEPARTING;
        currentRunway.occupy(this.id);
        Logger.log("Avión [" + id + " - " + status + "] Despegando en " + currentRunway.getId() + " (100ms)...");
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
        }

        currentRunway.free();
        this.status = FlightStatus.DEPARTED;
        tower.notifyDeparture(this);
        Logger.log("Avión [" + id + " - " + status + "] Despegue exitoso.");
    }

    @Override
    public String toString() {
        return "Avión " + id + " [" + status + "]";
    }
}