package aeronpcd.model;

/**
 * Representa una solicitud de servicio enviada por un Avión a la Torre de
 * Control.
 * Encapsula el avión que realiza la petición y el tipo de operación requerida.
 * Este objeto se encola en la lista de espera de la Torre.
 */
public class Request {

    /**
     * Enumerado con los tipos de peticiones posibles en el ciclo de vida del vuelo.
     */
    public enum Type {
        /** Solicitud para aterrizar (Requiere Pista y Puerta) */
        LANDING,
        /** Notificación de fin de aterrizaje (Libera Pista) */
        LANDED,
        /** Notificación de inicio de embarque */
        BOARDING,
        /** Notificación de fin de embarque (Libera Puerta) */
        BOARDED,
        /** Solicitud para despegar (Requiere Pista) */
        TAKEOFF,
        /** Notificación de fin de despegue (Libera Pista) y salida del sistema */
        DEPARTED
    }

    private Airplane plane;
    private Type type;

    /**
     * Constructor de una nueva petición.
     * 
     * @param plane El avión que genera la solicitud.
     * @param type  El tipo de acción que solicita u operación que notifica.
     */
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
        return type + " -> " + plane.getFlightId();
    }
}