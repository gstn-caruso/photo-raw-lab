package photorawlab.domain;

import java.time.Instant;

public record GalleryPhoto(String filename, Instant modified, long bytes, PhotoMetadata metadata) {
    public GalleryPhoto(String filename, Instant modified, long bytes) {
        this(filename, modified, bytes, PhotoMetadata.unknown());
    }
}
