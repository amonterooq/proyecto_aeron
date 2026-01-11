package aeronpcd.model;

public enum FlightStatus {
    IN_FLIGHT, // En vuelo, iniciando
    LANDING_REQUESTED, // Solicitud enviada a torre [cite: 163]
    LANDING_ASSIGNED, // Torre asignó pista y puerta [cite: 177]
    LANDING, // Aterrizando (duración 100ms) [cite: 201]
    LANDED, // Ya en tierra, libera pista [cite: 211]
    BOARDING, // Pasajeros subiendo [cite: 227]
    BOARDED, // Embarque completo [cite: 240]
    TAKEOFF_REQUESTED, // Pide despegar [cite: 259]
    TAKEOFF_ASSIGNED, // Torre asigna pista despegue [cite: 274]
    DEPARTING, // Despegando (duración 100ms) [cite: 296]
    DEPARTED // Fuera del sistema [cite: 308]
}