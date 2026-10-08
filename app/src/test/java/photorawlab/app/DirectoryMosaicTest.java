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
import java.util.Optional;
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
                }, new LastDirectory() {
                    public Optional<Path> load() { return Optional.empty(); }
                    public void save(Path path) {}
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

    @Test void returningDuringPreviewLoadingFinishesRemainingTiles() throws Exception {
        Path first = Files.createFile(directory.resolve("a.raw"));
        Files.createFile(directory.resolve("b.raw"));
        RawViewerFrame[] frame = new RawViewerFrame[1];
        CountDownLatch secondEntered = new CountDownLatch(1), releaseSecond = new CountDownLatch(1);
        java.util.concurrent.CompletableFuture<JButton> firstTile = new java.util.concurrent.CompletableFuture<>();
        javax.swing.Timer observer = new javax.swing.Timer(25, event -> {
            JButton tile = button(frame[0], "a.raw");
            if (tile != null && tile.isEnabled()) firstTile.complete(tile);
        });
        try {
            SwingUtilities.invokeAndWait(() -> {
                frame[0] = new RawViewerFrame(path -> {
                    if (!path.equals(first)) {
                        secondEntered.countDown();
                        try { assertTrue(releaseSecond.await(5, TimeUnit.SECONDS)); }
                        catch (InterruptedException error) { throw new IOException(error); }
                    }
                    return new RgbImage(1, 1, new int[] {0xff0000});
                }, new LastDirectory() {
                    public Optional<Path> load() { return Optional.empty(); }
                    public void save(Path path) {}
                });
                observer.start();
                frame[0].openDirectory(directory);
            });
            JButton tile = firstTile.get(5, TimeUnit.SECONDS);
            assertTrue(secondEntered.await(5, TimeUnit.SECONDS));
            SwingUtilities.invokeAndWait(() -> {
                tile.doClick();
                button(frame[0], "Volver al mosaico").doClick();
            });
            releaseSecond.countDown();
            java.util.concurrent.CompletableFuture<JButton> finished = new java.util.concurrent.CompletableFuture<>();
            javax.swing.Timer completion = new javax.swing.Timer(25, event -> {
                JButton last = button(frame[0], "b.raw");
                if (last != null && last.getIcon() != null) finished.complete(last);
            });
            try {
                SwingUtilities.invokeAndWait(completion::start);
                assertNotNull(finished.get(2, TimeUnit.SECONDS).getIcon());
            } finally { SwingUtilities.invokeAndWait(completion::stop); }
        } finally {
            releaseSecond.countDown();
            SwingUtilities.invokeAndWait(() -> { observer.stop(); if (frame[0] != null) frame[0].dispose(); });
        }
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
