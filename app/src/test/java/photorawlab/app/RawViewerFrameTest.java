package photorawlab.app;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Component;
import java.awt.Container;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import photorawlab.domain.RgbImage;

class RawViewerFrameTest {
    private RawViewerFrame frame;
    private final CountDownLatch release = new CountDownLatch(1);

    @AfterEach
    void disposeWindow() throws Exception {
        release.countDown();
        if (frame != null) SwingUtilities.invokeAndWait(frame::dispose);
    }

    @Test
    void decodesOffEdtAndDisplaysImageWhileEdtRemainsResponsive() throws Exception {
        CountDownLatch entered = new CountDownLatch(1);
        AtomicBoolean decodedOnEdt = new AtomicBoolean(true);
        SwingUtilities.invokeAndWait(() -> {
            frame = new RawViewerFrame(path -> {
                decodedOnEdt.set(SwingUtilities.isEventDispatchThread());
                entered.countDown();
                awaitRelease();
                return new RgbImage(2, 1, new int[] {0xff0000, 0x00ff00});
            });
            frame.setVisible(true);
            frame.openRaw(Path.of("renamed.raw"));
        });
        assertTrue(entered.await(5, TimeUnit.SECONDS));
        SwingUtilities.invokeAndWait(() -> {
            assertFalse(button().isEnabled());
            assertTrue(status().getText().contains("Cargando"));
        });
        assertFalse(decodedOnEdt.get());
        CountDownLatch completed = completion();
        release.countDown();
        assertTrue(completed.await(5, TimeUnit.SECONDS));
        SwingUtilities.invokeAndWait(() -> {
            assertTrue(status().getText().contains("renamed.raw"));
            RawImagePanel panel = find(frame, RawImagePanel.class);
            panel.setSize(200, 100);
            BufferedImage painted = new BufferedImage(200, 100, BufferedImage.TYPE_INT_RGB);
            var graphics = painted.createGraphics();
            try { panel.paint(graphics); } finally { graphics.dispose(); }
            assertEquals(0xff0000, painted.getRGB(50, 50) & 0xffffff);
            assertEquals(0x00ff00, painted.getRGB(150, 50) & 0xffffff);
        });
    }

    @Test
    void showsRecoverableErrorAndAllowsNextOpen() throws Exception {
        AtomicBoolean fail = new AtomicBoolean(true);
        SwingUtilities.invokeAndWait(() -> {
            frame = new RawViewerFrame(path -> {
                if (fail.getAndSet(false)) throw new IOException("Formato no compatible");
                return new RgbImage(1, 1, new int[] {0xffffff});
            });
            frame.setVisible(true);
        });
        CountDownLatch failed = completion();
        SwingUtilities.invokeAndWait(() -> frame.openRaw(Path.of("bad.raw")));
        assertTrue(failed.await(5, TimeUnit.SECONDS));
        SwingUtilities.invokeAndWait(() -> assertTrue(status().getText().contains("Formato no compatible")));
        CountDownLatch succeeded = completion();
        SwingUtilities.invokeAndWait(() -> frame.openRaw(Path.of("good.raw")));
        assertTrue(succeeded.await(5, TimeUnit.SECONDS));
        SwingUtilities.invokeAndWait(() -> assertTrue(status().getText().contains("good.raw")));
    }

    @Test
    void closingDuringDecodeDoesNotPublishOrInterruptDecoder() throws Exception {
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch finished = new CountDownLatch(1);
        AtomicBoolean interrupted = new AtomicBoolean();
        SwingUtilities.invokeAndWait(() -> {
            frame = new RawViewerFrame(path -> {
                entered.countDown();
                try { awaitRelease(); }
                catch (IOException error) { interrupted.set(true); throw error; }
                finished.countDown();
                return new RgbImage(1, 1, new int[] {0xffffff});
            });
            frame.setVisible(true);
            frame.openRaw(Path.of("pending.raw"));
        });
        assertTrue(entered.await(5, TimeUnit.SECONDS));
        AtomicReference<String> closedStatus = new AtomicReference<>();
        AtomicBoolean publishedAfterClose = new AtomicBoolean();
        CountDownLatch completion = new CountDownLatch(1);
        SwingUtilities.invokeAndWait(() -> {
            frame.dispose();
            closedStatus.set(status().getText());
            status().addPropertyChangeListener("text", event -> publishedAfterClose.set(true));
            frame.addPropertyChangeListener("loading", event -> completion.countDown());
        });
        release.countDown();
        assertTrue(finished.await(5, TimeUnit.SECONDS));
        assertTrue(completion.await(5, TimeUnit.SECONDS));
        SwingUtilities.invokeAndWait(() -> {
            assertFalse(frame.isDisplayable());
            assertEquals(closedStatus.get(), status().getText());
        });
        assertFalse(interrupted.get());
        assertFalse(publishedAfterClose.get());
    }

    private void awaitRelease() throws IOException {
        try {
            if (!release.await(5, TimeUnit.SECONDS)) throw new IOException("Decoder release timed out");
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new IOException(error);
        }
    }

    private CountDownLatch completion() throws Exception {
        CountDownLatch completed = new CountDownLatch(1);
        SwingUtilities.invokeAndWait(() -> button().addPropertyChangeListener("enabled", event -> {
            if (Boolean.TRUE.equals(event.getNewValue())) completed.countDown();
        }));
        return completed;
    }

    private JButton button() { return find(frame, JButton.class); }
    private JLabel status() { return find(frame, JLabel.class); }

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
