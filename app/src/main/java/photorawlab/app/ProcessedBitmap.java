package photorawlab.app;

import photorawlab.domain.RgbImage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

import javax.imageio.ImageIO;

final class ProcessedBitmap {
    private static final int RGB_BITMAP = 2;
    private static final int JPEG = 1;

    private ProcessedBitmap() {}

    static RgbImage copyToRgb(MemorySegment pointer) throws IOException {
        return copyToRgb(pointer, 0);
    }

    static RgbImage copyToRgb(MemorySegment pointer, int cameraFlip) throws IOException {
        if (pointer.address() == 0) {
            throw new IOException("LibRaw returned no processed bitmap");
        }
        int[] info;
        MemorySegment data;
        try (var arena = java.lang.foreign.Arena.ofConfined()) {
            var metadata = arena.allocate(6 * 4, 4);
            photorawlab.adapters.NativeBridge.call(
                    "pr_bitmap_info",
                    java.lang.foreign.FunctionDescriptor.ofVoid(
                            ValueLayout.ADDRESS, ValueLayout.ADDRESS),
                    pointer,
                    metadata);
            info = metadata.toArray(ValueLayout.JAVA_INT);
            data =
                    (MemorySegment)
                            photorawlab.adapters.NativeBridge.call(
                                    "pr_bitmap_data",
                                    java.lang.foreign.FunctionDescriptor.of(
                                            ValueLayout.ADDRESS, ValueLayout.ADDRESS),
                                    pointer);
        }
        int type = info[0], height = info[1], width = info[2], channels = info[3], bits = info[4];
        long dataSize = Integer.toUnsignedLong(info[5]);
        if (type == JPEG) return copyJpegToRgb(data, dataSize, cameraFlip);
        long pixelCount = (long) width * height;
        if (type != RGB_BITMAP
                || width == 0
                || height == 0
                || channels != 3
                || bits != 8
                || pixelCount > Integer.MAX_VALUE
                || dataSize != pixelCount * 3) {
            throw new IOException(
                    "LibRaw returned an invalid bitmap: "
                            + width
                            + "x"
                            + height
                            + ", type="
                            + type
                            + ", channels="
                            + channels
                            + ", bits="
                            + bits
                            + ", bytes="
                            + dataSize);
        }
        MemorySegment bytes = data.reinterpret(dataSize);
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

    private static RgbImage copyJpegToRgb(MemorySegment pointer, long dataSize, int cameraFlip)
            throws IOException {
        if (dataSize == 0 || dataSize > Integer.MAX_VALUE) {
            throw new IOException("LibRaw returned an invalid JPEG size: " + dataSize);
        }
        byte[] bytes = pointer.reinterpret(dataSize).toArray(ValueLayout.JAVA_BYTE);
        var image = ImageIO.read(new ByteArrayInputStream(bytes));
        if (image == null) {
            throw new IOException("LibRaw returned an unreadable JPEG thumbnail");
        }
        int[] pixels =
                image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
        for (int pixel = 0; pixel < pixels.length; pixel++) {
            pixels[pixel] &= 0xffffff;
        }
        return PreviewOrientation.apply(
                new RgbImage(image.getWidth(), image.getHeight(), pixels),
                JpegOrientation.flip(bytes, cameraFlip));
    }
}
