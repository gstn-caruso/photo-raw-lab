package photorawlab.app;

import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import org.junit.jupiter.api.Test;
import photorawlab.domain.RgbImage;

import static org.junit.jupiter.api.Assertions.*;

class ProcessedBitmapTest {
    @Test
    void decodesEmbeddedJpegWithDimensionsFromItsPayload() throws Exception {
        var source = new java.awt.image.BufferedImage(2, 1, java.awt.image.BufferedImage.TYPE_INT_RGB);
        source.setRGB(0, 0, 0x336699);
        source.setRGB(1, 0, 0x336699);
        var output = new java.io.ByteArrayOutputStream();
        assertTrue(javax.imageio.ImageIO.write(source, "jpeg", output));
        byte[] jpeg = output.toByteArray();
        int expectedPixel = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(jpeg)).getRGB(0, 0) & 0xffffff;
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment thumbnail = arena.allocate(16 + jpeg.length, 4);
            thumbnail.set(ValueLayout.JAVA_INT, 0, 1);
            thumbnail.set(ValueLayout.JAVA_INT, 12, jpeg.length);
            thumbnail.asSlice(16).copyFrom(MemorySegment.ofArray(jpeg));
            RgbImage image = ProcessedBitmap.copyToRgb(thumbnail);
            assertEquals(2, image.width());
            assertEquals(1, image.height());
            assertEquals(expectedPixel, image.pixels()[0]);
            thumbnail.fill((byte) 0);
            assertEquals(expectedPixel, image.pixels()[0]);
        }
    }

    @Test
    void rejectsUnreadableJpegAndInvalidPayloadSizes() {
        try (Arena arena = Arena.ofConfined()) {
            assertThrows(IOException.class, () -> ProcessedBitmap.copyToRgb(bitmap(arena, 1, 0, 0, 0, 0, 0)));
            assertThrows(IOException.class, () -> ProcessedBitmap.copyToRgb(bitmap(arena, 1, 0, 0, 0, 0, -1)));
            assertThrows(IOException.class, () -> ProcessedBitmap.copyToRgb(bitmap(arena, 1, 0, 0, 0, 0, 16)));
        }
    }

    @Test
    void copiesUnsignedRgbBytesToHeap() throws IOException {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment bitmap = bitmap(arena, 2, 1, 2, 3, 8, 6);
            bitmap.asSlice(16, 6).copyFrom(MemorySegment.ofArray(new byte[]{(byte) 255, 0, 1, 2, 3, (byte) 128}));
            RgbImage image = ProcessedBitmap.copyToRgb(bitmap);
            assertEquals(2, image.width());
            assertEquals(1, image.height());
            assertArrayEquals(new int[]{0xff0001, 0x020380}, image.pixels());
            bitmap.fill((byte) 0);
            assertArrayEquals(new int[]{0xff0001, 0x020380}, image.pixels());
        }
    }

    @Test
    void rejectsInvalidHeaderBeforeReadingPixelMemory() {
        try (Arena arena = Arena.ofConfined()) {
            assertThrows(IOException.class, () -> ProcessedBitmap.copyToRgb(MemorySegment.NULL));
            assertThrows(IOException.class, () -> ProcessedBitmap.copyToRgb(bitmap(arena, 1, 1, 1, 3, 8, 3)));
            assertThrows(IOException.class, () -> ProcessedBitmap.copyToRgb(bitmap(arena, 2, 0, 1, 3, 8, 0)));
            assertThrows(IOException.class, () -> ProcessedBitmap.copyToRgb(bitmap(arena, 2, 1, 1, 4, 8, 4)));
            assertThrows(IOException.class, () -> ProcessedBitmap.copyToRgb(bitmap(arena, 2, 1, 1, 3, 16, 6)));
            assertThrows(IOException.class, () -> ProcessedBitmap.copyToRgb(bitmap(arena, 2, 1, 1, 3, 8, 2)));
            assertThrows(IOException.class, () -> ProcessedBitmap.copyToRgb(bitmap(arena, 2, 1, 1, 3, 8, -1)));
            assertThrows(IOException.class, () -> ProcessedBitmap.copyToRgb(bitmap(arena, 2, 65535, 65535, 3, 8, 0)));
        }
    }

    private MemorySegment bitmap(Arena arena, int type, int height, int width, int colors, int bits, int size) {
        MemorySegment bitmap = arena.allocate(32, 4);
        bitmap.set(ValueLayout.JAVA_INT, 0, type);
        bitmap.set(ValueLayout.JAVA_SHORT, 4, (short) height);
        bitmap.set(ValueLayout.JAVA_SHORT, 6, (short) width);
        bitmap.set(ValueLayout.JAVA_SHORT, 8, (short) colors);
        bitmap.set(ValueLayout.JAVA_SHORT, 10, (short) bits);
        bitmap.set(ValueLayout.JAVA_INT, 12, size);
        return bitmap;
    }
}
