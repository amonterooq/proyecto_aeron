package aeronpcd.resources;

public class Runway {
    private String id;
    private boolean available; // true = libre, false = ocupada
    private String occupiedBy; // ID del avión que la usa

    public Runway(String id) {
        this.id = id;
        this.available = true;
        this.occupiedBy = null;
    }

    public String getId() {
        return id;
    }

    public boolean isAvailable() {
        return available;
    }

    // Métodos simples para ocupar/liberar (sin synchronized por ahora)
    public void occupy(String planeId) {
        this.available = false;
        this.occupiedBy = planeId;
    }

    public void free() {
        this.available = true;
        this.occupiedBy = null;
    }

    @Override
    public String toString() {
        return "Pista " + id + (available ? " (Libre)" : " (Ocupada por " + occupiedBy + ")");
    }
}