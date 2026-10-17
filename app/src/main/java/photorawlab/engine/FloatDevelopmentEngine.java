package photorawlab.engine;

import photorawlab.adapters.*;
import photorawlab.domain.edit.*;

import java.io.IOException;
import java.nio.file.*;

/** One bounded decoded preview cache. Export always decodes the original at full resolution. */
public final class FloatDevelopmentEngine implements DevelopmentEngine {
    public record Rendered(
            LinearImage image,
            float[] originalCoordinates,
            int originalWidth,
            int originalHeight,
            double pixelScale) {
        public Point originalAt(double u, double v) {
            int x = Math.max(0, Math.min(image.width() - 1, (int) (u * image.width()))),
                    y = Math.max(0, Math.min(image.height() - 1, (int) (v * image.height())));
            int i = (y * image.width() + x) * 2;
            return new Point(originalCoordinates[i], originalCoordinates[i + 1]);
        }
    }

    private final LinearRawDecoder decoder = new LinearRawDecoder();
    private String cacheKey = "";
    private LinearImage cached;
    private int originalWidth, originalHeight, cachedOriginalWidth, cachedOriginalHeight;

    @Override
    public synchronized LinearImage render(Path path, Recipe recipe, int maxSide)
            throws IOException {
        return renderInternal(path, recipe, maxSide, false).image();
    }

    public synchronized Rendered renderMapped(Path path, Recipe recipe, int maxSide)
            throws IOException {
        return renderInternal(path, recipe, maxSide, true);
    }

    private Rendered renderInternal(Path path, Recipe recipe, int maxSide, boolean keepMap)
            throws IOException {
        String key =
                path.toAbsolutePath()
                        + ":"
                        + Files.getLastModifiedTime(path)
                        + ":"
                        + Files.size(path)
                        + ":"
                        + recipe.adjustments().temperature()
                        + ":"
                        + recipe.adjustments().tint()
                        + ":"
                        + maxSide;
        LinearImage source;
        if (maxSide > 0 && key.equals(cacheKey)) {
            source = cached;
            originalWidth = cachedOriginalWidth;
            originalHeight = cachedOriginalHeight;
        } else {
            source = decoder.decode(path, recipe.adjustments());
            originalWidth = source.width();
            originalHeight = source.height();
            if (maxSide > 0) {
                source = resize(source, maxSide);
                cached = source;
                cachedOriginalWidth = originalWidth;
                cachedOriginalHeight = originalHeight;
                cacheKey = key;
            }
        }
        return process(source, recipe, keepMap, originalWidth, originalHeight);
    }

    public static LinearImage resize(LinearImage input, int maxSide) {
        double scale = Math.min(1, (double) maxSide / Math.max(input.width(), input.height()));
        if (scale == 1) return input;
        int w = Math.max(1, (int) Math.round(input.width() * scale)),
                h = Math.max(1, (int) Math.round(input.height() * scale));
        float[] out = new float[w * h * 3];
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
                for (int c = 0; c < 3; c++)
                    out[(y * w + x) * 3 + c] =
                            sample(
                                    input.pixels(),
                                    input.width(),
                                    input.height(),
                                    (x + .5) / scale - .5,
                                    (y + .5) / scale - .5,
                                    c);
        return new LinearImage(w, h, out);
    }

