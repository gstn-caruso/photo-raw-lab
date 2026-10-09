package photorawlab.app;

import static org.junit.jupiter.api.Assertions.*;
import java.awt.Container;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.*;
import org.junit.jupiter.api.Test;
import photorawlab.domain.GalleryPhoto;
import photorawlab.domain.PhotoMetadata;

class MetadataFilterPanelTest {
    @Test void filteringKeepsLateHiddenPreviewsErrorsAndActionsAndReportsVisibleTotal() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            var opened = new AtomicReference<Path>();
            var mosaic = new DirectoryMosaicPanel(opened::set);
            Path alpha = Path.of("a.dng"), beta = Path.of("b.cr2"), failed = Path.of("c.cr2");
            mosaic.showFiles(List.of(alpha, beta, failed));
            var types = facet(mosaic, "Tipo de archivo");
            assertEquals(4, lists(mosaic).size());
            select(types, "DNG");
            assertEquals(1, grid(mosaic).getComponentCount());
            assertTrue(labels(mosaic).stream().anyMatch(label -> label.getText().equals("1 de 3 fotos")));
            mosaic.showPreview(beta, new java.awt.image.BufferedImage(2, 2, java.awt.image.BufferedImage.TYPE_INT_RGB));
            mosaic.showError(failed, "corrupto");
            select(types, "Todas");
            JButton betaTile = (JButton) grid(mosaic).getComponent(1);
            assertTrue(betaTile.isEnabled());
            assertNotNull(betaTile.getIcon());
            assertEquals("No se pudo abrir: corrupto", ((JButton) grid(mosaic).getComponent(2)).getToolTipText());
            ((JComboBox<?>) ((JPanel) mosaic.getComponent(1)).getComponent(3)).setSelectedIndex(1);
            assertSame(betaTile, grid(mosaic).getComponent(1));
            betaTile.doClick();
            assertEquals(beta, opened.get());
            select(facet(mosaic, "Cámara"), "Sin datos");
            assertEquals(3, grid(mosaic).getComponentCount());
            select(types, "DNG");
            mosaic.showFiles(List.of(Path.of("new.raw")));
            assertEquals(1, grid(mosaic).getComponentCount(), "New directory clears filters");
            assertEquals("Todas", types.getSelectedValue().toString().split(" \\(")[0]);
            assertEquals(1, ((JComboBox<?>) ((JPanel) mosaic.getComponent(1)).getComponent(3)).getSelectedIndex());
        });
    }

    @Test void metadataColumnsReflowAndRemainReachableInWideMediumAndNarrowWindows() throws Exception {
        JFrame[] frame = new JFrame[1];
        try {
            SwingUtilities.invokeAndWait(() -> {
                frame[0] = new JFrame();
                frame[0].setContentPane(new DirectoryMosaicPanel(path -> {}));
                frame[0].setSize(1080, 770);
                frame[0].setVisible(true);
            });
            for (int width : new int[] {1080, 650, 350}) {
                SwingUtilities.invokeAndWait(() -> { frame[0].setSize(width, 770); frame[0].validate(); });
                SwingUtilities.invokeAndWait(() -> {
                    var mosaic = (DirectoryMosaicPanel) frame[0].getContentPane();
                    assertEquals(4, lists(mosaic).size());
                    for (var list : lists(mosaic)) {
                        assertTrue(list.isFocusable());
                        var bounds = SwingUtilities.convertRectangle(list, list.getVisibleRect(), mosaic);
                        assertTrue(mosaic.getVisibleRect().contains(bounds), bounds.toString());
                        assertTrue(bounds.width > 100);
                        assertTrue(bounds.height > 30);
                    }
                    var camera = facet(mosaic, "Cámara");
                    var date = facet(mosaic, "Fecha de captura");
                    var cameraOrigin = SwingUtilities.convertPoint(camera, 0, 0, mosaic);
                    var dateOrigin = SwingUtilities.convertPoint(date, 0, 0, mosaic);
                    assertEquals(width == 350, cameraOrigin.y > dateOrigin.y);
                });
            }
        } finally {
            SwingUtilities.invokeAndWait(() -> { if (frame[0] != null) frame[0].dispose(); });
        }
    }

    static JList<?> facet(Container container, String name) {
        return lists(container).stream().filter(list -> name.equals(list.getAccessibleContext().getAccessibleName())).findFirst().orElseThrow();
    }
    static void select(JList<?> list, String value) {
        for (int i = 0; i < list.getModel().getSize(); i++) {
            if (list.getModel().getElementAt(i).toString().startsWith(value + " (")) { list.setSelectedIndex(i); return; }
        }
        fail("Missing facet value " + value);
    }
    private static List<JList<?>> lists(Container container) {
        var result = new java.util.ArrayList<JList<?>>();
        for (var child : container.getComponents()) {
            if (child instanceof JList<?> list) result.add(list);
            else if (child instanceof Container nested) result.addAll(lists(nested));
        }
        return result;
    }
    private static List<JLabel> labels(Container container) {
        var result = new java.util.ArrayList<JLabel>();
        for (var child : container.getComponents()) {
            if (child instanceof JLabel label) result.add(label);
            else if (child instanceof Container nested) result.addAll(labels(nested));
        }
        return result;
    }
    static Container grid(DirectoryMosaicPanel mosaic) { return (Container) ((JScrollPane) mosaic.getComponent(0)).getViewport().getView(); }
}
