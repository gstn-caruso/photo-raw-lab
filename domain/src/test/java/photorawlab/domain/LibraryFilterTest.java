package photorawlab.domain;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.Test;

class LibraryFilterTest {
    @Test void emptyLibraryHasNoMatchesAndNoFacetCounts() {
        var filter = new LibraryFilter();
        assertEquals(List.of(), filter.apply(List.of()));
        for (var facet : LibraryFilter.Facet.values()) assertTrue(filter.counts(List.of(), facet).isEmpty());
        assertEquals(PhotoMetadata.unknown(), new GalleryPhoto("a.raw", java.time.Instant.EPOCH, 0).metadata());
    }
}
