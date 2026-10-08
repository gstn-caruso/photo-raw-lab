package photorawlab.app;

import org.junit.jupiter.api.Test;
import photorawlab.domain.RgbImage;

import static org.junit.jupiter.api.Assertions.*;

class PreviewOrientationTest {
    @Test
    void appliesLibRawRotationAndMirroringToNonSquarePixels() {
        RgbImage source = new RgbImage(3, 2, new int[] {1, 2, 3, 4, 5, 6});
        int[][] expected = {
                {1, 2, 3, 4, 5, 6}, {3, 2, 1, 6, 5, 4}, {4, 5, 6, 1, 2, 3}, {6, 5, 4, 3, 2, 1},
                {1, 4, 2, 5, 3, 6}, {3, 6, 2, 5, 1, 4}, {4, 1, 5, 2, 6, 3}, {6, 3, 5, 2, 4, 1}};
        for (int flip = 0; flip < expected.length; flip++) {
            RgbImage result = PreviewOrientation.apply(source, flip);
            assertEquals(flip < 4 ? 3 : 2, result.width());
            assertEquals(flip < 4 ? 2 : 3, result.height());
            assertArrayEquals(expected[flip], result.pixels(), "flip=" + flip);
        }
    }
}
