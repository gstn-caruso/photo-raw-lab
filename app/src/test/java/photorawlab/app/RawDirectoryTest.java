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
}
