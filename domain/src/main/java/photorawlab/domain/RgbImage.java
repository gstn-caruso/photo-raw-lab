package photorawlab.domain;

import java.util.Objects;

public final class RgbImage {
    private final int width;
    private final int height;
    private final int[] pixels;

    public RgbImage(int width, int height, int[] pixels) {
        Objects.requireNonNull(pixels, "pixels");
        if (width <= 0 || height <= 0 || (long) width * height != pixels.length) {
            throw new IllegalArgumentException("Dimensions must be positive and match the pixel count");
        }
        this.width = width;
        this.height = height;
        this.pixels = pixels.clone();
    }

    public int width() { return width; }

    public int height() { return height; }

    public int[] pixels() { return pixels.clone(); }
}
