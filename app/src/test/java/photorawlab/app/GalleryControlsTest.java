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
