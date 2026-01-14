package aeronpcd.model;

/**
 * Representa a un pasajero individual.
 * Clase modelo preparada para futura expansión de la simulación (ej: gestión de
 * terminales).
 * Actualmente actúa como contenedor simple de identidad.
 */
public class Passenger {
    private String id;

    /**
     * Crea un nuevo pasajero con identificador único.
     * 
     * @param id Identificador del pasajero (ej: DNI o Pasaporte)
     */
    public Passenger(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return "Pasajero " + id;
    }
}