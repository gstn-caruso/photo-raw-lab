package photorawlab.app;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import photorawlab.domain.RgbImage;

import static org.junit.jupiter.api.Assertions.*;

class LibRawDecoderTest {
    @TempDir
    Path directory;

    @Test
    void developsCameraRawByContentWithUnicodeAndSpaces() throws Exception {
        Path renamed = directory.resolve("cámara con espacios.raw");
        Files.copy(fixture(), renamed);
        LibRawDecoder decoder = new LibRawDecoder();
        RgbImage image = decoder.decode(renamed);
        assertTrue(image.width() > 256);
        assertTrue(image.height() > 256);
        assertEquals((long) image.width() * image.height(), image.pixels().length);
        assertTrue(Arrays.stream(image.pixels()).distinct().limit(2).count() > 1);
        for (int attempt = 0; attempt < 3; attempt++) {
            RgbImage repeated = decoder.decode(renamed);
            assertEquals(image.width(), repeated.width());
            assertEquals(image.height(), repeated.height());
            assertArrayEquals(image.pixels(), repeated.pixels());
        }
    }

    @Test
    void reportsNativeFailuresAndCanDecodeAgain() throws Exception {
        LibRawDecoder decoder = new LibRawDecoder();
        Path corrupt = Files.writeString(directory.resolve("corrupt.raw"), "not a raw photograph");
        IOException corruptError = assertThrows(IOException.class, () -> decoder.decode(corrupt));
        assertTrue(corruptError.getMessage().contains("open"));
        IOException missingError = assertThrows(IOException.class, () -> decoder.decode(directory.resolve("missing.raw")));
        assertTrue(missingError.getMessage().contains("open"));
        assertTrue(decoder.decode(fixture()).pixels().length > 0);
    }

    @Test
    void reportsMissingNativeLibraryAsRecoverableIoFailure() throws Exception {
        LibRawDecoder decoder = new LibRawDecoder(directory.resolve("missing-libraw.so"));
        IOException failure = assertThrows(IOException.class, () -> decoder.decode(fixture()));
        assertTrue(failure.getMessage().contains("LibRaw"));
        assertTrue(failure.getMessage().contains("missing-libraw.so"));
    }

    @Test
    void acceptsAnExplicitNativeLibrary() throws Exception {
        assertTrue(new LibRawDecoder(Path.of("/usr/lib/x86_64-linux-gnu/libraw.so.23"))
                .decode(fixture()).pixels().length > 0);
    }

    @Test
    void survivesTerminatingBackgroundThreadsAcrossDecoderInstances() throws Exception {
        Path source = fixture();
        RgbImage first = null;
        for (int attempt = 0; attempt < 4; attempt++) {
            LibRawDecoder decoder = new LibRawDecoder();
            try (var executor = Executors.newSingleThreadExecutor()) {
                RgbImage image = executor.submit(() -> decoder.decode(source)).get(10, TimeUnit.SECONDS);
                if (first == null) {
                    first = image;
                } else {
                    assertArrayEquals(first.pixels(), image.pixels());
                }
            }
        }
        assertNotNull(first);
    }

    private Path fixture() throws Exception {
        return Path.of(getClass().getResource("/raw/kodak-dc50.kdc").toURI());
    }
}
