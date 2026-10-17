package photorawlab.domain.edit;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.util.List;

class RecipeTest {
    @Test
    void oneGestureIsOneUndoAndNewGestureDropsRedo() {
        var h = new EditHistory(Recipe.DEFAULT);
        var r = Recipe.DEFAULT.withAdjustments(Adjustments.NEUTRAL.with(0, 2));
        h.commit(r);
        h.commit(r);
        assertEquals(Recipe.DEFAULT, h.undo());
        assertEquals(r, h.redo());
        h.undo();
        h.commit(Recipe.DEFAULT.withGeometry(new Geometry(0, 0, 1, 1, 1, 0)));
        assertEquals(1, h.redo().geometry().quarterTurns());
    }

    @Test
    void maskCombinesSegmentsErasureInversionAndGradients() {
        var stroke =
                new Mask.Stroke(
                        Mask.Kind.BRUSH,
                        false,
                        .1,
                        .5,
                        1,
                        List.of(new Point(.2, .5), new Point(.8, .5)));
        var m = new Mask("paint", false, Adjustments.NEUTRAL, List.of(stroke));
        assertEquals(1, m.weight(.5, .5, 1));
        assertEquals(0, m.weight(.5, .8, 1));
        var erase = new Mask.Stroke(Mask.Kind.BRUSH, true, .1, 0, 1, List.of(new Point(.5, .5)));
        assertEquals(0, m.append(erase).weight(.5, .5, 1));
        assertEquals(
                1,
                new Mask("inverse", true, Adjustments.NEUTRAL, List.of(stroke)).weight(.1, .1, 1));
        var linear =
                new Mask.Stroke(
                        Mask.Kind.LINEAR,
                        false,
                        .1,
                        .5,
                        1,
                        List.of(new Point(0, 0), new Point(1, 0)));
        assertEquals(
                .5,
                new Mask("linear", false, Adjustments.NEUTRAL, List.of(linear)).weight(.5, .2, 1),
                1e-8);
    }

    @Test
    void cropAndRotationMapToOrientedOriginal() {
        var g = new Geometry(.25, 0, .5, 1, 1, 0);
        assertEquals(new Point(.25, .75), g.toOriginal(0, .25, 600, 400));
    }

    @Test
    void rejectsInvalidNumbersAndFutureRecipes() {
        assertThrows(IllegalArgumentException.class, () -> Adjustments.NEUTRAL.with(0, Double.NaN));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new Recipe(
                                2,
                                Adjustments.NEUTRAL,
                                Geometry.IDENTITY,
                                LensSettings.DEFAULT,
                                List.of()));
        assertThrows(IllegalArgumentException.class, () -> new Geometry(.8, 0, .5, 1, 0, 0));
    }
}
