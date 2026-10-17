package photorawlab.domain.edit;

import java.util.Objects;

/** The XML snapshot is opaque calibration data, never a pointer into the live database. */
public record LensProfile(
        String maker, String model, String variant, String calibration, double cropFactor) {
    public LensProfile {
        Objects.requireNonNull(maker);
        Objects.requireNonNull(model);
        Objects.requireNonNull(variant);
        Objects.requireNonNull(calibration);
        Adjustments.check(cropFactor, .1, 100);
    }

    @Override
    public String toString() {
        return variant;
    }
}
