package aeronpcd.driver;

import aeronpcd.gui.AirportWindow;
import aeronpcd.model.Airplane;
import aeronpcd.resources.Gate;
import aeronpcd.resources.Runway;
import aeronpcd.tower.ControlTower;
import aeronpcd.util.Logger;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;

public class AirportDriver {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // 1. MENÚ DE CONFIGURACIÓN INICIAL
        System.out.println("============================================");
        System.out.println("   AERON - CONFIGURACIÓN DE SIMULACIÓN      ");
        System.out.println("============================================");

        System.out.println("Selecciona el Modo de Ejecución:");
        System.out.println("  1. SECUENCIAL");
        System.out.println("  2. CONCURRENTE");
        
        int opcionModo = readInt(scanner, ">> Opción: ", 1, 2);

        String modo;
        if (opcionModo == 1) {
            modo = "SECUENCIAL";
        } else {
            modo = "CONCURRENTE";
        }

        int numAviones = readInt(scanner, "\nNúmero de AVIONES (ej. 1-100): ", 1, 100);
        int numPistas = readInt(scanner, "Número de PISTAS (ej. 1-10): ", 1, 10);
        int numPuertas = readInt(scanner, "Número de PUERTAS (ej. 1-20): ", 1, 20);

        int numOperarios = 1;
        // En secuencial no hay operarios concurrentes reales, pero el log pide el
        // número.
        // En concurrente, preguntamos cuántos hilos queremos.
        if (modo.equals("CONCURRENTE")) {
            numOperarios = readInt(scanner, "Número de OPERARIOS (ej. 1-20): ", 1, 20);
        } else {
            // En secuencial simulamos que hay 1 para el nombre del log
            numOperarios = 1;
        }

        System.out.println("\n--------------------------------------------");
        System.out.println("Iniciando simulación " + modo + "...");
        System.out.println("--------------------------------------------");

        // 2. CONFIGURACIÓN DEL NOMBRE DEL LOG (Requisito estricto)
        String timeStamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        
        // Formato: aeron-MODE-N1AV-N2PIS-N3PUE[-N4OPE-]-TIMESTAMP
        String logName;
        if (modo.equals("CONCURRENTE")) {
            logName = String.format("concurrente/aeron-%s-%d-%d-%d-%d-%s",
                    modo, numAviones, numPistas, numPuertas, numOperarios, timeStamp);
        } else {
            // En secuencial no incluimos el número de operarios en el nombre
            logName = String.format("secuencial/aeron-%s-%d-%d-%d-%s",
                    modo, numAviones, numPistas, numPuertas, timeStamp);
        }

        Logger.setup(logName);
        // Configuramos también el Panel de Vuelos para que use un JSON distinto
         aeronpcd.util.FlightPanel.setup(modo);

        Logger.log("=== INICIO SIMULACIÓN (" + modo + ") ===");
        Logger.log("Configuración: " + numAviones + " Aviones, " + numPistas + " Pistas, " +
                numPuertas + " Puertas, " + numOperarios + " Operarios.");

        // 3. INICIALIZAR ENTORNO
        AirportWindow window = new AirportWindow();

        List<Runway> runways = new ArrayList<>();
        for (int i = 1; i <= numPistas; i++)
            runways.add(new Runway("PIS" + i));

        List<Gate> gates = new ArrayList<>();
        for (int i = 1; i <= numPuertas; i++)
            gates.add(new Gate("PUE" + i));

        ControlTower tower = new ControlTower(runways, gates);
        tower.setWindow(window);

        // 4. CREAR AVIONES
        List<Airplane> airplanes = new ArrayList<>();
        for (int i = 1; i <= numAviones; i++) {
            airplanes.add(new Airplane(String.format("IBE-%03d", i), tower));
        }

        long simulationStart = System.currentTimeMillis();

