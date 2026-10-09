package photorawlab.app;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Path;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import photorawlab.domain.GalleryPhoto;

class PhotoTileTest {
    @Test void tileShowsKnownMetadataAndUnknownsWithoutLosingFilenamePreviewOrAction() throws Exception {
        Path raw = Path.of("camera.dng");
        Path fixture = java.nio.file.Files.createTempFile("exif-test", ".dng");
        try {
            java.nio.file.Files.write(fixture, RawMetadataReaderTest.exifTiff());
            var metadata = new RawMetadataReader().read(fixture);
            SwingUtilities.invokeAndWait(() -> {
                var opened = new AtomicReference<Path>();
                var mosaic = new DirectoryMosaicPanel(opened::set);
                mosaic.showEntries(java.util.List.of(new RawDirectory.Entry(raw, new GalleryPhoto("camera.dng", Instant.EPOCH, 0, metadata))));
                var grid = (java.awt.Container) ((JScrollPane) mosaic.getComponent(0)).getViewport().getView();
                JButton tile = (JButton) grid.getComponent(0);
                assertEquals("camera.dng", tile.getText());
                assertTrue(tile.getAccessibleContext().getAccessibleDescription().contains("Test Camera"));
                assertTrue(tile.getAccessibleContext().getAccessibleDescription().contains("ISO 400"));
                assertTrue(tile.getAccessibleContext().getAccessibleDescription().contains("1/125 s"));
                assertTrue(tile.getToolTipText().contains("6000 × 4000"));
                assertTrue(tile.getToolTipText().contains("2024-05-06"));
                mosaic.showPreview(raw, new java.awt.image.BufferedImage(1, 1, java.awt.image.BufferedImage.TYPE_INT_RGB));
                tile.doClick();
                assertEquals(raw, opened.get());
                assertEquals(240, tile.getIcon().getIconWidth());
                mosaic.showFiles(java.util.List.of(Path.of("unknown.raw")));
                tile = (JButton) grid.getComponent(0);
                assertTrue(tile.getAccessibleContext().getAccessibleDescription().contains("Sin datos"));
            });
        } finally { java.nio.file.Files.deleteIfExists(fixture); }
    }
}
