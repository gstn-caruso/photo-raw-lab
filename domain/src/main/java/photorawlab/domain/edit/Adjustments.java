package photorawlab.domain.edit;

/** Neutral is camera white balance; temperature/tint are relative mired/green shifts. */
public record Adjustments(
        double exposure,
        double temperature,
        double tint,
        double contrast,
        double highlights,
        double shadows,
        double whites,
        double blacks,
        double saturation,
        double vibrance) {
    public static final Adjustments NEUTRAL = new Adjustments(0, 0, 0, 0, 0, 0, 0, 0, 0, 0);

    public Adjustments {
        check(exposure, -5, 5);
        check(temperature, -100, 100);
        check(tint, -100, 100);
        for (double v :
                new double[] {contrast, highlights, shadows, whites, blacks, saturation, vibrance})
            check(v, -100, 100);
    }

    static void check(double value, double min, double max) {
        if (!Double.isFinite(value) || value < min || value > max)
            throw new IllegalArgumentException("Ajuste fuera de rango");
    }

    public Adjustments with(int index, double value) {
        double[] a = {
            exposure,
            temperature,
            tint,
            contrast,
            highlights,
            shadows,
            whites,
            blacks,
            saturation,
            vibrance
        };
        a[index] = value;
        return new Adjustments(a[0], a[1], a[2], a[3], a[4], a[5], a[6], a[7], a[8], a[9]);
    }

    public double value(int i) {
        return new double[] {
                    exposure,
                    temperature,
                    tint,
                    contrast,
                    highlights,
                    shadows,
                    whites,
                    blacks,
                    saturation,
                    vibrance
                }
                [i];
    }
}
