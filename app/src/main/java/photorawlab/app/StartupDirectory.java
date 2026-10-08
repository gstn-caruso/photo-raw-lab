package photorawlab.app;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Supplier;

public final class StartupDirectory {
    private final LastDirectory memory;

    public StartupDirectory(LastDirectory memory) { this.memory = memory; }

    public Optional<Path> select(Supplier<Optional<Path>> choose) {
        Optional<Path> saved = memory.load().filter(Files::isDirectory);
        if (saved.isPresent()) return saved;
        Optional<Path> selected = choose.get().filter(Files::isDirectory);
        selected.ifPresent(memory::save);
        return selected;
    }
}
