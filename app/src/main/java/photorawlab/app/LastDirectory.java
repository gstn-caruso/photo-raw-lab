package photorawlab.app;

import java.nio.file.Path;
import java.util.Optional;

public interface LastDirectory {
    Optional<Path> load();
    void save(Path directory);
}
