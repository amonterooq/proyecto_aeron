package aeronpcd.util;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FlightPanel {
    // Usamos ConcurrentHashMap porque varios Operarios (hilos) escribirán a la vez
    private static final Map<String, String> flights = new ConcurrentHashMap<>();
    private static final String FILE_PATH = "flight_board.json";

    // Método que llama la Torre para actualizar un avión
    public static void updateStatus(String flightId, String status) {
        flights.put(flightId, status);
        saveToJson();
    }

    // Método privado que vuelca el mapa al fichero simulando formato JSON
    private static synchronized void saveToJson() {
        StringBuilder json = new StringBuilder();
        json.append("{\n");

        int count = 0;
        int size = flights.size();

        for (Map.Entry<String, String> entry : flights.entrySet()) {
            json.append("  \"").append(entry.getKey()).append("\": \"").append(entry.getValue()).append("\"");

            // Añadir coma si no es el último elemento
            if (++count < size) {
                json.append(",");
            }
            json.append("\n");
        }

        json.append("}");

        // Escribir al fichero (sobrescribiéndolo)
        try (FileWriter writer = new FileWriter(FILE_PATH)) {
            writer.write(json.toString());
        } catch (IOException e) {
            System.err.println("Error actualizando el panel de vuelos: " + e.getMessage());
        }
    }
}