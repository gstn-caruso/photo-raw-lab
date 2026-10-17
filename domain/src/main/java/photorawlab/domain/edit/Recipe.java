package photorawlab.domain.edit;

import java.util.List;
import java.util.Objects;

public record Recipe(
        int version,
        Adjustments adjustments,
        Geometry geometry,
        LensSettings lens,
        List<Mask> masks) {
    public static final Recipe DEFAULT =
            new Recipe(1, Adjustments.NEUTRAL, Geometry.IDENTITY, LensSettings.DEFAULT, List.of());

    public Recipe {
        if (version != 1)
            throw new IllegalArgumentException("Versión de receta no compatible: " + version);
        Objects.requireNonNull(adjustments);
        Objects.requireNonNull(geometry);
        Objects.requireNonNull(lens);
        masks = List.copyOf(masks);
        if (masks.size() > 100) throw new IllegalArgumentException("Demasiadas máscaras");
    }

    public Recipe withAdjustments(Adjustments a) {
        return new Recipe(version, a, geometry, lens, masks);
    }

    public Recipe withGeometry(Geometry g) {
        return new Recipe(version, adjustments, g, lens, masks);
    }

    public Recipe withLens(LensSettings l) {
        return new Recipe(version, adjustments, geometry, l, masks);
    }

    public Recipe withMasks(List<Mask> m) {
        return new Recipe(version, adjustments, geometry, lens, m);
    }
}