    public static Rendered process(
            LinearImage source, Recipe recipe, boolean keepMap, int originalW, int originalH)
            throws IOException {
        int w = source.width(), h = source.height();
        float[] corrected = new float[w * h * 3];
        float[] map = keepMap ? new float[w * h * 2] : null;
        try (Optics optics = new Optics(recipe.lens(), w, h)) {
            float[] pixels = optics.vignette(source);
            for (int y = 0; y < h; y++) {
                if (Thread.currentThread().isInterrupted())
                    throw new IOException("Render cancelado");
                float[] row = optics.row(y);
                for (int x = 0; x < w; x++) {
                    int i = (y * w + x) * 3;
                    for (int c = 0; c < 3; c++)
                        corrected[i + c] =
                                sample(pixels, w, h, row[x * 6 + c * 2], row[x * 6 + c * 2 + 1], c);
                    double ox = (row[x * 6 + 2] + .5) / w, oy = (row[x * 6 + 3] + .5) / h;
                    if (keepMap) {
                        map[(y * w + x) * 2] = (float) ox;
                        map[(y * w + x) * 2 + 1] = (float) oy;
                    }
                    adjust(corrected, i, recipe.adjustments(), 1);
                    for (Mask mask : recipe.masks())
                        adjust(
                                corrected,
                                i,
                                mask.adjustments(),
                                mask.weight(ox, oy, (double) w / h));
                }
            }
        }
        Geometry g = recipe.geometry();
        boolean turned = g.quarterTurns() % 2 != 0;
        int ow = Math.max(1, (int) Math.round((turned ? h : w) * g.width())),
                oh = Math.max(1, (int) Math.round((turned ? w : h) * g.height()));
        float[] output = new float[ow * oh * 3],
                outputMap = keepMap ? new float[ow * oh * 2] : null;
        for (int y = 0; y < oh; y++)
            for (int x = 0; x < ow; x++) {
                Point p = g.toOriginal((x + .5) / ow, (y + .5) / oh, w, h);
                double px = p.x() * w - .5, py = p.y() * h - .5;
                for (int c = 0; c < 3; c++)
                    output[(y * ow + x) * 3 + c] = sample(corrected, w, h, px, py, c);
                if (keepMap) {
                    int sx = Math.max(0, Math.min(w - 1, (int) Math.round(px))),
                            sy = Math.max(0, Math.min(h - 1, (int) Math.round(py)));
                    outputMap[(y * ow + x) * 2] = map[(sy * w + sx) * 2];
                    outputMap[(y * ow + x) * 2 + 1] = map[(sy * w + sx) * 2 + 1];
                }
            }
        return new Rendered(
                new LinearImage(ow, oh, output),
                outputMap,
                originalW,
                originalH,
                (double) originalW / w);
    }

    private static void adjust(float[] p, int i, Adjustments a, double weight) {
        if (weight <= 0 || a.equals(Adjustments.NEUTRAL)) return;
        double r = p[i], g = p[i + 1], b = p[i + 2], lum = .2627 * r + .6780 * g + .0593 * b;
        double l = Math.max(0, Math.min(1, lum)), shadow = (1 - l) * (1 - l), high = l * l;
        double gain =
                Math.pow(
                        2,
                        weight
                                * (a.exposure()
                                        + a.shadows() / 100 * shadow
                                        + a.highlights() / 100 * high));
        r *= gain;
        g *= gain;
        b *= gain;
        double contrast = Math.pow(2, a.contrast() / 100 * weight);
        r = (r - .18) * contrast + .18;
        g = (g - .18) * contrast + .18;
        b = (b - .18) * contrast + .18;
        double offset = weight * (a.whites() / 100 * high * .25 + a.blacks() / 100 * shadow * .1);
        r += offset;
        g += offset;
        b += offset;
        double max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b));
        double sat =
                Math.max(
                        0,
                        1
                                + weight
                                        * (a.saturation() / 100
                                                + a.vibrance()
                                                        / 100
                                                        * (1 - (max - min) / Math.max(.001, max))));
        double grey = .2627 * r + .6780 * g + .0593 * b;
        p[i] = (float) (grey + (r - grey) * sat);
        p[i + 1] = (float) (grey + (g - grey) * sat);
        p[i + 2] = (float) (grey + (b - grey) * sat);
    }

    static float sample(float[] data, int w, int h, double x, double y, int c) {
        if (x < -.5 || y < -.5 || x > w - .5 || y > h - .5) return 0;
        x = Math.max(0, Math.min(w - 1, x));
        y = Math.max(0, Math.min(h - 1, y));
        int x0 = (int) x, y0 = (int) y, x1 = Math.min(w - 1, x0 + 1), y1 = Math.min(h - 1, y0 + 1);
        double fx = x - x0, fy = y - y0;
        return (float)
                ((data[(y0 * w + x0) * 3 + c] * (1 - fx) + data[(y0 * w + x1) * 3 + c] * fx)
                                * (1 - fy)
                        + (data[(y1 * w + x0) * 3 + c] * (1 - fx)
                                        + data[(y1 * w + x1) * 3 + c] * fx)
                                * fy);
    }
}
