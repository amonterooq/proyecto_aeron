package aeronpcd.resources;

public class Gate {
    private String id;
    private boolean occupied;
    private String planeId;

    public Gate(String id) {
        this.id = id;
        this.occupied = false;
        this.planeId = null;
    }

    public String getId() {
        return id;
    }

    public boolean isOccupied() {
        return occupied;
    }

    public void occupy(String planeId) {
        this.occupied = true;
        this.planeId = planeId;
    }

    public void free() {
        this.occupied = false;
        this.planeId = null;
    }

    @Override
    public String toString() {
        return "Puerta " + id + (occupied ? " (Ocupada por " + planeId + ")" : " (Libre)");
    }
}