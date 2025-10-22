package creditpay;

import javax.swing.*;
import java.awt.*;

/**
 * Minimal Swing application example.
 */
public class SwingApp {
    public static void main(String[] args) {
        // Schedule UI creation on the EDT
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Sample Swing App");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new BorderLayout());

            JLabel label = new JLabel("Hello from Swing", SwingConstants.CENTER);
            label.setFont(label.getFont().deriveFont(18f));
            frame.add(label, BorderLayout.CENTER);

            JButton btn = new JButton("Close");
            btn.addActionListener(e -> frame.dispose());
            frame.add(btn, BorderLayout.SOUTH);

            frame.setSize(300, 200);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
