package {{package}}.app;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

public final class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame window = new JFrame("{{name}}");
            window.add(new JLabel("{{name}}", JLabel.CENTER));
            window.setSize(640, 480);
            window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            window.setLocationRelativeTo(null);
            window.setVisible(true);
        });
    }
}
