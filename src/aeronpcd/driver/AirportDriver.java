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
        System.out.print(">> Opción: ");

        int opcionModo = scanner.nextInt();

        String modo;
        if (opcionModo == 1) {
            modo = "SECUENCIAL";
        } else {
            modo = "CONCURRENTE";
        }

        System.out.print("\nNúmero de AVIONES (ej. 20): ");
        int numAviones = scanner.nextInt();

        System.out.print("Número de PISTAS (ej. 3): ");
        int numPistas = scanner.nextInt();

        System.out.print("Número de PUERTAS (ej. 5): ");
        int numPuertas = scanner.nextInt();

        int numOperarios = 1;
        // En secuencial no hay operarios concurrentes reales, pero el log pide el
        // número.
        // En concurrente, preguntamos cuántos hilos queremos.
        if (modo.equals("CONCURRENTE")) {
            System.out.print("Número de OPERARIOS (ej. 5): ");
            numOperarios = scanner.nextInt();
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
        String logName = String.format("aeron-%s-%d-%d-%d-%d-%s",
                modo, numAviones, numPistas, numPuertas, numOperarios, timeStamp);

        Logger.setup(logName);
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
            for (Airplane plane : airplanes) {
                plane.start();
            }
            // Esperar a que terminen (Join)
            for (Airplane plane : airplanes) {
                try {
                    plane.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        long simulationEnd = System.currentTimeMillis();
        long totalDuration = simulationEnd - simulationStart;

        Logger.log("=== FIN DE LA SIMULACIÓN ===");
        Logger.log("TIEMPO TOTAL: " + totalDuration + " ms");
        Logger.close();

        // 6. GENERAR CSV
        generateCSV(airplanes);

        scanner.close();
    }

    private static void generateCSV(List<Airplane> airplanes) {
        String fileName = "aeropuerto_resumen.csv";
        airplanes.sort(Comparator.comparingLong(Airplane::getTotalTime));

        try (FileWriter writer = new FileWriter(fileName)) {
            writer.write("Avión;Tiempo total (ms);Observaciones\n");
            int rank = 1;
            for (Airplane plane : airplanes) {
                writer.write(String.format("%s;%d;%dº\n",
                        plane.getFlightId(), plane.getTotalTime(), rank));
                rank++;
            }
            System.out.println("Archivo CSV generado correctamente: " + fileName);
        } catch (IOException e) {
            System.err.println("Error al escribir el CSV: " + e.getMessage());
        }
    }
}