package aeronpcd.gui;

import javax.swing.*;
import java.awt.*;

/**
 * Ventana principal de la simulación basada en Swing.
 * Muestra dos paneles:
 * 1. Log de eventos en tiempo real (Izquierda).
 * 2. Visualización gráfica del estado de recursos mediante HTML (Derecha).
 * 
 * Es Thread-Safe utilizando SwingUtilities.invokeLater para actualizaciones
 * desde otros hilos.
 */
public class AirportWindow extends JFrame {
    // Componentes de la interfaz
    private JTextArea logArea;
    private JTextPane stateArea;

    // Constantes de diseño
    private static final Color BG_LOG = new Color(30, 30, 30);
    private static final Color BG_STATE = new Color(10, 10, 40);
    private static final Color FG_LOG = Color.GREEN;
    private static final Font FONT_LOG = new Font("Monospaced", Font.PLAIN, 12);

    public AirportWindow() {
        super("Aeropuerto AERON - Monitor de Estado y Recursos");
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setSize(1200, 700);

        // Maximizar ventana para ver bien todos los recursos en simulaciones grandes
        this.setExtendedState(JFrame.MAXIMIZED_BOTH);
        this.setLayout(new GridLayout(1, 2));

        initComponents();

        this.setVisible(true);

        // PRÁCTICA 7: Forzar que la ventana tome el foco y aparezca al frente
        this.toFront();
        this.requestFocus();
    }

    private void initComponents() {
        // --- Panel Izquierdo: LOGS ---
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(FONT_LOG);
        logArea.setBackground(BG_LOG);
        logArea.setForeground(FG_LOG);

        JScrollPane scrollLog = new JScrollPane(logArea);
        scrollLog.setBorder(BorderFactory.createTitledBorder("Registro de Eventos"));
        this.add(scrollLog);

        // --- Panel Derecho: ESTADO (HTML NATIVO) ---
        stateArea = new JTextPane();
        stateArea.setEditable(false);
        stateArea.setContentType("text/html"); // Importante: interpreta HTML generado por AirportState
        stateArea.setBackground(BG_STATE);

        JScrollPane scrollState = new JScrollPane(stateArea);
        scrollState.setBorder(BorderFactory.createTitledBorder("Estado del Aeropuerto"));
        this.add(scrollState);
    }

    /**
     * Añade una línea de texto al panel de logs izquierdo.
     * Thread-Safe: Se puede invocar desde cualquier hilo.
     * 
     * @param text El mensaje a mostrar.
     */
    public void appendLog(String text) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(text + "\n");
            // Auto-scroll hacia abajo
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    /**
     * Actualiza el panel derecho con nuevo contenido HTML.
     * Thread-Safe: Se puede invocar desde cualquier hilo.
     * 
     * @param htmlContent Código HTML generado por AirportState.
     */
    public void updateState(String htmlContent) {
        SwingUtilities.invokeLater(() -> {
            // Ya recibimos HTML puro, lo pintamos directamente
            stateArea.setText(htmlContent);
            // PRÁCTICA 7: Eliminamos el reset de scroll (caretPosition) para permitir
            // que el usuario baje y vea la cola completa sin saltos involuntarios.
            // stateArea.setCaretPosition(0);
        });
    }
}