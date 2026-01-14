package aeronpcd.model;

/**
 * Enumerado que define los posibles estados de un Avión (Airplane)
 * durante su ciclo de vida en la simulación.
 * Se utiliza para la sincronización con la Torre y la visualización en
 * pantalla.
 */
public enum FlightStatus {
    /** El avión está volando hacia el aeropuerto. Estado inicial. */
    IN_FLIGHT,

    /** El avión ha solicitado aterrizar y espera respuesta de la Torre. */
    LANDING_REQUEST,

    /** La Torre ha asignado Pista y Puerta. Permiso concedido. */
    LANDING_ASSIGNED,

    /** El avión está ejecutando la maniobra de aterrizaje (Ocupa pista). */
    LANDING,

    /** El avión ha liberado la pista y está en rodaje o aparcado. */
    LANDED,

    /** El proceso de embarque de pasajeros ha comenzado. */
    BOARDING,

    /** El embarque ha finalizado y la Puerta se ha liberado. */
    BOARDED,

    /** El avión solicita permiso para despegar. */
    TAKEOFF_REQUESTED,

    /** La Torre ha asignado una Pista para el despegue. */
    TAKEOFF_ASSIGNED,

    /** El avión está ejecutando la maniobra de despegue (Ocupa pista). */
    DEPARTING,

    /** El avión ha despegado y salido del espacio aéreo controlado. */
    DEPARTED
}