package aeronpcd.model;

public class Passenger {
    private String id;

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