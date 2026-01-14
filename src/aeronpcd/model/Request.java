package aeronpcd.model;

// Clase que representa un mensaje/petición entre el Avión y la Torre
public class Request {
    // Tipos de mensajes posibles
    public enum Type {
        LANDING, LANDED, BOARDING, BOARDED, TAKEOFF, DEPARTED
    }

    private Airplane plane;
    private Type type;

    public Request(Airplane plane, Type type) {
        this.plane = plane;
        this.type = type;
    }

    public Airplane getPlane() {
        return plane;
    }

    public Type getType() {
        return type;
    }

    @Override
    public String toString() {
        // CORREGIDO: getFlightId()
        return type + " -> " + plane.getFlightId();
    }
}