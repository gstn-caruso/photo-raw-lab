package photorawlab.app;

import java.io.IOException;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import photorawlab.domain.RgbImage;

final class ProcessedBitmap {
    private static final long HEADER_SIZE = 16;
    private static final int RGB_BITMAP = 2;

    private ProcessedBitmap() { }

    static RgbImage copyToRgb(MemorySegment pointer) throws IOException {
        if (pointer.address() == 0) {
            throw new IOException("LibRaw returned no processed bitmap");
        }
        // LibRaw 0.21 libraw_processed_image_t: int type, four ushorts, uint data_size, RGB data.
        MemorySegment header = pointer.reinterpret(HEADER_SIZE);
        int type = header.get(ValueLayout.JAVA_INT, 0);
        int height = Short.toUnsignedInt(header.get(ValueLayout.JAVA_SHORT, 4));
        int width = Short.toUnsignedInt(header.get(ValueLayout.JAVA_SHORT, 6));
        int channels = Short.toUnsignedInt(header.get(ValueLayout.JAVA_SHORT, 8));
        int bits = Short.toUnsignedInt(header.get(ValueLayout.JAVA_SHORT, 10));
        long dataSize = Integer.toUnsignedLong(header.get(ValueLayout.JAVA_INT, 12));
        long pixelCount = (long) width * height;
        if (type != RGB_BITMAP || width == 0 || height == 0 || channels != 3 || bits != 8
                || pixelCount > Integer.MAX_VALUE || dataSize != pixelCount * 3) {
            throw new IOException("LibRaw returned an invalid bitmap: " + width + "x" + height
                    + ", type=" + type + ", channels=" + channels + ", bits=" + bits + ", bytes=" + dataSize);
        }
        MemorySegment bytes = pointer.reinterpret(HEADER_SIZE + dataSize).asSlice(HEADER_SIZE, dataSize);
        int[] pixels = new int[(int) pixelCount];
        for (int pixel = 0; pixel < pixels.length; pixel++) {
            long offset = (long) pixel * 3;
            int red = Byte.toUnsignedInt(bytes.get(ValueLayout.JAVA_BYTE, offset));
            int green = Byte.toUnsignedInt(bytes.get(ValueLayout.JAVA_BYTE, offset + 1));
            int blue = Byte.toUnsignedInt(bytes.get(ValueLayout.JAVA_BYTE, offset + 2));
            pixels[pixel] = (red << 16) | (green << 8) | blue;
        }
        return new RgbImage(width, height, pixels);
    }
}
