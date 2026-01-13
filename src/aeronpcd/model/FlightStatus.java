package aeronpcd.model;

public enum FlightStatus {
    IN_FLIGHT, // En vuelo
    LANDING_REQUEST, // Aterrizaje solicitado (Esperando respuesta)
    LANDING_ASSIGNED, // Aterrizaje autorizado (Tiene pista y puerta asignada)
    LANDING, // Aterrizando (Ocupando pista)
    LANDED, // Aterrizado (Ya en tierra, libera pista)
    BOARDING, // Embarcando (Pasajeros subiendo)
    BOARDED, // Embarcado (Listo para pedir salida)
    TAKEOFF_REQUESTED, // Despegue solicitado
    TAKEOFF_ASSIGNED, // Despegue autorizado (Tiene pista asignada)
    DEPARTING, // Despegando (Ocupando pista)
    DEPARTED // Despegado (Fin del ciclo)
}