package photorawlab.domain.edit;

import java.io.IOException;
import java.util.List;

public interface LensCatalog {
    record Availability(boolean distortion, boolean chromatic, boolean vignetting) {}

    List<String> manufacturers();

    List<LensProfile> search(String maker, String query, String camera, boolean adapted)
            throws IOException;

    List<LensProfile> automatic(String camera, String lens) throws IOException;

    Availability available(LensSettings settings) throws IOException;
}
