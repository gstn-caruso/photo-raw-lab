package photorawlab.domain.edit;

/** Crop is normalized in the rotated/straightened frame. Mapping uses original aspect ratio. */
public record Geometry(
        double x, double y, double width, double height, int quarterTurns, double angle) {
    public static final Geometry IDENTITY = new Geometry(0, 0, 1, 1, 0, 0);

    public Geometry {
        Adjustments.check(x, 0, 1);
        Adjustments.check(y, 0, 1);
        Adjustments.check(width, 0.001, 1);
        Adjustments.check(height, 0.001, 1);
        Adjustments.check(angle, -45, 45);
        if (x + width > 1.00000001
                || y + height > 1.00000001
                || quarterTurns < 0
                || quarterTurns > 3) throw new IllegalArgumentException("Recorte inválido");
    }

    public Point toOriginal(double u, double v, int originalWidth, int originalHeight) {
        double a = x + u * width, b = y + v * height;
        double rx, ry;
        switch (quarterTurns) {
            case 1 -> {
                rx = b;
                ry = 1 - a;
            }
            case 2 -> {
                rx = 1 - a;
                ry = 1 - b;
            }
            case 3 -> {
                rx = 1 - b;
                ry = a;
            }
            default -> {
                rx = a;
                ry = b;
            }
        }
        double px = (rx - .5) * originalWidth,
                py = (ry - .5) * originalHeight,
                r = Math.toRadians(angle);
        return new Point(
                (px * Math.cos(r) + py * Math.sin(r)) / originalWidth + .5,
                (-px * Math.sin(r) + py * Math.cos(r)) / originalHeight + .5);
    }
}
