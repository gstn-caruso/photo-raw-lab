package photorawlab.app;

import photorawlab.domain.RgbImage;

final class PreviewOrientation {
    private PreviewOrientation() { }

    static RgbImage apply(RgbImage source, int flip) {
        if (flip == 0) {
            return source;
        }
        boolean transpose = (flip & 4) != 0;
        int width = transpose ? source.height() : source.width();
        int height = transpose ? source.width() : source.height();
        int[] original = source.pixels();
        int[] rotated = new int[original.length];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int sourceX = transpose ? y : x;
                int sourceY = transpose ? x : y;
                if ((flip & 1) != 0) { sourceX = source.width() - 1 - sourceX; }
                if ((flip & 2) != 0) { sourceY = source.height() - 1 - sourceY; }
                rotated[y * width + x] = original[sourceY * source.width() + sourceX];
            }
        }
        return new RgbImage(width, height, rotated);
    }
}
