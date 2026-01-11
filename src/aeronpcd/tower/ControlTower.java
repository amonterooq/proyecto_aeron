package aeronpcd.tower;

import aeronpcd.model.Airplane;
import aeronpcd.model.FlightStatus;
import aeronpcd.resources.Gate;
import aeronpcd.resources.Runway;
import aeronpcd.util.Logger;
import java.util.List;

public class ControlTower {
    private List<Runway> runways;
    private List<Gate> gates;

    public ControlTower(List<Runway> runways, List<Gate> gates) {
        this.runways = runways;
        this.gates = gates;
    }

    // Método simple para asignar una pista libre (sin sincronización real aún)
    public Runway getFreeRunway() {
        for (Runway r : runways) {
            if (r.isAvailable()) {
                return r;
            }
        }
        return null; // O manejar espera en el futuro
    }

    // Método simple para asignar una puerta libre
    public Gate getFreeGate() {
        for (Gate g : gates) {
            if (!g.isOccupied()) {
                return g;
            }
        }
        return null;
    }

    // Simulación de procesamiento de entrada (Aterrizaje)
    public void requestLanding(Airplane plane) {
        Logger.log("Torre: Recibida solicitud de aterrizaje del avión " + plane.getId());
        plane.setStatus(FlightStatus.LANDING_REQUESTED);

        // En la versión secuencial, asignamos recursos inmediatamente si hay libres
        Runway runway = getFreeRunway();
        Gate gate = getFreeGate();

        if (runway != null && gate != null) {
            Logger.log("Torre: Avión " + plane.getId() + " autorizado a aterrizar en " + runway.getId() + " y puerta "
                    + gate.getId());
            plane.assignResources(runway, gate);
        } else {
            Logger.log("Torre: No hay recursos disponibles para el avión " + plane.getId());
        }
    }

    // Simulación de procesamiento de salida (Despegue)
    public void requestTakeoff(Airplane plane) {
        Logger.log("Torre: Recibida solicitud de despegue del avión " + plane.getId());
        plane.setStatus(FlightStatus.TAKEOFF_REQUESTED);

        Runway runway = getFreeRunway();
        if (runway != null) {
            Logger.log("Torre: Avión " + plane.getId() + " autorizado a despegar en " + runway.getId());
            plane.assignTakeoffRunway(runway);
        }
    }

    // Métodos para notificar liberación de recursos (Logging)
    public void notifyLandingCompleted(Airplane plane) {
        Logger.log("Torre: Avión " + plane.getId() + " ha aterrizado y liberado la pista.");
    }

    public void notifyDeparture(Airplane plane) {
        Logger.log("Torre: Avión " + plane.getId() + " ha despegado y salido del espacio aéreo.");
    }
}