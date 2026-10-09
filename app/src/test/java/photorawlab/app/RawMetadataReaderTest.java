package photorawlab.app;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RawMetadataReaderTest {
    @TempDir Path directory;

    @Test void readsCaptureAndOpticalMetadataFromAnActualExifTiffFile() throws Exception {
        Path raw = Files.write(directory.resolve("camera.dng"), exifTiff());
        var metadata = new RawMetadataReader().read(raw);
        assertEquals(LocalDateTime.of(2024, 5, 6, 12, 30, 45), metadata.captured().orElseThrow());
        assertEquals("Test Camera", metadata.camera().orElseThrow());
        assertEquals("Prime 50mm", metadata.lens().orElseThrow());
        assertEquals(400, metadata.iso().orElseThrow());
        assertEquals(2.8, metadata.aperture().orElseThrow(), 0.001);
        assertEquals(0.008, metadata.exposureSeconds().orElseThrow(), 0.0001);
        assertEquals(50, metadata.focalLength().orElseThrow());
        assertEquals(6000, metadata.width().orElseThrow());
        assertEquals(4000, metadata.height().orElseThrow());
        assertEquals(metadata, new RawDirectory().snapshot(directory).getFirst().photo().metadata());
    }

    // A TIFF IFD0 points to an Exif SubIFD; there is deliberately no preview image.
    static byte[] exifTiff() {
        ByteBuffer bytes = ByteBuffer.allocate(400).order(ByteOrder.LITTLE_ENDIAN);
        bytes.putShort((short) 0x4949).putShort((short) 42).putInt(8);
        bytes.putShort((short) 2);
        entry(bytes, 0x0110, 2, 12, 180);
        entry(bytes, 0x8769, 4, 1, 40);
        bytes.putInt(0);
        bytes.position(40).putShort((short) 8);
        entry(bytes, 0x9003, 2, 20, 200);
        entry(bytes, 0xa434, 2, 11, 230);
        entry(bytes, 0x8827, 3, 1, 400);
        entry(bytes, 0x829d, 5, 1, 250);
        entry(bytes, 0x829a, 5, 1, 258);
        entry(bytes, 0x920a, 5, 1, 266);
        entry(bytes, 0xa002, 4, 1, 6000);
        entry(bytes, 0xa003, 4, 1, 4000);
        bytes.putInt(0);
        bytes.position(180).put("Test Camera\0".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        bytes.position(200).put("2024:05:06 12:30:45\0".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        bytes.position(230).put("Prime 50mm\0".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        bytes.position(250).putInt(28).putInt(10).putInt(1).putInt(125).putInt(50).putInt(1);
        return bytes.array();
    }

    private static void entry(ByteBuffer bytes, int tag, int type, int count, int value) {
        bytes.putShort((short) tag).putShort((short) type).putInt(count).putInt(value);
    }
}
