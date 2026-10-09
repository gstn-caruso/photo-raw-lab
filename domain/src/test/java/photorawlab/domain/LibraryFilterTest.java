package photorawlab.domain;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class LibraryFilterTest {
    @Test void selectionsCombineAndFacetCountsIgnoreOnlyTheirOwnSelection() {
        var alpha = photo("a.dng", "Camera A", "Prime");
        var beta = photo("b.cr2", "Camera A", "Zoom");
        var gamma = photo("c.dng", "Camera B", "Zoom");
        var unknown = new GalleryPhoto("d.raw", java.time.Instant.EPOCH, 0);
        var photos = List.of(alpha, beta, gamma, unknown);
        var filter = new LibraryFilter();
        assertEquals(1L, filter.counts(photos, LibraryFilter.Facet.CAMERA).get(LibraryFilter.Value.unknown()));
        filter.select(LibraryFilter.Facet.CAMERA, LibraryFilter.Value.known("Camera A"));
        assertEquals(List.of(alpha, beta), filter.apply(photos));
        assertEquals(java.util.Map.of(LibraryFilter.Value.known("Prime"), 1L, LibraryFilter.Value.known("Zoom"), 1L), filter.counts(photos, LibraryFilter.Facet.LENS));
        filter.select(LibraryFilter.Facet.LENS, LibraryFilter.Value.known("Zoom"));
        assertEquals(List.of(beta), filter.apply(photos));
        assertEquals(java.util.Map.of(LibraryFilter.Value.known("Camera A"), 1L, LibraryFilter.Value.known("Camera B"), 1L), filter.counts(photos, LibraryFilter.Facet.CAMERA));
        filter.select(LibraryFilter.Facet.FILE_TYPE, LibraryFilter.Value.known("DNG"));
        assertTrue(filter.apply(photos).isEmpty());
        assertEquals(1L, filter.counts(photos, LibraryFilter.Facet.CAMERA).get(LibraryFilter.Value.known("Camera B")));
        filter.clear();
        filter.select(LibraryFilter.Facet.CAMERA, LibraryFilter.Value.unknown());
        assertEquals(List.of(unknown), filter.apply(photos));
        filter.clear();
        assertEquals(photos, filter.apply(photos));
    }

    private GalleryPhoto photo(String filename, String camera, String lens) {
        var missing = PhotoMetadata.unknown();
        return new GalleryPhoto(filename, java.time.Instant.EPOCH, 0, new PhotoMetadata(
                Optional.of(java.time.LocalDateTime.of(2024, 5, 6, 12, 0)), Optional.of(camera), Optional.of(lens),
                missing.iso(), missing.aperture(), missing.exposureSeconds(), missing.focalLength(), missing.width(), missing.height()));
    }
    @Test void emptyLibraryHasNoMatchesAndNoFacetCounts() {
        var filter = new LibraryFilter();
        assertEquals(List.of(), filter.apply(List.of()));
        for (var facet : LibraryFilter.Facet.values()) assertTrue(filter.counts(List.of(), facet).isEmpty());
        assertEquals(PhotoMetadata.unknown(), new GalleryPhoto("a.raw", java.time.Instant.EPOCH, 0).metadata());
    }
}
