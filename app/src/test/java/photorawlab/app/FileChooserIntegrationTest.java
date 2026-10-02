package photorawlab.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.awt.Window;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

class FileChooserIntegrationTest {
    @TempDir
    Path directory;

    @Test
    @Timeout(20)
    void opensSelectedRawThroughTheVisibleFileChooser() throws Exception {
        Path raw = directory.resolve("fotografía.raw");
        Files.copy(Path.of(getClass().getResource("/raw/kodak-dc50.kdc").toURI()), raw);
        CompletableFuture<JFileChooser> chooserShown = new CompletableFuture<>();
        CountDownLatch loaded = new CountDownLatch(1);
        RawViewerFrame[] frame = new RawViewerFrame[1];
        Timer observer = new Timer(25, event -> {
            for (Window window : Window.getWindows()) {
                JFileChooser chooser = find(window, JFileChooser.class);
                if (chooser != null && chooser.isShowing()) chooserShown.complete(chooser);
            }
        });
        try {
            SwingUtilities.invokeAndWait(() -> {
                frame[0] = new RawViewerFrame(new LibRawDecoder());
                frame[0].addPropertyChangeListener("loading", event -> loaded.countDown());
                frame[0].setVisible(true);
                observer.start();
            });
            SwingUtilities.invokeLater(() -> find(frame[0], JButton.class).doClick());
            JFileChooser chooser = chooserShown.get(5, TimeUnit.SECONDS);
            SwingUtilities.invokeAndWait(() -> {
                chooser.setSelectedFile(raw.toFile());
                chooser.approveSelection();
            });
            assertTrue(loaded.await(5, TimeUnit.SECONDS));
            SwingUtilities.invokeAndWait(() -> {
                assertEquals(raw.getFileName().toString(), find(frame[0], JLabel.class).getText());
                assertTrue(find(frame[0], JButton.class).isEnabled());
            });
        } finally {
            SwingUtilities.invokeAndWait(() -> {
                observer.stop();
                for (Window window : Window.getWindows()) {
                    if (find(window, JFileChooser.class) != null) window.dispose();
                }
                if (frame[0] != null) frame[0].dispose();
            });
        }
    }

    private static <T> T find(Container container, Class<T> type) {
        for (Component component : container.getComponents()) {
            if (type.isInstance(component)) return type.cast(component);
            if (component instanceof Container child) {
                T found = find(child, type);
                if (found != null) return found;
            }
        }
        return null;
    }
}
