package photorawlab.app;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RawDirectoryTest {
    @TempDir Path directory;

    @Test void listsOnlyImmediateRawFilesInStableOrder() throws Exception {
        Path second = Files.createFile(directory.resolve("b.NEF"));
        Path first = Files.createFile(directory.resolve("a.kdc"));
        Files.createFile(directory.resolve("photo.jpg"));
        Files.createFile(directory.resolve("notes"));
        Files.createDirectories(directory.resolve("nested.raw"));
        Files.createFile(directory.resolve("nested.raw/hidden.dng"));
        assertEquals(java.util.List.of(first, second), new RawDirectory().files(directory));
    }

    @Test void emptyDirectoryHasNoPhotosAndMissingDirectoryFails() throws Exception {
        assertTrue(new RawDirectory().files(directory).isEmpty());
        assertThrows(java.io.IOException.class, () -> new RawDirectory().files(directory.resolve("missing")));
    }

    @Test void capturesFileMetadataForSortingWithoutReadingImageContent() throws Exception {
        Path photo = Files.write(directory.resolve("photo.raw"), new byte[17]);
        var modified = java.time.Instant.ofEpochSecond(1234567);
        Files.setLastModifiedTime(photo, java.nio.file.attribute.FileTime.from(modified));
        var entries = new RawDirectory().snapshot(directory);
        assertEquals(1, entries.size());
        assertEquals(photo, entries.getFirst().path());
        assertEquals(new photorawlab.domain.GalleryPhoto("photo.raw", modified, 17), entries.getFirst().photo());
    }

    @Test void snapshotSkipsBrokenLinksAndDirectoriesButIncludesValidFileLinks() throws Exception {
        Path good = Files.write(directory.resolve("good.raw"), new byte[7]);
        Path linked = Files.createSymbolicLink(directory.resolve("linked.raw"), good);
        Files.createSymbolicLink(directory.resolve("broken.raw"), directory.resolve("missing.raw"));
        Files.createDirectory(directory.resolve("subdir.raw"));
        var listing = new RawDirectory();
        assertEquals(java.util.List.of(good, linked), listing.files(directory));
        var snapshot = listing.snapshot(directory);
        assertEquals(java.util.List.of(good, linked), snapshot.stream().map(RawDirectory.Entry::path).toList());
        assertEquals(7, snapshot.getLast().photo().bytes());
        assertThrows(java.io.IOException.class, () -> listing.snapshot(directory.resolve("absent")));
    }
}
