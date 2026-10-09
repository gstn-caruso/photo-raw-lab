package photorawlab.domain;

import java.util.List;
import java.util.Map;

public final class LibraryFilter {
    public enum Facet { CAPTURE_DATE, CAMERA, LENS, FILE_TYPE }
    public List<GalleryPhoto> apply(List<GalleryPhoto> photos) { return List.copyOf(photos); }
    public Map<String, Long> counts(List<GalleryPhoto> photos, Facet facet) { return Map.of(); }
}
