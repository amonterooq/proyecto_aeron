package aeronpcd.test;

import aeronpcd.model.Airplane;
import aeronpcd.model.FlightStatus;
import aeronpcd.resources.Gate;
import aeronpcd.resources.Runway;
import aeronpcd.tower.ControlTower;
import aeronpcd.util.FlightPanel;
import aeronpcd.util.Logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.nio.file.Files;
import java.nio.file.Path;

public class TestRunner {

    private static final String MODE_SEQ = "SECUENCIAL";
    private static final String MODE_CON = "CONCURRENTE";

    public static void main(String[] args) {
        // Un bloqueo de concurrencia debe fallar en CI, no esperar indefinidamente.
        Thread watchdog = new Thread(() -> {
            try {
                Thread.sleep(300_000);
                System.err.println("FAIL: la batería excedió el límite de 300 segundos.");
                System.exit(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "test-timeout");
        watchdog.setDaemon(true);
        watchdog.start();
        // Asegurar que la carpeta 'test' y subcarpetas existen
        // Logger antepone logs/; la ruta logs/../test también debe funcionar en un clon limpio.
        new File("logs").mkdirs();
        new File("test/secuencial").mkdirs();
        new File("test/concurrente").mkdirs();

        System.out.println("==========================================");
        System.out.println("   INICIANDO BATERÍA DE PRUEBAS AERON");
        System.out.println("   (Resultados en carpeta /test)");
        System.out.println("==========================================\n");

        runTestCase(1, MODE_SEQ, 2, 1, 1, 1);
        runTestCase(2, MODE_SEQ, 20, 3, 5, 1);
        runTestCase(3, MODE_CON, 2, 1, 1, 1);
        runTestCase(4, MODE_CON, 2, 1, 1, 5);
        runTestCase(5, MODE_CON, 20, 3, 5, 1);
        runTestCase(6, MODE_CON, 20, 3, 5, 5);

        System.out.println("\n==========================================");
        System.out.println("   TODAS LAS PRUEBAS FINALIZADAS");
        System.out.println("==========================================");
        System.exit(0);
    }

    private static void runTestCase(int id, String mode, int planes, int runways, int gates, int operators) {
        System.out.println(String.format(">>> TEST #%d: [%s] Aviones=%d, Pistas=%d, Puertas=%d, Ops=%d", 
                id, mode, planes, runways, gates, operators));
        
        // 1. Configurar Logger
        Logger.close();
        
        String timeStamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        // TRUCO: Ruta relativa "..\\" para salir de 'logs' y entrar a 'test'
        String relativePath = "../test/" + mode.toLowerCase(); 
        
        String logName;
        if (MODE_CON.equals(mode)) {
            logName = String.format("%s/TEST%d_aeron-%s-%dAV-%dPIS-%dPUE-%dOPE-%s",
                    relativePath, id, mode.toLowerCase(), planes, runways, gates, operators, timeStamp);
        } else {
            logName = String.format("%s/TEST%d_aeron-%s-%dAV-%dPIS-%dPUE-%s",
                    relativePath, id, mode.toLowerCase(), planes, runways, gates, timeStamp);
        }

        // Logger.setup prepends "logs/", so "logs/../test/..." works.
        Logger.setup(logName);
        
        // Configuramos FlightPanel en la carpeta test también
        // Usamos el mismo nombre base que el log para vincularlos: ..._board.json
        String outputBase = Path.of("logs", logName).normalize().toString();
        FlightPanel.setup(outputBase + "_board.json");
        
        Logger.log("=== INICIO TEST #" + id + " ===");

        // 2. Inicializar entorno
        List<Runway> runwayList = new ArrayList<>();
        for (int i = 1; i <= runways; i++) runwayList.add(new Runway("PIS" + i));

        List<Gate> gateList = new ArrayList<>();
        for (int i = 1; i <= gates; i++) gateList.add(new Gate("PUE" + i));

        ControlTower tower = new ControlTower(runwayList, gateList, operators);

        // 3. Crear Aviones
        List<Airplane> airline = new ArrayList<>();
        for (int i = 1; i <= planes; i++) {
            airline.add(new Airplane(String.format("AIR-%03d", i), tower));
        }

        long start = System.currentTimeMillis();

        // 4. Ejecutar simulación
        if (MODE_SEQ.equals(mode)) {
            for (Airplane plane : airline) {
                // Reiniciamos estado para secuencial por si acaso
                plane.runSequential();
            }
        } else {
            // Modo Concurrente
            for (Airplane plane : airline) plane.start();
            
            for (Airplane plane : airline) {
                try {
                    plane.join();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError("Test interrumpido esperando a " + plane.getName(), e);
                }
            }
        }

        long duration = System.currentTimeMillis() - start;
        Logger.log("=== FIN TEST #" + id + " - Tiempo: " + duration + " ms ===");

        // 5. Generar CSV propio del test
        // Usamos logName como base para el CSV: ..._resumen.csv
        generateCSV(airline, outputBase + "_resumen.csv");
        verifyResults(airline, runwayList, gateList, outputBase);

        System.out.println("    -> Finalizado en " + duration + " ms.");
        Logger.close();
        
        try { Thread.sleep(1000); } catch (Exception e) {}
    }

    private static void generateCSV(List<Airplane> airplanes, String fullPath) {
        airplanes.sort(Comparator.comparingLong(Airplane::getTotalTime));

        try (FileWriter writer = new FileWriter(fullPath)) {
            writer.write("Avión;Tiempo total (ms);Ranking\n");
            int rank = 1;
            for (Airplane plane : airplanes) {
                writer.write(String.format("%s;%d;%dº\n",
                        plane.getFlightId(), plane.getTotalTime(), rank));
                rank++;
            }
            Logger.log("CSV Generado: " + fullPath);
        } catch (IOException e) {
            throw new AssertionError("Error generando CSV", e);
        }
    }

    private static void verifyResults(List<Airplane> airplanes, List<Runway> runways,
            List<Gate> gates, String outputBase) {
        for (Airplane plane : airplanes) {
            if (plane.getStatus() != FlightStatus.DEPARTED || plane.getTotalTime() <= 0) {
                throw new AssertionError("Vuelo incompleto: " + plane.getFlightId());
            }
        }
        if (runways.stream().anyMatch(r -> !r.isAvailable()) || gates.stream().anyMatch(Gate::isOccupied)) {
            throw new AssertionError("Quedan recursos ocupados al finalizar");
        }
        try {
            if (Files.readAllLines(Path.of(outputBase + "_resumen.csv")).size() != airplanes.size() + 1) {
                throw new AssertionError("Número incorrecto de filas en CSV");
            }
            String board = Files.readString(Path.of(outputBase + "_board.json"));
            for (Airplane plane : airplanes) {
                if (!board.contains("\"" + plane.getFlightId() + "\": \"DEPARTED\"")) {
                    throw new AssertionError("Panel incompleto: " + plane.getFlightId());
                }
            }
            long entries = board.lines().filter(line -> line.contains(":" )).count();
            if (entries != airplanes.size()) {
                throw new AssertionError("El panel contiene vuelos de otra simulación");
            }
            if (Files.size(Path.of(outputBase + ".log")) == 0) {
                throw new AssertionError("Log vacío");
            }
        } catch (IOException e) {
            throw new AssertionError("No se pudieron verificar los resultados", e);
        }
        System.out.println("PASS: vuelos completados, recursos libres y resultados verificados.");
    }
}
