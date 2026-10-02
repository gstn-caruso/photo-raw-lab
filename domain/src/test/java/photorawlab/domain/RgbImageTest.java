package photorawlab.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RgbImageTest {
    @Test
    void preservesDimensionsAndProtectsPixels() {
        int[] pixels = {0x123456, 0xabcdef};
        RgbImage image = new RgbImage(2, 1, pixels);
        pixels[0] = 0;
        image.pixels()[1] = 0;
        assertEquals(2, image.width());
        assertEquals(1, image.height());
        assertArrayEquals(new int[]{0x123456, 0xabcdef}, image.pixels());
    }

    @Test
    void rejectsInvalidDimensionsOrPixelCount() {
        assertThrows(IllegalArgumentException.class, () -> new RgbImage(0, 1, new int[0]));
        assertThrows(IllegalArgumentException.class, () -> new RgbImage(1, -1, new int[0]));
        assertThrows(IllegalArgumentException.class, () -> new RgbImage(2, 1, new int[1]));
        assertThrows(IllegalArgumentException.class, () -> new RgbImage(Integer.MAX_VALUE, 2, new int[0]));
        assertThrows(NullPointerException.class, () -> new RgbImage(1, 1, null));
    }
}
