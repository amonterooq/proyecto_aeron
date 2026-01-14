package aeronpcd.util;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FlightPanel {
    private static final Map<String, String> flights = new ConcurrentHashMap<>();
    private static final String FILE_PATH = "flight_board.json";

    public static void updateStatus(String flightId, String status) {
        flights.put(flightId, status);
        saveToJson();
    }

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

        try (FileWriter writer = new FileWriter(FILE_PATH)) {
            writer.write(json.toString());
        } catch (IOException e) {
            // REQUISITO PRÁCTICA 6: Captura de excepción de Panel
            System.err.println("No se ha actualizado el panel de vuelos. Fichero JSON no encontrado");
        }
    }
}