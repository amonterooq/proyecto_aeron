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
import java.util.Scanner;

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

    public static void main(String[] args) {
        // 1. CONFIGURACIÓN MEDIANTE VENTANA (GUI)
        // Intentamos usar el estilo visual del sistema operativo
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        // MEJORA DE ACCESIBILIDAD: Fuentes más grandes y legibles (16px)
        Font mainFont = new Font("Segoe UI", Font.PLAIN, 18);
        Font boldFont = new Font("Segoe UI", Font.BOLD, 18);
        
        // Configurar fuentes globales para componentes internos de JOptionPane
        UIManager.put("OptionPane.messageFont", mainFont);
        UIManager.put("OptionPane.buttonFont", mainFont);
        UIManager.put("Label.font", mainFont);

        // Componentes del formulario
        JComboBox<String> comboModo = new JComboBox<>(new String[]{"-- Seleccione Modo --", "SECUENCIAL", "CONCURRENTE"});
        comboModo.setFont(mainFont);
        
        // Modelos de datos
        JSpinner spinAviones = new JSpinner(new SpinnerNumberModel(5, 1, 100, 1));
        JSpinner spinPistas = new JSpinner(new SpinnerNumberModel(3, 1, 10, 1));
        JSpinner spinPuertas = new JSpinner(new SpinnerNumberModel(5, 1, 20, 1));
        JSpinner spinOperarios = new JSpinner(new SpinnerNumberModel(5, 1, 20, 1));

        // Aplicamos la fuente y configuración de validación a los spinners
        configureSpinner(spinAviones, mainFont);
        configureSpinner(spinPistas, mainFont);
        configureSpinner(spinPuertas, mainFont);
        configureSpinner(spinOperarios, mainFont);

        // Lógica dinámica
        spinOperarios.setEnabled(false); 
        comboModo.addActionListener(e -> {
            String selected = (String) comboModo.getSelectedItem();
            boolean isConcurrent = "CONCURRENTE".equals(selected);
            spinOperarios.setEnabled(isConcurrent);
        });

        // Panel con diseño espacioso (Accesibilidad: Layout limpio)
        JPanel panel = new JPanel(new GridLayout(0, 2, 20, 20)); // Más espacio entre elementos
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Helper local para etiquetas legibles
        JLabel lblModo = new JLabel("Modo de Ejecución:"); lblModo.setFont(boldFont);
        panel.add(lblModo);
        panel.add(comboModo);
        
        panel.add(new JLabel("Número de Aviones (1-100):")); panel.add(spinAviones);
        panel.add(new JLabel("Número de Pistas (1-10):"));  panel.add(spinPistas);
        panel.add(new JLabel("Número de Puertas (1-20):")); panel.add(spinPuertas);
        panel.add(new JLabel("Nº Operarios (Solo Concurrente):")); panel.add(spinOperarios);

        // Bucle de validación
        while (true) {
            int result = JOptionPane.showConfirmDialog(null, panel, "Configuración AERON", 
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

            if (result != JOptionPane.OK_OPTION) {
                System.out.println("Configuración cancelada por el usuario.");
                System.exit(0);
            }

            // Validación explícita: El usuario DEBE seleccionar un modo
            if (comboModo.getSelectedIndex() == 0) {
                JOptionPane.showMessageDialog(null, 
                    "Por favor, seleccione un modo de ejecución válido (Secuencial o Concurrente).", 
                    "Aviso", JOptionPane.WARNING_MESSAGE);
                continue; // Re-mostrar diálogo
            }
            
            // Validar spinners para detectar basura e impedir corrección automática sin aviso
            try {
                validateSpinner(spinAviones, "Aviones", 1, 100);
                validateSpinner(spinPistas, "Pistas", 1, 10);
                validateSpinner(spinPuertas, "Puertas", 1, 20);
                if (spinOperarios.isEnabled()) {
                    validateSpinner(spinOperarios, "Operarios", 1, 20);
                }
            } catch (Exception e) {
                 JOptionPane.showMessageDialog(null, e.getMessage(), "Error en entrada", JOptionPane.ERROR_MESSAGE);
                 continue; // Re-mostrar diálogo sin cerrar
            }
            
            // Si todo es correcto, salimos del bucle
            break;
        }

        // Lectura de valores seleccionados
        String modo = (String) comboModo.getSelectedItem();
        int numAviones = (Integer) spinAviones.getValue();
        int numPistas = (Integer) spinPistas.getValue();
        int numPuertas = (Integer) spinPuertas.getValue();
        int numOperarios = 1;
        
        if ("CONCURRENTE".equals(modo)) {
            numOperarios = (Integer) spinOperarios.getValue();
        }

        System.out.println("\n--------------------------------------------");
        System.out.println("Iniciando simulación " + modo + "...");
        System.out.println("--------------------------------------------");

        // 2. CONFIGURACIÓN DEL NOMBRE DEL LOG (Requisito estricto)
        String timeStamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

        // Formato: aeron-mode-N1AV-N2PIS-N3PUE[-N4OPE-]-TIMESTAMP
        String logName;
        if (modo.equals("CONCURRENTE")) {
            logName = String.format("concurrente/aeron-%s-%d-%d-%d-%d-%s",
                    modo.toLowerCase(), numAviones, numPistas, numPuertas, numOperarios, timeStamp);
        } else {
            // En secuencial no incluimos el número de operarios en el nombre
            logName = String.format("secuencial/aeron-%s-%d-%d-%d-%s",
                    modo.toLowerCase(), numAviones, numPistas, numPuertas, timeStamp);
        }

        Logger.setup(logName);
        // Configuramos también el Panel de Vuelos para que use un JSON distinto
        FlightPanel.setup(modo);

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
            // Modo Concurrente: Todos a la vez.
            // Los aviones son hilos (extends Thread)
            System.out.println("Lanzando " + numAviones + " aviones en paralelo...");
            for (Airplane plane : airplanes) {
                plane.start();
            }

            // JOIN: Esperar a que terminen con feedback visual de progreso
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
                    Logger.log("Driver interrumpido mientras esperaba al avión " + plane.getName());
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

        // 7. MOSTRAR RESUMEN EN CONSOLA (Mejora de UX)
        System.out.println("\n=== RANKING DE EFICIENCIA ===");
        System.out.println(String.format("%-10s %-15s %s", "AVIÓN", "TIEMPO (ms)", "POS"));
        System.out.println("------------------------------------");
        // Mostramos solo los 5 primeros para no saturar
        // OPTIMIZACIÓN: Iteración simple sobre lista ya ordenada, O(1) para top 5
        for (int i = 0; i < Math.min(5, airplanes.size()); i++) {
            Airplane p = airplanes.get(i);
            System.out.println(String.format("%-10s %-15d %dº",
                    p.getFlightId(), p.getTotalTime(), i + 1));
        }

        // scanner.close(); // Eliminado porque ya no usamos Scanner

        System.out.println("\nSimulación finalizada con éxito. Cerrando aplicación...");
        JOptionPane.showMessageDialog(null, "Simulación finalizada.\nConsulta el log y el CSV para más detalles.", "AERON - Fin", JOptionPane.INFORMATION_MESSAGE);
        System.exit(0);
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