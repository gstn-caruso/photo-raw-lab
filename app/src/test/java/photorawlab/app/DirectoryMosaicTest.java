package photorawlab.app;

import static org.junit.jupiter.api.Assertions.*;
import java.awt.Component;
import java.awt.Container;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.JButton;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import photorawlab.domain.RgbImage;

class DirectoryMosaicTest {
    @TempDir Path directory;

    @Test void showsPreviewsOffEdtContinuesAfterBadFileAndOpensPhotoThenReturns() throws Exception {
        Files.createFile(directory.resolve("a-bad.raw"));
        Files.createFile(directory.resolve("b-good.raw"));
        RawViewerFrame[] frame = new RawViewerFrame[1];
        AtomicBoolean onEdt = new AtomicBoolean();
        CountDownLatch loaded = new CountDownLatch(1);
        try {
            SwingUtilities.invokeAndWait(() -> {
                frame[0] = new RawViewerFrame(path -> {
                    onEdt.set(onEdt.get() || SwingUtilities.isEventDispatchThread());
                    if (path.getFileName().toString().contains("bad")) throw new IOException("Bad RAW");
                    return new RgbImage(2, 1, new int[] {0xff0000, 0x00ff00});
                });
                frame[0].addPropertyChangeListener("loading", event -> loaded.countDown());
                frame[0].openDirectory(directory);
            });
            assertTrue(loaded.await(5, TimeUnit.SECONDS));
            assertFalse(onEdt.get());
            CountDownLatch opened = new CountDownLatch(1);
            SwingUtilities.invokeAndWait(() -> {
                JButton bad = button(frame[0], "a-bad.raw");
                assertFalse(bad.isEnabled());
                assertTrue(bad.getToolTipText().contains("Bad RAW"));
                JButton good = button(frame[0], "b-good.raw");
                assertNotNull(good.getIcon());
                assertTrue(good.isEnabled());
                frame[0].addPropertyChangeListener("loading", event -> opened.countDown());
                good.doClick();
            });
            assertTrue(opened.await(5, TimeUnit.SECONDS));
            SwingUtilities.invokeAndWait(() -> {
                assertTrue(button(frame[0], "Volver al mosaico").isEnabled());
                button(frame[0], "Volver al mosaico").doClick();
                assertTrue(button(frame[0], "b-good.raw").isShowing() || !frame[0].isShowing());
            });
        } finally { SwingUtilities.invokeAndWait(() -> { if (frame[0] != null) frame[0].dispose(); }); }
    }

    static JButton button(Container parent, String text) {
        for (Component child : parent.getComponents()) {
            if (child instanceof JButton button && text.equals(button.getText())) return button;
            if (child instanceof Container container) {
                JButton found = button(container, text);
                if (found != null) return found;
            }
        }
        return null;
    }
}
