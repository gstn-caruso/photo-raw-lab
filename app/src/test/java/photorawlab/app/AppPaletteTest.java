package photorawlab.app;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Color;
import java.awt.Container;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;

class AppPaletteTest {
    @Test
    void galleryUsesNeutralGrayWhileLoadingAndAfterPreviewArrives() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            DirectoryMosaicPanel mosaic = new DirectoryMosaicPanel(path -> {});
            Path photo = Path.of("photo.raw");
            mosaic.showFiles(List.of(photo));
            JScrollPane scroll = (JScrollPane) mosaic.getComponent(0);
            Container tiles = (Container) scroll.getViewport().getView();
            JButton tile = (JButton) tiles.getComponent(0);
            assertEquals(new Color(0x404040), tiles.getBackground());
            assertEquals(new Color(0x4d4d4d), scroll.getViewport().getBackground());
            assertEquals(0x6b6b6b, paintedCorner(tile));
            assertEquals(new Color(0xd6d6d6), tile.getForeground());
            mosaic.showPreview(photo, new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB));
            assertEquals(0x6b6b6b, paintedCorner(tile));
            tile.getModel().setArmed(true);
            tile.getModel().setPressed(true);
            assertEquals(0x929292, paintedCorner(tile));
        });
    }

    @Test
    void viewerUsesDarkChromeAndLightText() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            RawViewerFrame frame = new RawViewerFrame(path -> {
                throw new AssertionError("No photo should be decoded");
            });
            try {
                Container toolbar = (Container) frame.getContentPane().getComponent(0);
                assertEquals(new Color(0x333333), toolbar.getBackground());
                Container actions = (Container) toolbar.getComponent(0);
                assertEquals(new Color(0x333333), actions.getBackground());
                assertEquals(0x4d4d4d, paintedCorner((JButton) actions.getComponent(0)));
                assertEquals(new Color(0xd6d6d6), actions.getComponent(0).getForeground());
                assertEquals(new Color(0xd6d6d6), toolbar.getComponent(1).getForeground());
                assertEquals(new Color(0x4d4d4d), new RawImagePanel().getBackground());
            } finally {
                frame.dispose();
            }
        });
    }

    private static int paintedCorner(JComponent component) {
        component.setSize(240, 180);
        BufferedImage painted = new BufferedImage(240, 180, BufferedImage.TYPE_INT_RGB);
        var graphics = painted.createGraphics();
        try {
            component.paint(graphics);
        } finally {
            graphics.dispose();
        }
        return painted.getRGB(8, 8) & 0xffffff;
    }
}
