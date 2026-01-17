package aeronpcd.resources;

/**
 * Representa una pista de aterrizaje/despegue del aeropuerto.
 * Es un recurso compartido crítico que debe ser gestionado con exclusión mutua
 * por la Torre de Control para evitar colisiones.
 */
public class Runway {
    private String id;
    private volatile boolean available; // true = libre, false = ocupada
    private String occupiedBy; // ID del avión que la usa

    /**
     * Constructor de la pista.
     * 
     * @param id Identificador único (ej. "PIS1")
     */
    public Runway(String id) {
        this.id = id;
        this.available = true;
        this.occupiedBy = null;
    }

    public String getId() {
        return id;
    }

    /**
     * Verifica si la pista está libre.
     * 
     * @return true si está disponible, false si está ocupada.
     */
    public boolean isAvailable() {
        return available;
    }

    /**
     * Marca la pista como ocupada por un avión específico.
     * Nota: La sincronización (monitor) se gestiona en la ControlTower, no aquí,
     * para permitir operaciones atómicas complejas (Pista + Puerta).
     * 
     * @param planeId Identificador del avión que ocupa la pista.
     */
    public void occupy(String planeId) {
        this.available = false;
        this.occupiedBy = planeId;
    }

    /**
     * Libera la pista, dejándola disponible para el siguiente avión.
     */
    public void free() {
        this.available = true;
        this.occupiedBy = null;
    }

    @Override
    public String toString() {
        return "Pista " + id + (available ? " (Libre)" : " (Ocupada por " + occupiedBy + ")");
    }
}