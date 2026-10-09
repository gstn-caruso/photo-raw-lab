package photorawlab.domain;

import java.time.LocalDateTime;
import java.util.Optional;

public record PhotoMetadata(Optional<LocalDateTime> captured, Optional<String> camera,
        Optional<String> lens, Optional<Integer> iso, Optional<Double> aperture,
        Optional<Double> exposureSeconds, Optional<Double> focalLength,
        Optional<Integer> width, Optional<Integer> height) {
    public static PhotoMetadata unknown() {
        return new PhotoMetadata(Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty());
    }
}
