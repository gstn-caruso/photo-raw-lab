package photorawlab.domain;

import static org.junit.jupiter.api.Assertions.*;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class GallerySortTest {
    private final GalleryPhoto alpha = new GalleryPhoto("a.raw", Instant.ofEpochSecond(30), 20);
    private final GalleryPhoto beta = new GalleryPhoto("B.raw", Instant.ofEpochSecond(10), 30);
    private final GalleryPhoto gamma = new GalleryPhoto("c.raw", Instant.ofEpochSecond(20), 10);

    @Test void sortsEachCriterionInBothDirections() {
        var input = List.of(gamma, alpha, beta);
        assertOrder(input, GallerySort.Criterion.NAME, List.of(alpha, beta, gamma));
        assertOrder(input, GallerySort.Criterion.MODIFIED, List.of(beta, gamma, alpha));
        assertOrder(input, GallerySort.Criterion.SIZE, List.of(gamma, alpha, beta));
    }

    @Test void tiesUseAscendingNamesInEitherDirectionRegardlessOfInputOrder() {
        var upper = new GalleryPhoto("A.raw", Instant.EPOCH, 10);
        var lower = new GalleryPhoto("a.raw", Instant.EPOCH, 10);
        var beta = new GalleryPhoto("b.raw", Instant.EPOCH, 10);
        for (var input : List.of(List.of(beta, lower, upper), List.of(lower, upper, beta))) {
            for (var criterion : List.of(GallerySort.Criterion.MODIFIED, GallerySort.Criterion.SIZE)) {
                for (var direction : GallerySort.Direction.values()) {
                    assertEquals(List.of(upper, lower, beta), input.stream().sorted(new GallerySort(criterion, direction).comparator()).toList());
                }
            }
        }
    }

    private void assertOrder(List<GalleryPhoto> input, GallerySort.Criterion criterion, List<GalleryPhoto> ascending) {
        assertEquals(ascending, input.stream().sorted(new GallerySort(criterion, GallerySort.Direction.ASCENDING).comparator()).toList());
        assertEquals(ascending.reversed(), input.stream().sorted(new GallerySort(criterion, GallerySort.Direction.DESCENDING).comparator()).toList());
    }
}
