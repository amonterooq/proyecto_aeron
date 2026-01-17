package aeronpcd.resources;

/**
 * Representa una puerta de embarque del aeropuerto.
 * Recurso limitado donde los aviones estacionan para embarcar pasajeros.
 */
public class Gate {
    private String id;
    private volatile boolean occupied;
    private String planeId;

    /**
     * Constructor de la puerta.
     * 
     * @param id Identificador único (ej. "PUE1")
     */
    public Gate(String id) {
        this.id = id;
        this.occupied = false;
        this.planeId = null;
    }

    public String getId() {
        return id;
    }

    /**
     * Verifica si la puerta está ocupada.
     * 
     * @return true si hay un avión, false si está libre.
     */
    public boolean isOccupied() {
        return occupied;
    }

    /**
     * Asigna la puerta a un avión.
     * La lógica de exclusión mutua reside en la ControlTower.
     * 
     * @param planeId Identificador del avión.
     */
    public void occupy(String planeId) {
        this.occupied = true;
        this.planeId = planeId;
    }

    /**
     * Libera la puerta tras el embarque.
     */
    public void free() {
        this.occupied = false;
        this.planeId = null;
    }

    @Override
    public String toString() {
        return "Puerta " + id + (occupied ? " (Ocupada por " + planeId + ")" : " (Libre)");
    }
}