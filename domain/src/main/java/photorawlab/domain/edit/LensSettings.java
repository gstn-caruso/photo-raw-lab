package photorawlab.domain.edit;

import java.util.Objects;

public record LensSettings(
        Mode mode,
        LensProfile profile,
        boolean distortion,
        boolean chromatic,
        boolean vignetting,
        double focal,
        double aperture,
        double cameraCrop,
        boolean adapted) {
    public enum Mode {
        AUTO,
        MANUAL,
        OFF
    }

    public static final LensSettings DEFAULT =
            new LensSettings(Mode.AUTO, null, true, true, true, 0, 0, 1, false);

    public LensSettings {
        Objects.requireNonNull(mode);
        Adjustments.check(focal, 0, 10000);
        Adjustments.check(aperture, 0, 256);
        Adjustments.check(cameraCrop, .1, 100);
    }

    public LensSettings automatic() {
        return new LensSettings(
                Mode.AUTO,
                null,
                distortion,
                chromatic,
                vignetting,
                focal,
                aperture,
                cameraCrop,
                adapted);
    }
}
