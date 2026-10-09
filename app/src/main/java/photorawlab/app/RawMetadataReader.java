package photorawlab.app;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Optional;
import photorawlab.domain.PhotoMetadata;

public final class RawMetadataReader {
    private static final DateTimeFormatter EXIF_DATE = DateTimeFormatter.ofPattern("uuuu:MM:dd HH:mm:ss")
            .withResolverStyle(ResolverStyle.STRICT);

    public PhotoMetadata read(Path path) {
        try {
            var metadata = ImageMetadataReader.readMetadata(path.toFile());
            var camera = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
            var exif = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
            return new PhotoMetadata(capture(exif), text(camera, ExifIFD0Directory.TAG_MODEL),
                    text(exif, ExifSubIFDDirectory.TAG_LENS_MODEL), integer(exif, ExifSubIFDDirectory.TAG_ISO_EQUIVALENT),
                    number(exif, ExifSubIFDDirectory.TAG_FNUMBER), number(exif, ExifSubIFDDirectory.TAG_EXPOSURE_TIME),
                    number(exif, ExifSubIFDDirectory.TAG_FOCAL_LENGTH),
                    integer(exif, ExifSubIFDDirectory.TAG_EXIF_IMAGE_WIDTH),
                    integer(exif, ExifSubIFDDirectory.TAG_EXIF_IMAGE_HEIGHT));
        } catch (IOException | ImageProcessingException unavailable) {
            return PhotoMetadata.unknown();
        }
    }

    private Optional<LocalDateTime> capture(Directory exif) {
        return text(exif, ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL).flatMap(value -> {
            try { return Optional.of(LocalDateTime.parse(value, EXIF_DATE)); }
            catch (DateTimeParseException invalid) { return Optional.empty(); }
        });
    }

    private Optional<String> text(Directory directory, int tag) {
        return directory == null ? Optional.empty() : Optional.ofNullable(directory.getString(tag))
                .map(String::strip).filter(value -> !value.isEmpty());
    }

    private Optional<Integer> integer(Directory directory, int tag) {
        return directory == null ? Optional.empty() : Optional.ofNullable(directory.getInteger(tag))
                .filter(value -> value > 0);
    }

    private Optional<Double> number(Directory directory, int tag) {
        return directory == null ? Optional.empty() : Optional.ofNullable(directory.getDoubleObject(tag))
                .filter(value -> Double.isFinite(value) && value > 0);
    }
}
