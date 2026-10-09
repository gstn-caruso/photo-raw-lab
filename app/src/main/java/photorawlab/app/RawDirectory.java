package photorawlab.app;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.ArrayList;
import java.nio.file.attribute.BasicFileAttributes;
import photorawlab.domain.GalleryPhoto;

public final class RawDirectory {
    public record Entry(Path path, GalleryPhoto photo) {}
    private static final List<String> EXTENSIONS = List.of("raw", "cr2", "cr3", "crw", "nef", "nrw",
            "arw", "dng", "kdc", "dcr", "orf", "rw2", "raf", "pef", "srw", "3fr", "fff", "iiq");

    public static String[] extensions() { return EXTENSIONS.toArray(String[]::new); }

    public List<Path> files(Path directory) throws IOException {
        try (var entries = Files.list(directory)) {
            return entries.filter(Files::isRegularFile).filter(this::isRaw)
                    .sorted(Comparator.comparing(Path::toString)).toList();
        }
    }

    public List<Entry> snapshot(Path directory) throws IOException {
        List<Entry> photos = new ArrayList<>();
        try (var paths = Files.list(directory)) {
            for (Path path : paths.filter(this::isRaw).sorted().toList()) {
                BasicFileAttributes attributes;
                try {
                    attributes = Files.readAttributes(path, BasicFileAttributes.class);
                } catch (IOException unavailable) {
                    continue;
                }
                if (attributes.isRegularFile()) {
                    photos.add(new Entry(path, new GalleryPhoto(path.getFileName().toString(),
                            attributes.lastModifiedTime().toInstant(), attributes.size())));
                }
            }
        }
        return List.copyOf(photos);
    }

    private boolean isRaw(Path path) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        int dot = name.lastIndexOf('.');
        return dot >= 0 && EXTENSIONS.contains(name.substring(dot + 1));
    }
}
