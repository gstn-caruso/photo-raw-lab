package photorawlab.engine;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import photorawlab.domain.edit.*;

import java.util.List;

class MaskGeometryTest {
    @Test
    void paintedAreaFollowsRotationAndCrop() throws Exception {
        float[] rgb = new float[100 * 60 * 3];
        java.util.Arrays.fill(rgb, .1f);
        var image = new LinearImage(100, 60, rgb);
        var mask =
                new Mask(
                        "light",
                        false,
                        Adjustments.NEUTRAL.with(0, 1),
                        List.of(
                                new Mask.Stroke(
                                        Mask.Kind.BRUSH,
                                        false,
                                        .15,
                                        0,
                                        1,
                                        List.of(new Point(.25, .5)))));
        var r =
                Recipe.DEFAULT
                        .withMasks(List.of(mask))
                        .withGeometry(new Geometry(.1, 0, .8, 1, 1, 0));
        var out = FloatDevelopmentEngine.process(image, r, true, 100, 60);
        assertEquals(48, out.image().width());
        assertEquals(100, out.image().height());
        Point mapped = out.originalAt(.5, .25);
        assertEquals(.25, mapped.x(), .02);
        assertEquals(.5, mapped.y(), .02);
        int index = (25 * 48 + 24) * 3;
        assertEquals(.2, out.image().pixels()[index], 1e-5);
        assertEquals(.1, out.image().pixels()[(75 * 48 + 24) * 3], 1e-5);
    }
}
