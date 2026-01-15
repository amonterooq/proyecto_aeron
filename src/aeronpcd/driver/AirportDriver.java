package aeronpcd.driver;

import aeronpcd.gui.AirportWindow;
import aeronpcd.model.Airplane;
import aeronpcd.resources.Gate;
import aeronpcd.resources.Runway;
import aeronpcd.tower.ControlTower;
import aeronpcd.util.FlightPanel;
import aeronpcd.util.Logger;

import javax.swing.*;
import java.awt.*;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Clase principal (Driver) del simulador de aeropuerto AERON.
 * 
 * Responsabilidades:
 * 1. Configuración de la simulación (Modo, número de recursos).
 * 2. Inicialización de componentes (Ventana, Torre, Logs).
 * 3. Orquestación de la ejecución (Secuencial o Concurrente).
 * 4. Generación de informes finales (CSV y Ranking en consola).
 */
public class AirportDriver {

    // Constantes de configuración por defecto y límites
    private static final int MIN_PLANES = 1, MAX_PLANES = 100;
    private static final int MIN_RUNWAYS = 1, MAX_RUNWAYS = 10;
    private static final int MIN_GATES = 1, MAX_GATES = 20;
    private static final int MIN_OPERATORS = 1, MAX_OPERATORS = 20;

    private static class SimulationConfig {
        String mode;
        int planes;
        int runways;
        int gates;
        int operators;
    }

    public static void main(String[] args) {
        // 1. Configuración visual y obtención de parámetros
        setupLookAndFeel();
        SimulationConfig config = showConfigurationDialog();

        if (config == null) {
            System.out.println("Configuración cancelada por el usuario.");
            System.exit(0);
        }

        System.out.println("\n--------------------------------------------");
        System.out.println("Iniciando simulación " + config.mode + "...");
        System.out.println("--------------------------------------------");

        // 2. Configuración del Logging
        initializeLogging(config);

        // 3. Inicialización del entorno (Ventana, Torre, Recursos)
        AirportWindow window = new AirportWindow();
        ControlTower tower = initializeEnvironment(config, window);

        // 4. Creación de Aviones
        List<Airplane> airplanes = createAirplanes(config.planes, tower);

        // 5. Ejecución
        runSimulation(config, airplanes);

        // 6. Cierre y Reporting
        finalizeSimulation(config, airplanes);
    }

    // --- MÉTODOS DE SOPORTE ESTRUCTURAL ---

