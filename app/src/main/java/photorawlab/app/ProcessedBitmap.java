package photorawlab.app;

import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import javax.imageio.ImageIO;
import photorawlab.domain.RgbImage;

final class ProcessedBitmap {
    private static final long HEADER_SIZE = 16;
    private static final int RGB_BITMAP = 2;
    private static final int JPEG = 1;

    private ProcessedBitmap() { }

    static RgbImage copyToRgb(MemorySegment pointer) throws IOException {
        return copyToRgb(pointer, 0);
    }

    static RgbImage copyToRgb(MemorySegment pointer, int cameraFlip) throws IOException {
        if (pointer.address() == 0) {
            throw new IOException("LibRaw returned no processed bitmap");
        }
        // LibRaw 0.21 libraw_processed_image_t: int type, four ushorts, uint data_size, RGB data.
        MemorySegment header = pointer.reinterpret(HEADER_SIZE);
        int type = header.get(ValueLayout.JAVA_INT, 0);
        if (type == JPEG) {
            return copyJpegToRgb(pointer, Integer.toUnsignedLong(header.get(ValueLayout.JAVA_INT, 12)), cameraFlip);
        }
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
        return PreviewOrientation.apply(new RgbImage(width, height, pixels), cameraFlip);
    }

    private static RgbImage copyJpegToRgb(MemorySegment pointer, long dataSize, int cameraFlip) throws IOException {
        if (dataSize == 0 || dataSize > Integer.MAX_VALUE) {
            throw new IOException("LibRaw returned an invalid JPEG size: " + dataSize);
        }
        byte[] bytes = pointer.reinterpret(HEADER_SIZE + dataSize).asSlice(HEADER_SIZE, dataSize)
                .toArray(ValueLayout.JAVA_BYTE);
        var image = ImageIO.read(new ByteArrayInputStream(bytes));
        if (image == null) {
            throw new IOException("LibRaw returned an unreadable JPEG thumbnail");
        }
        int[] pixels = image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
        for (int pixel = 0; pixel < pixels.length; pixel++) {
            pixels[pixel] &= 0xffffff;
        }
        return PreviewOrientation.apply(new RgbImage(image.getWidth(), image.getHeight(), pixels),
                JpegOrientation.flip(bytes, cameraFlip));
    }
}
