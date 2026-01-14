package aeronpcd.util;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Logger {
    private static PrintWriter fileWriter;

    // Configura el log al inicio (crea carpeta y fichero con timestamp)
    public static void setup(String fullLogName) {
        // Creamos carpeta logs si no existe
        String directoryPath = "logs";
        new java.io.File(directoryPath).mkdirs();

        // El driver ya nos pasa el nombre con formato correcto + timestamp
        String fileName = directoryPath + "/" + fullLogName + ".log";

        try {
            fileWriter = new PrintWriter(new FileWriter(fileName, true));
            System.out.println("Log iniciado en: " + fileName);
        } catch (IOException e) {
            System.err.println("Error: No se ha encontrado el archivo de log " + fileName);
        }
    }

    // Método estático para escribir en consola y fichero a la vez
    public static void log(String message) {
        // PRÁCTICA 7: Añadir timestamp preciso a cada línea
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
        String threadName = Thread.currentThread().getName();

        // Formato: [HORA] [HILO] Mensaje
        String formattedMessage = String.format("[%s] [%-10s] %s", time, threadName, message);

        System.out.println(formattedMessage);

        if (fileWriter != null) {
            fileWriter.println(formattedMessage);
            fileWriter.flush();
        }
    }

    public static void close() {
        if (fileWriter != null)
            fileWriter.close();
    }
}