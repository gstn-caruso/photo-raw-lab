package photorawlab.domain.edit;

import java.util.List;
import java.util.Objects;

public record Mask(String name, boolean inverted, Adjustments adjustments, List<Stroke> strokes) {
    public enum Kind {
        BRUSH,
        LINEAR,
        RADIAL
    }

    public record Stroke(
            Kind kind,
            boolean subtract,
            double radius,
            double softness,
            double opacity,
            List<Point> points) {
        public Stroke {
            Objects.requireNonNull(kind);
            points = List.copyOf(points);
            if (points.isEmpty() || points.size() > 100000)
                throw new IllegalArgumentException("Trazo vacío o demasiado largo");
            Adjustments.check(radius, .001, 1);
            Adjustments.check(softness, 0, 1);
            Adjustments.check(opacity, 0, 1);
        }

        double coverage(double x, double y, double aspect) {
            Point first = points.getFirst(), last = points.getLast();
            double dx = (last.x() - first.x()) * aspect, dy = last.y() - first.y();
            if (kind == Kind.LINEAR) {
                double length = dx * dx + dy * dy;
                double position =
                        length < 1e-12
                                ? 0
                                : ((x - first.x()) * aspect * dx + (y - first.y()) * dy) / length;
                return opacity * clamp((position - .5) / Math.max(.001, softness) + .5);
            }
            if (kind == Kind.RADIAL) {
                double rx = Math.max(.001, Math.abs(last.x() - first.x())),
                        ry = Math.max(.001, Math.abs(last.y() - first.y()));
                return opacity * feather(Math.hypot((x - first.x()) / rx, (y - first.y()) / ry));
            }
            double distance = Double.POSITIVE_INFINITY;
            Point previous = first;
            for (Point point : points) {
                double ax = (x - previous.x()) * aspect, ay = y - previous.y();
                double bx = (point.x() - previous.x()) * aspect, by = point.y() - previous.y();
                double t = clamp((ax * bx + ay * by) / Math.max(1e-20, bx * bx + by * by));
                distance = Math.min(distance, Math.hypot(ax - t * bx, ay - t * by));
                previous = point;
            }
            return opacity * feather(distance / radius);
        }

        private double feather(double distance) {
            return distance >= 1 ? 0 : softness == 0 ? 1 : clamp((1 - distance) / softness);
        }
    }

    public Mask {
        Objects.requireNonNull(name);
        Objects.requireNonNull(adjustments);
        strokes = List.copyOf(strokes);
        if (strokes.size() > 10000) throw new IllegalArgumentException("Demasiados trazos");
    }

    public double weight(double x, double y, double aspect) {
        double value = 0;
        for (Stroke stroke : strokes) {
            double c = stroke.coverage(x, y, aspect);
            value = stroke.subtract() ? value * (1 - c) : value + (1 - value) * c;
        }
        return inverted ? 1 - value : value;
    }

    public Mask append(Stroke s) {
        var copy = new java.util.ArrayList<>(strokes);
        copy.add(s);
        return new Mask(name, inverted, adjustments, copy);
    }

    private static double clamp(double v) {
        return Math.max(0, Math.min(1, v));
    }
}
