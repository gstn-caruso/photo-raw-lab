package photorawlab.app;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import photorawlab.domain.RgbImage;

class PreviewRendererTest {
    @Test void preservesAspectRatioBoundsAndPhotographicPixels() throws Exception {
        int[] pixels = new int[800 * 400];
        java.util.Arrays.fill(pixels, 0x4080c0);
        var preview = new PreviewRenderer(path -> new RgbImage(800, 400, pixels)).render(Path.of("wide.raw"));
        assertEquals(240, preview.getWidth());
        assertEquals(120, preview.getHeight());
        assertEquals(0x4080c0, preview.getRGB(100, 50) & 0xffffff);
    }

    @Test void doesNotEnlargeSmallImagesAndBoundsPortraits() throws Exception {
        var small = new PreviewRenderer(path -> new RgbImage(1, 2, new int[] {1, 2})).render(Path.of("small.raw"));
        assertEquals(1, small.getWidth());
        assertEquals(2, small.getHeight());
        var tall = new PreviewRenderer(path -> new RgbImage(400, 800, new int[320000])).render(Path.of("tall.raw"));
        assertEquals(90, tall.getWidth());
        assertEquals(180, tall.getHeight());
    }
}