        // 5. EJECUCIÓN SEGÚN MODO
        if (modo.equals("SECUENCIAL")) {
            // Modo Secuencial: Uno detrás de otro
            for (Airplane plane : airplanes) {
                plane.runSequential();
            }
        } else {
            // Modo Concurrente: Todos a la vez
            System.out.println("Lanzando " + numAviones + " aviones en paralelo...");
            for (Airplane plane : airplanes) {
                plane.start();
            }
            
            // Esperar a que terminen (Join) con feedback visual
            System.out.println("Esperando finalización de vuelos...");
            int finalizados = 0;
            for (Airplane plane : airplanes) {
                try {
                    plane.join();
                    finalizados++;
                    if (finalizados % 5 == 0 || finalizados == numAviones) {
                        System.out.println("Progreso: " + finalizados + "/" + numAviones + " aviones finalizados.");
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("Todos los aviones han aterrizado y despegado.");
        }

        long simulationEnd = System.currentTimeMillis();
        long totalDuration = simulationEnd - simulationStart; // Duración total experimental

        Logger.log("=== FIN DE LA SIMULACIÓN ===");
        Logger.log("TIEMPO TOTAL: " + totalDuration + " ms");
        Logger.close(); // Cerramos el fichero de log de forma limpia

        // 6. GENERAR CSV (Práctica 6: Resultados y archivo de salida)
        generateCSV(airplanes, modo);

        scanner.close();

        System.out.println("Simulación finalizada con éxito. Cerrando aplicación...");
        System.exit(0);
    }

    /**
     * Genera un archivo CSV con el resumen de la simulación.
     * Ordena los aviones por tiempo total de menor a mayor y genera un ranking.
     * 
     * @param airplanes Lista de aviones que han completado (o intentado) la simulación.
     * @param modo Modo de ejecución para generar el nombre del archivo.
     */
    private static void generateCSV(List<Airplane> airplanes, String modo) {
        // Se guarda dentro de la carpeta logs/secuencial o logs/concurrente
        String fileName = "logs/" + modo.toLowerCase() + "/aeropuerto_resumen.csv";
        
        // Ordenamos la lista de aviones por su tiempo total de operación (ascendente)
        airplanes.sort(Comparator.comparingLong(Airplane::getTotalTime));

        try (FileWriter writer = new FileWriter(fileName)) {
            // Cabecera del CSV
            writer.write("Avión;Tiempo total (ms);Observaciones\n");
            
            int rank = 1;
            for (Airplane plane : airplanes) {
                // Escribe una fila por avión: ID, Tiempo Total, Posición en el ranking
                writer.write(String.format("%s;%d;%dº\n",
                        plane.getFlightId(), plane.getTotalTime(), rank));
                rank++;
            }
            System.out.println("Archivo CSV generado correctamente: " + fileName);
        } catch (IOException e) {
            // PRÁCTICA 6: Gestión de excepción de escritura CSV
            System.err.println("Error al escribir el CSV: " + e.getMessage());
        }
    }

    /**
     * Lee un entero por consola asegurando que esté dentro de un rango determinado.
     * Repite la solicitud hasta obtener un valor válido.
     */
    private static int readInt(Scanner scanner, String prompt, int min, int max) {
        int value;
        while (true) {
            System.out.print(prompt);
            if (scanner.hasNextInt()) {
                value = scanner.nextInt();
                if (value >= min && value <= max) {
                    break;
                } else {
                    System.out.println("Error: El valor debe estar entre " + min + " y " + max + ".");
                }
            } else {
                // Si se cierra el scanner (por ejemplo, Ctrl+C), hasNext() es false
                if (!scanner.hasNext()) {
                    System.out.println("\nEntrada finalizada. Cerrando programa...");
                    System.exit(0);
                }

                // Capturamos la entrada que falló para analizarla
                String input = scanner.next();
                
                // Si solo contiene dígitos, es un número válido pero demasiado grande (Overflow)
                if (input.matches("-?\\d+")) {
                     System.out.println("Error: El número es demasiado grande para ser procesado.");
                } else {
                     System.out.println("Error: Entrada no válida ('" + input + "'). Introduce un número entero.");
                }
            }
        }
        return value;
    }
}