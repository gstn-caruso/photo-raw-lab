package photorawlab.domain;

import java.io.IOException;
import java.nio.file.Path;

public interface RawImageDecoder {
    RgbImage decode(Path path) throws IOException;
}