    private static void setupLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            // Accesibilidad: Fuentes globales más grandes (18px)
            Font mainFont = new Font("Segoe UI", Font.PLAIN, 18);
            UIManager.put("OptionPane.messageFont", mainFont);
            UIManager.put("OptionPane.buttonFont", mainFont);
            UIManager.put("Label.font", mainFont);
            UIManager.put("ComboBox.font", mainFont);
            UIManager.put("TextField.font", mainFont);
        } catch (Exception ignored) {}
    }

    private static SimulationConfig showConfigurationDialog() {
        Font mainFont = new Font("Segoe UI", Font.PLAIN, 18);
        Font boldFont = new Font("Segoe UI", Font.BOLD, 18);

        // Componentes del formulario
        JComboBox<String> comboModo = new JComboBox<>(new String[]{"-- Seleccione Modo --", "SECUENCIAL", "CONCURRENTE"});
        comboModo.setFont(mainFont);
        
        // Modelos de datos
        JSpinner spinAviones = new JSpinner(new SpinnerNumberModel(5, MIN_PLANES, MAX_PLANES, 1));
        JSpinner spinPistas = new JSpinner(new SpinnerNumberModel(3, MIN_RUNWAYS, MAX_RUNWAYS, 1));
        JSpinner spinPuertas = new JSpinner(new SpinnerNumberModel(5, MIN_GATES, MAX_GATES, 1));
        JSpinner spinOperarios = new JSpinner(new SpinnerNumberModel(5, MIN_OPERATORS, MAX_OPERATORS, 1));

        configureSpinner(spinAviones, mainFont);
        configureSpinner(spinPistas, mainFont);
        configureSpinner(spinPuertas, mainFont);
        configureSpinner(spinOperarios, mainFont);

        // Lógica visual dinámica
        spinOperarios.setEnabled(false); 
        comboModo.addActionListener(e -> {
            boolean isConcurrent = "CONCURRENTE".equals(comboModo.getSelectedItem());
            spinOperarios.setEnabled(isConcurrent);
        });

        // Panel de diseño
        JPanel panel = new JPanel(new GridLayout(0, 2, 20, 20)); // Layout espacioso
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel lblModo = new JLabel("Modo de Ejecución:"); 
        lblModo.setFont(boldFont);
        panel.add(lblModo);
        panel.add(comboModo);
        
        panel.add(new JLabel("Número de Aviones (" + MIN_PLANES + "-" + MAX_PLANES + "):")); panel.add(spinAviones);
        panel.add(new JLabel("Número de Pistas (" + MIN_RUNWAYS + "-" + MAX_RUNWAYS + "):"));  panel.add(spinPistas);
        panel.add(new JLabel("Número de Puertas (" + MIN_GATES + "-" + MAX_GATES + "):")); panel.add(spinPuertas);
        panel.add(new JLabel("Nº Operarios (Solo Concurrente):")); panel.add(spinOperarios);

        // Bucle de validación
        while (true) {
            int result = JOptionPane.showConfirmDialog(null, panel, "Configuración AERON", 
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

            if (result != JOptionPane.OK_OPTION) return null;

            if (comboModo.getSelectedIndex() == 0) {
                showError("Por favor, seleccione un modo de ejecución válido.");
                continue;
            }
            
            try {
                validateSpinner(spinAviones, "Aviones", MIN_PLANES, MAX_PLANES);
                validateSpinner(spinPistas, "Pistas", MIN_RUNWAYS, MAX_RUNWAYS);
                validateSpinner(spinPuertas, "Puertas", MIN_GATES, MAX_GATES);
                if (spinOperarios.isEnabled()) {
                    validateSpinner(spinOperarios, "Operarios", MIN_OPERATORS, MAX_OPERATORS);
                }
            } catch (Exception e) {
                 showError(e.getMessage());
                 continue;
            }
            
            // Construir resultado
            SimulationConfig config = new SimulationConfig();
            config.mode = (String) comboModo.getSelectedItem();
            config.planes = (Integer) spinAviones.getValue();
            config.runways = (Integer) spinPistas.getValue();
            config.gates = (Integer) spinPuertas.getValue();
            config.operators = "CONCURRENTE".equals(config.mode) ? (Integer) spinOperarios.getValue() : 1;
            
            return config;
        }
    }

    private static void initializeLogging(SimulationConfig config) {
        String timeStamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String logName;
        
        if ("CONCURRENTE".equals(config.mode)) {
            logName = String.format("concurrente/aeron-%s-%d-%d-%d-%d-%s",
                    config.mode.toLowerCase(), config.planes, config.runways, config.gates, config.operators, timeStamp);
        } else {
            logName = String.format("secuencial/aeron-%s-%d-%d-%d-%s",
                    config.mode.toLowerCase(), config.planes, config.runways, config.gates, timeStamp);
        }

        Logger.setup(logName);
        FlightPanel.setup(config.mode);

        Logger.log("=== INICIO SIMULACIÓN (" + config.mode + ") ===");
        Logger.log(String.format("Configuración: %d Aviones, %d Pistas, %d Puertas, %d Operarios.", 
                   config.planes, config.runways, config.gates, config.operators));
    }

    private static ControlTower initializeEnvironment(SimulationConfig config, AirportWindow window) {
        List<Runway> runways = new ArrayList<>();
        for (int i = 1; i <= config.runways; i++) runways.add(new Runway("PIS" + i));

        List<Gate> gates = new ArrayList<>();
        for (int i = 1; i <= config.gates; i++) gates.add(new Gate("PUE" + i));

        ControlTower tower = new ControlTower(runways, gates);
        tower.setWindow(window);
        return tower;
    }

    private static List<Airplane> createAirplanes(int count, ControlTower tower) {
        List<Airplane> airplanes = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            airplanes.add(new Airplane(String.format("IBE-%03d", i), tower));
        }
        return airplanes;
    }

    private static void runSimulation(SimulationConfig config, List<Airplane> airplanes) {
        long start = System.currentTimeMillis();

        if ("SECUENCIAL".equals(config.mode)) {
            for (Airplane plane : airplanes) {
                plane.runSequential();
            }
        } else {
            System.out.println("Lanzando " + config.planes + " aviones en paralelo...");
            for (Airplane plane : airplanes) plane.start();

            System.out.println("Esperando finalización de vuelos...");
            int done = 0;
            for (Airplane plane : airplanes) {
                try {
                    plane.join();
                    done++;
                    if (done % 5 == 0 || done == config.planes) {
                        System.out.println("Progreso: " + done + "/" + config.planes + " activados.");
                    }
                } catch (InterruptedException e) {
                    Logger.log("Driver interrumpido: " + e.getMessage());
                }
            }
            System.out.println("Todos los aviones han finalizado.");
        }

        long duration = System.currentTimeMillis() - start;
        Logger.log("=== FIN DE LA SIMULACIÓN ===");
        Logger.log("TIEMPO TOTAL: " + duration + " ms");
        Logger.close();
    }

    private static void finalizeSimulation(SimulationConfig config, List<Airplane> airplanes) {
        generateCSV(airplanes, config.mode);
        printRanking(airplanes);
        JOptionPane.showMessageDialog(null, "Simulación finalizada.\nConsulta el log y el CSV para más detalles.", 
            "AERON - Fin", JOptionPane.INFORMATION_MESSAGE);
    }

    private static void printRanking(List<Airplane> airplanes) {
        System.out.println("\n=== RANKING DE EFICIENCIA ===");
        System.out.println(String.format("%-10s %-15s %s", "AVIÓN", "TIEMPO (ms)", "POS"));
        System.out.println("------------------------------------");
        // Aseguramos ordenación por si acaso se llama fuera de contexto
        airplanes.sort(Comparator.comparingLong(Airplane::getTotalTime));
        
        for (int i = 0; i < Math.min(5, airplanes.size()); i++) {
            Airplane p = airplanes.get(i);
            System.out.println(String.format("%-10s %-15d %dº", p.getFlightId(), p.getTotalTime(), i + 1));
        }
    }

    // --- AYUDANTES GUI ---

    private static void showError(String message) {
        JOptionPane.showMessageDialog(null, message, "Error en entrada", JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Genera un archivo CSV con el resumen de la simulación.
     * Ordena los aviones por tiempo total de menor a mayor y genera un ranking.
     * 
     * @param airplanes Lista de aviones que han completado (o intentado) la
     *                  simulación.
     * @param modo      Modo de ejecución para generar el nombre del archivo.
     */
    private static void generateCSV(List<Airplane> airplanes, String modo) {
        // Se guarda dentro de la carpeta logs/secuencial o logs/concurrente
        String fileName = "logs/" + modo.toLowerCase() + "/aeropuerto_resumen.csv";

        // Ordenamos la lista de aviones por su tiempo total de operación (ascendente)
        airplanes.sort(Comparator.comparingLong(Airplane::getTotalTime));

        try (FileWriter writer = new FileWriter(fileName)) {
            // Cabecera del CSV
            writer.write("Avión;Tiempo total (ms);Ranking\n");

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

    // --- MÉTODOS DE VALIDACIÓN GUI ---

    private static void configureSpinner(JSpinner spinner, Font font) {
        spinner.setFont(font);
        JComponent editor = spinner.getEditor();
        if (editor instanceof JSpinner.DefaultEditor) {
            JFormattedTextField tf = ((JSpinner.DefaultEditor) editor).getTextField();
            tf.setFont(font); // Aplicamos fuente grande también al campo de texto
            
            // IMPORTANTE: Evita que el spinner revierta automáticamente valores inválidos al perder foco.
            // Esto permite que podamos "leer" el valor no válido y notificar al usuario.
            tf.setFocusLostBehavior(JFormattedTextField.PERSIST);
        }
    }

    private static void validateSpinner(JSpinner spinner, String name, int min, int max) throws Exception {
        JComponent editor = spinner.getEditor();
        if (editor instanceof JSpinner.DefaultEditor) {
            JFormattedTextField tf = ((JSpinner.DefaultEditor) editor).getTextField();
            String text = tf.getText();
            try {
                int value = Integer.parseInt(text.trim());
                if (value < min || value > max) {
                     throw new Exception("El campo '" + name + "' debe estar entre " + min + " y " + max + ".");
                }
                // Si es válido, actualizamos el modelo explícitamente por si estaba en modo PERSIST sin commit
                spinner.setValue(value);
            } catch (NumberFormatException e) {
                throw new Exception("El campo '" + name + "' contiene un valor inválido ('" + text + "').\nPor favor, introduce solo números enteros.");
            }
        }
    }
}