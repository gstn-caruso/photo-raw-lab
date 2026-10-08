package photorawlab.app;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.prefs.Preferences;

public final class PreferencesLastDirectory implements LastDirectory {
    private final Preferences preferences;

    public PreferencesLastDirectory(Preferences preferences) { this.preferences = preferences; }

    public Optional<Path> load() {
        String stored = preferences.get("lastDirectory", null);
        try { return stored == null ? Optional.empty() : Optional.of(Path.of(stored)); }
        catch (InvalidPathException error) { return Optional.empty(); }
    }

    public void save(Path directory) { preferences.put("lastDirectory", directory.toAbsolutePath().toString()); }
}
