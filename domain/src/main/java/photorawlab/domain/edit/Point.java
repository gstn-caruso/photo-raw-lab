package photorawlab.domain.edit;

public record Point(double x, double y) {
    public Point {
        if (!Double.isFinite(x) || !Double.isFinite(y))
            throw new IllegalArgumentException("Coordenada inválida");
    }
}
