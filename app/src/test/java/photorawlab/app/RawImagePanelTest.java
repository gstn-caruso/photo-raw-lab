package photorawlab.app;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.image.BufferedImage;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import photorawlab.domain.RgbImage;

class RawImagePanelTest {
    @Test
    void paintsRgbImageCenteredWithoutDistortingItsProportions() throws Exception {
        BufferedImage image = RawImagePanel.toBufferedImage(
                new RgbImage(2, 1, new int[] {0xff0000, 0x00ff00}));
        assertEquals(0xff0000, image.getRGB(0, 0) & 0xffffff);
        assertEquals(0x00ff00, image.getRGB(1, 0) & 0xffffff);
        SwingUtilities.invokeAndWait(() -> {
            RawImagePanel panel = new RawImagePanel();
            panel.setImage(image);
            assertFit(panel, 100, 100, 25, 75);
            assertFit(panel, 200, 100, 0, 100);
        });
    }

    private void assertFit(RawImagePanel panel, int width, int height, int top, int bottom) {
        panel.setSize(width, height);
        BufferedImage rendered = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        var graphics = rendered.createGraphics();
        try {
            panel.paint(graphics);
        } finally {
            graphics.dispose();
        }
        assertEquals(0xff0000, rendered.getRGB(width / 4, top) & 0xffffff);
        assertEquals(0x00ff00, rendered.getRGB(3 * width / 4, bottom - 1) & 0xffffff);
        if (top > 0) {
            assertEquals(panel.getBackground().getRGB() & 0xffffff,
                    rendered.getRGB(width / 2, top - 1) & 0xffffff);
            assertEquals(panel.getBackground().getRGB() & 0xffffff,
                    rendered.getRGB(width / 2, bottom) & 0xffffff);
        }
    }
}
