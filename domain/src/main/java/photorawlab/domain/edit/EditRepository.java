package photorawlab.domain.edit;

import java.io.IOException;
import java.nio.file.Path;

public interface EditRepository {
    record Saved(Recipe recipe, String originalHash, String revision) {}

    Saved load(Path original) throws IOException;

    Saved save(Path original, Recipe recipe, Saved expected) throws IOException;
}
