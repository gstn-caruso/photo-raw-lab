package photorawlab.domain;

import java.util.Comparator;
import java.util.Locale;

public record GallerySort(Criterion criterion, Direction direction) {
    public enum Criterion {
        NAME(Comparator.comparing((GalleryPhoto photo) -> photo.filename().toLowerCase(Locale.ROOT))
                .thenComparing(GalleryPhoto::filename)),
        MODIFIED(Comparator.comparing(GalleryPhoto::modified)),
        SIZE(Comparator.comparingLong(GalleryPhoto::bytes));

        private final Comparator<GalleryPhoto> comparator;

        Criterion(Comparator<GalleryPhoto> comparator) { this.comparator = comparator; }
    }

    public enum Direction { ASCENDING, DESCENDING }

    public Comparator<GalleryPhoto> comparator() {
        var primary = criterion.comparator;
        return direction == Direction.ASCENDING ? primary : primary.reversed();
    }
}
