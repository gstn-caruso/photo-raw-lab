package photorawlab.app;

import static org.junit.jupiter.api.Assertions.*;

import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDirectory;

import org.junit.jupiter.api.Test;

class SonyMetadataTest {
    @Test
    void skipsSensorSubIfdToReadCaptureExifInSonyArw() {
        var metadata = new Metadata();
        var sensor = new ExifSubIFDDirectory();
        sensor.setInt(256, 6048);
        metadata.addDirectory(sensor);
        var capture = new ExifSubIFDDirectory();
        capture.setString(ExifSubIFDDirectory.TAG_LENS_MODEL, "FE 28-60mm F4-5.6");
        capture.setDouble(ExifSubIFDDirectory.TAG_FOCAL_LENGTH, 42);
        metadata.addDirectory(capture);
        assertSame(capture, RawMetadataReader.captureDirectory(metadata));
    }
}
