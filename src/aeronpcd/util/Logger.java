package aeronpcd.util;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Logger {
    private static PrintWriter fileWriter;

    // Inicializa el logger creando el fichero con el timestamp actual
    public static void setup(String mode) {
        String timeStamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        // Formato: logs/secuencial/aeron-secuencial-[time].log
        String fileName = "logs/" + mode.toLowerCase() + "/aeron-" + mode.toLowerCase() + "-" + timeStamp + ".log";

        try {
            fileWriter = new PrintWriter(new FileWriter(fileName, true));
            System.out.println("Log iniciado en: " + fileName);
        } catch (IOException e) {
            System.err.println("Error creando el fichero de log: " + e.getMessage());
        }
    }

    // Método para escribir eventos
    public static void log(String message) {
        // Escribir en consola
        System.out.println(message);

        // Escribir en fichero si está inicializado
        if (fileWriter != null) {
            fileWriter.println(message);
            fileWriter.flush(); // Forzar escritura inmediata
        }
    }

    // Cerrar el fichero al final
    public static void close() {
        if (fileWriter != null) {
            fileWriter.close();
        }
    }
}