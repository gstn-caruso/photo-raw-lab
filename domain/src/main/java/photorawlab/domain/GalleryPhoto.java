package photorawlab.domain;

import java.time.Instant;

public record GalleryPhoto(String filename, Instant modified, long bytes) {}
