package aeronpcd.gui;

import javax.swing.*;
import java.awt.*;

public class AirportWindow extends JFrame {
    private JTextArea logArea;
    private JTextPane stateArea;

    public AirportWindow() {
        super("Aeropuerto AERON - Monitor de Estado y Recursos");
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setSize(1200, 700);
        // PRÁCTICA 7: Maximizar ventana para ver bien todos los recursos en simulaciones grandes
        this.setExtendedState(JFrame.MAXIMIZED_BOTH);
        this.setLayout(new GridLayout(1, 2));

        // --- Panel Izquierdo: LOGS ---
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        logArea.setBackground(new Color(30, 30, 30));
        logArea.setForeground(Color.GREEN);

        JScrollPane scrollLog = new JScrollPane(logArea);
        scrollLog.setBorder(BorderFactory.createTitledBorder("Registro de Eventos"));
        this.add(scrollLog);

        // --- Panel Derecho: ESTADO (HTML NATIVO) ---
        stateArea = new JTextPane();
        stateArea.setEditable(false);
        stateArea.setContentType("text/html"); // Importante: interpreta HTML
        stateArea.setBackground(new Color(10, 10, 40));

        JScrollPane scrollState = new JScrollPane(stateArea);
        scrollState.setBorder(BorderFactory.createTitledBorder("Estado del Aeropuerto"));
        this.add(scrollState);

        this.setVisible(true);
    }

    public void appendLog(String text) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(text + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    public void updateState(String htmlContent) {
        SwingUtilities.invokeLater(() -> {
            // Ya recibimos HTML puro, lo pintamos directamente
            stateArea.setText(htmlContent);
            // PRÁCTICA 7: Eliminamos el reset de scroll (caretPosition) para permitir 
            // que el usuario baje y vea la cola completa sin saltos.
            // stateArea.setCaretPosition(0); 
        });
    }
}