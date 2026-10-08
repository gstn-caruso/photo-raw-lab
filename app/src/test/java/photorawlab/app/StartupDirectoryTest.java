package photorawlab.app;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Path;
import java.util.Optional;
import java.util.prefs.Preferences;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class StartupDirectoryTest {
    @TempDir Path directory;

    @Test void remembersSelectedDirectoryAndReusesItWithoutPrompt() throws Exception {
        Preferences node = Preferences.userRoot().node("photo-raw-test-" + java.util.UUID.randomUUID());
        try {
            LastDirectory memory = new PreferencesLastDirectory(node);
            assertEquals(Optional.of(directory), new StartupDirectory(memory).select(() -> Optional.of(directory)));
            assertEquals(Optional.of(directory), new StartupDirectory(new PreferencesLastDirectory(node)).select(() -> {
                fail("Existing remembered directory should bypass chooser");
                return Optional.empty();
            }));
        } finally { node.removeNode(); }
    }

    @Test void missingSavedDirectoryPromptsAndCancellationDoesNotReplaceMemory() {
        LastDirectory memory = new LastDirectory() {
            public Optional<Path> load() { return Optional.of(directory.resolve("missing")); }
            public void save(Path path) { fail("Cancel must not save"); }
        };
        assertEquals(Optional.empty(), new StartupDirectory(memory).select(Optional::empty));
    }
}
