package aeronpcd.model;

public class Request {
    // Definimos los tipos de petición que la torre sabe procesar
    public enum Type {
        LANDING, // Petición para aterrizar
        LANDED, // Notificación de que ha terminado de aterrizar
        BOARDING, // Notificación de inicio de embarque
        BOARDED, // Notificación de fin de embarque
        TAKEOFF, // Petición para despegar
        DEPARTED // Notificación de que ha salido del sistema
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
        return type + " -> " + plane.getId();
    }
}