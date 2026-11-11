package aeronpcd.util;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simula un Panel de Información de Vuelos (FIDS - Flight Information Display
 * System).
 * Mantiene un registro en tiempo real de los estados de cada vuelo y los
 * persiste
 * en un archivo JSON para que puedan ser consumidos por otros sistemas o
 * visualizadores externos.
 * 
 * Es una clase de utilidad estática (Singleton implícito).
 */
public class FlightPanel {
    // Mapa Thread-Safe para almacenar el estado de cada vuelo (ID -> Estado)
    private static final Map<String, String> flights = new ConcurrentHashMap<>();

    // Ruta del archivo JSON, configurable según el modo de la simulación
    private static String filePath = "flight_board.json";

    /**
     * Configura la ruta del archivo JSON.
     * Ahora recibe la ruta completa para vincularla con el ID de la ejecución (Log).
     * 
     * @param fullPath Ruta completa del archivo JSON (sin extensión o con ella, manejada por el caller).
     */
    public static void setup(String fullPath) {
        flights.clear();
        filePath = fullPath;
    }

    /**
     * @deprecated Usar setup(String fullPath) directamente.
     */
    @Deprecated
    public static void setupCustomPath(String customPath) {
        setup(customPath);
    }

    /**
     * Actualiza el estado de un vuelo en memoria y persiste el cambio en el fichero
     * JSON.
     * Este método es Thread-Safe. La escritura se realiza en un bloque sincronizado
     * y utiliza la misma lógica que antes, pero añade un pequeño control de errores
     * mejorado.
     * 
     * @param flightId Identificador del vuelo (ej. IBE-001)
     * @param status   Nuevo estado (ej. LANDED, BOARDING)
     */
    public static void updateStatus(String flightId, String status) {
        flights.put(flightId, status); // Actualiza mapa en memoria
        try {
            saveToJson(); // Persiste a disco
        } catch (Exception e) {
            // Evitar que un error de IO detenga el hilo del avión
            System.err.println("Error no crítico al actualizar panel: " + e.getMessage());
        }
    }

    /**
     * Guarda el estado actual de todos los vuelos en un fichero JSON.
     * Synchronized para evitar que dos hilos escriban en el archivo simultáneamente
     * y lo corrompan.
     */
    private static synchronized void saveToJson() {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        int count = 0;
        int size = flights.size();
        for (Map.Entry<String, String> entry : flights.entrySet()) {
            json.append("  \"").append(entry.getKey()).append("\": \"").append(entry.getValue()).append("\"");
            if (++count < size)
                json.append(",");
            json.append("\n");
        }
        json.append("}");

        try (FileWriter writer = new FileWriter(filePath)) {
            writer.write(json.toString());
        } catch (IOException e) {
            // REQUISITO PRÁCTICA 6: Captura de excepción de Panel
            System.err.println("No se ha actualizado el panel de vuelos. Fichero JSON no encontrado");
        }
    }
}
