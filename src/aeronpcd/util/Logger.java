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
        // Carpeta raíz
        String directoryPath = "logs";

        // El driver nos pasa "carpeta/nombre_log", así que construimos la ruta completa
        String fileName = directoryPath + "/" + fullLogName + ".log";

        // Aseguramos que existan todas las carpetas padres (logs/secuencial,
        // logs/concurrente)
        File file = new File(fileName);
        if (file.getParentFile() != null) {
            file.getParentFile().mkdirs();
        }

        try {
            fileWriter = new PrintWriter(new FileWriter(fileName, true));
            System.out.println("Log iniciado en: " + fileName);
        } catch (IOException e) {
            System.err.println("Error: No se ha encontrado el archivo de log " + fileName);
        }
    }

    /**
     * Registra un mensaje con marca de tiempo y nombre del hilo actual.
     * Método sincronizado para evitar que las líneas se entremezclen en el archivo.
     * 
     * @param message El mensaje a registrar.
     */
    public static synchronized void log(String message) {
        // Formato simplificado sin timestamp/thread en consola si el mensaje ya lo trae
        // Pero para cumplir estrictamente con el formato solicitado, asumimos que
        // 'message' es el contenido puro y aquí le añadimos el contexto.

        // Si el usuario quiere mensajes MUY específicos sin prefijos estándar,
        // deberíamos detectar si es un mensaje de sistema o de log normal.
        // Por ahora mantenemos el formato robusto de depuración.

        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
        String threadName = Thread.currentThread().getName();

        // Formato: [HORA] [HILO] Mensaje
        String formattedMessage = String.format("[%s] [%-10s] %s", time, threadName, message);

        System.out.println(formattedMessage);
        System.out.flush(); // Forzar salida a consola inmediatamente

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