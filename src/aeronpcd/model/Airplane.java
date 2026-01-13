package aeronpcd.model;

import aeronpcd.resources.Gate;
import aeronpcd.resources.Runway;
import aeronpcd.tower.ControlTower;
import aeronpcd.util.Logger;

public class Airplane {
    private String id;
    private ControlTower tower;
    private FlightStatus status;

    private Runway currentRunway;
    private Gate currentGate;

    private long startTime;
    private long endTime;

    public Airplane(String id, ControlTower tower) {
        this.id = id;
        this.tower = tower;
        this.status = FlightStatus.IN_FLIGHT;
    }

    // --- MÉTODOS DE SINCRONIZACIÓN Y CICLO ---

    public void runSequential() {
        this.startTime = System.currentTimeMillis();
        Logger.log("Aviones\n  Avión [" + id + " - " + status + "] Inicia ciclo");

        // 1. SOLICITAR ATERRIZAJE (Bloqueante)
        this.status = FlightStatus.LANDING_REQUEST;
        Logger.log("Aviones\n  Avión [" + id + "] Solicita aterrizaje y ESPERA...");
        sendRequestAndWait(Request.Type.LANDING);

        if (this.status == FlightStatus.LANDING_ASSIGNED) {
            Logger.log("Aviones\n  Avión [" + id + "] Recibe luz verde. Asignado a " + currentRunway.getId());

            // 2. ATERRIZAR
            performAction(100, "Aterrizando", FlightStatus.LANDING);
            sendRequestAndWait(Request.Type.LANDED);

            // 3. EMBARCAR
            performAction(50, "Embarcando", FlightStatus.BOARDING);
            // El boarding no requiere recurso nuevo, pero notificamos para cambiar estado
            sendRequestAndWait(Request.Type.BOARDING);

            // Fin embarque
            sendRequestAndWait(Request.Type.BOARDED);

            // 4. SOLICITAR DESPEGUE (Bloqueante)
            this.status = FlightStatus.TAKEOFF_REQUESTED;
            Logger.log("Aviones\n  Avión [" + id + "] Solicita despegue y ESPERA...");
            sendRequestAndWait(Request.Type.TAKEOFF);

            if (this.status == FlightStatus.TAKEOFF_ASSIGNED) {
                Logger.log("Aviones\n  Avión [" + id + "] Recibe luz verde. Despegue en " + currentRunway.getId());
                performAction(100, "Despegando", FlightStatus.DEPARTING);
                sendRequestAndWait(Request.Type.DEPARTED);
            }
        } else {
            Logger.log("Aviones\n  Avión [" + id + "] No pudo aterrizar (Sin recursos).");
        }

        this.endTime = System.currentTimeMillis();
        Logger.log("Aviones\n  Avión [" + id + " - DEPARTED] Ciclo completado en " + (endTime - startTime) + " ms");
    }

    // --- MÉTODO CLAVE PARA ESPERAR AL OPERARIO ---
    private void sendRequestAndWait(Request.Type type) {
        synchronized (this) {
            // Enviamos petición
            tower.addRequest(new Request(this, type));

            try {
                // Nos dormimos hasta que el Operario nos haga notify()
                this.wait();
            } catch (InterruptedException e) {
                Logger.log("Avión [" + id + "] Interrumpido mientras esperaba.");
            }
        }
    }

    private void performAction(int time, String actionName, FlightStatus tempStatus) {
        this.status = tempStatus;
        // Solo log visual, no cambia estado en torre aun
        Logger.log("Aviones\n  Avión [" + id + " - " + status + "] " + actionName + " (" + time + "ms)...");
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
        }
    }

    // Getters y Setters
    public String getId() {
        return id;
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