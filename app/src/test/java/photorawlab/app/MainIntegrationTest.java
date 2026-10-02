package photorawlab.app;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.awt.Robot;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

class MainIntegrationTest {
    @Test
    @Timeout(30)
    void opensRealRawFromCommandLineAndRendersItInAVisibleWindow() throws Exception {
        Path raw = Path.of(MainIntegrationTest.class.getResource("/raw/kodak-dc50.kdc").toURI());
        CompletableFuture<JFrame> loaded = new CompletableFuture<>();
        Timer observer = new Timer(25, event -> {
            for (Window window : Window.getWindows()) {
                if (window instanceof JFrame frame && frame.isShowing()
                        && containsLabel(frame, raw.getFileName().toString())) {
                    loaded.complete(frame);
                }
            }
        });
        try {
            SwingUtilities.invokeAndWait(observer::start);
            Main.main(new String[] {raw.toString()});
            JFrame frame = loaded.get(12, TimeUnit.SECONDS);
            Robot robot = new Robot();
            robot.waitForIdle();
            BufferedImage screenshot = robot.createScreenCapture(frame.getBounds());
            Set<Integer> imageColors = new HashSet<>();
            for (int y = screenshot.getHeight() / 4; y < screenshot.getHeight() * 3 / 4; y += 10) {
                for (int x = screenshot.getWidth() / 4; x < screenshot.getWidth() * 3 / 4; x += 10) {
                    imageColors.add(screenshot.getRGB(x, y));
                }
            }
            Files.createDirectories(Path.of("target", "verification"));
            ImageIO.write(screenshot, "png", Path.of("target", "verification", "raw-window.png").toFile());
            assertTrue(imageColors.size() > 32, "The center must show photographic pixels, observed " + imageColors.size() + " colors");
        } finally {
            SwingUtilities.invokeAndWait(() -> {
                observer.stop();
                for (Window window : Window.getWindows()) {
                    if (window instanceof JFrame frame && frame.getTitle().equals("Photo RAW Lab")) {
                        frame.dispose();
                    }
                }
            });
        }
    }

    private static boolean containsLabel(Container container, String text) {
        for (Component component : container.getComponents()) {
            if (component instanceof JLabel label && text.equals(label.getText())) return true;
            if (component instanceof Container child && containsLabel(child, text)) return true;
        }
        return false;
    }
}
