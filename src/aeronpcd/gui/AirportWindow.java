package aeronpcd.gui;

import javax.swing.*;
import java.awt.*;

public class AirportWindow extends JFrame {
    private JTextArea logArea;

    public AirportWindow() {
        super("Aeropuerto AERON - Monitor de Estado");
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setSize(800, 600);
        this.setLayout(new BorderLayout());

        // Área de texto para mostrar eventos y estado
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        logArea.setBackground(new Color(30, 30, 30));
        logArea.setForeground(Color.GREEN);

        JScrollPane scrollPane = new JScrollPane(logArea);
        this.add(scrollPane, BorderLayout.CENTER);

        this.setVisible(true);
    }

    // Método para añadir texto a la ventana desde cualquier parte del programa
    public void appendLog(String text) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(text + "\n");
            // Auto-scroll hacia abajo
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }
}