package photorawlab.app;

import static org.junit.jupiter.api.Assertions.*;
import java.awt.Container;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.*;
import org.junit.jupiter.api.Test;

class GalleryControlsTest {
    @org.junit.jupiter.api.io.TempDir Path directory;

    @Test void sortingDuringDecodeUsesMetadataAndKeepsPendingPreviewAttachedToItsPath() throws Exception {
        Path alpha = java.nio.file.Files.write(directory.resolve("a.raw"), new byte[20]);
        Path beta = java.nio.file.Files.write(directory.resolve("b.raw"), new byte[10]);
        var entered = new java.util.concurrent.CountDownLatch(1);
        var release = new java.util.concurrent.CountDownLatch(1);
        var done = new java.util.concurrent.CountDownLatch(1);
        var decodes = new java.util.concurrent.atomic.AtomicInteger();
        RawViewerFrame[] frame = new RawViewerFrame[1];
        java.util.concurrent.CompletableFuture<DirectoryMosaicPanel> shown = new java.util.concurrent.CompletableFuture<>();
        Timer observer = new Timer(10, event -> {
            DirectoryMosaicPanel mosaic = findMosaic(frame[0]);
            if (grid(mosaic).getComponentCount() == 2) shown.complete(mosaic);
        });
        try {
            SwingUtilities.invokeAndWait(() -> {
                frame[0] = new RawViewerFrame(path -> {
                    decodes.incrementAndGet();
                    if (path.equals(alpha)) {
                        entered.countDown();
                        try { assertTrue(release.await(5, java.util.concurrent.TimeUnit.SECONDS)); }
                        catch (InterruptedException error) { throw new java.io.IOException(error); }
                    }
                    return new photorawlab.domain.RgbImage(1, 1, new int[] {path.equals(alpha) ? 0xff0000 : 0x00ff00});
                });
                frame[0].addPropertyChangeListener("loading", event -> done.countDown());
                frame[0].openDirectory(directory);
                observer.start();
            });
            assertTrue(entered.await(5, java.util.concurrent.TimeUnit.SECONDS));
            var mosaic = shown.get(5, java.util.concurrent.TimeUnit.SECONDS);
            JButton[] original = new JButton[2];
            SwingUtilities.invokeAndWait(() -> {
                observer.stop();
                original[0] = (JButton) grid(mosaic).getComponent(0);
                original[1] = (JButton) grid(mosaic).getComponent(1);
                ((JComboBox<?>) ((JPanel) mosaic.getComponent(1)).getComponent(1)).setSelectedIndex(2);
                assertSame(original[1], grid(mosaic).getComponent(0), "Smaller file must move first while decoding is blocked");
            });
            release.countDown();
            assertTrue(done.await(5, java.util.concurrent.TimeUnit.SECONDS));
            SwingUtilities.invokeAndWait(() -> {
                assertSame(original[1], grid(mosaic).getComponent(0));
                assertNotNull(original[0].getIcon());
                assertNotNull(original[1].getIcon());
                assertTrue(original[0].isEnabled());
                assertTrue(original[1].isEnabled());
                assertPreviewColor(original[0], 0xff0000);
                assertPreviewColor(original[1], 0x00ff00);
            });
            assertEquals(2, decodes.get());
        } finally {
            release.countDown();
            SwingUtilities.invokeAndWait(() -> { observer.stop(); if (frame[0] != null) frame[0].dispose(); });
        }
    }

    private static Container grid(DirectoryMosaicPanel mosaic) {
        return (Container) ((JScrollPane) mosaic.getComponent(0)).getViewport().getView();
    }

    private static void assertPreviewColor(JButton tile, int expected) {
        var painted = new BufferedImage(240, 180, BufferedImage.TYPE_INT_RGB);
        var graphics = painted.createGraphics();
        try { tile.getIcon().paintIcon(tile, graphics, 0, 0); }
        finally { graphics.dispose(); }
        assertEquals(expected, painted.getRGB(119, 89) & 0xffffff);
    }

    static DirectoryMosaicPanel findMosaic(Container container) {
        if (container instanceof DirectoryMosaicPanel mosaic) return mosaic;
        for (var component : container.getComponents()) {
            if (component instanceof Container child) {
                var found = findMosaic(child);
                if (found != null) return found;
            }
        }
        return null;
    }
    @Test void directionReordersExistingTilesPreservingPreviewErrorAndAction() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            AtomicReference<Path> opened = new AtomicReference<>();
            var mosaic = new DirectoryMosaicPanel(opened::set);
            Path alpha = Path.of("a.raw"), beta = Path.of("b.raw");
            mosaic.showFiles(List.of(beta, alpha));
            Container grid = (Container) ((JScrollPane) mosaic.getComponent(0)).getViewport().getView();
            assertEquals("a.raw", ((JButton) grid.getComponent(0)).getText());
            JButton first = (JButton) grid.getComponent(0), second = (JButton) grid.getComponent(1);
            mosaic.showPreview(alpha, new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB));
            mosaic.showError(beta, "corrupto");
            Icon icon = first.getIcon();
            JPanel controls = (JPanel) mosaic.getComponent(1);
            JComboBox<?> direction = (JComboBox<?>) controls.getComponent(3);
            assertEquals("Ascendente", direction.getSelectedItem().toString());
            direction.setSelectedIndex(1);
            assertSame(second, grid.getComponent(0));
            assertSame(first, grid.getComponent(1));
            assertSame(icon, first.getIcon());
            assertFalse(second.isEnabled());
            assertEquals("No se pudo abrir: corrupto", second.getToolTipText());
            first.doClick();
            assertEquals(alpha, opened.get());
            assertEquals(AppPalette.PANEL, direction.getBackground());
            assertEquals(AppPalette.TEXT, direction.getForeground());
            assertSame(direction, ((JLabel) controls.getComponent(2)).getLabelFor());
        });
    }
}
