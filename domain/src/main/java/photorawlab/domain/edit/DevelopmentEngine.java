package photorawlab.domain.edit;

import java.io.IOException;
import java.nio.file.Path;

public interface DevelopmentEngine {
    LinearImage render(Path original, Recipe recipe, int maxSide) throws IOException;
}
