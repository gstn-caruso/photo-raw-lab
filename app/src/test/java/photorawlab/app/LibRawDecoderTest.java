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
    void appliesCameraOrientationToEmbeddedBitmap() throws Exception {
        byte[] cameraFile = Files.readAllBytes(fixture());
        var tiff = java.nio.ByteBuffer.wrap(cameraFile).order(java.nio.ByteOrder.BIG_ENDIAN);
        assertEquals(0x0112, Short.toUnsignedInt(tiff.getShort(0x82)));
        assertEquals(0x0112, Short.toUnsignedInt(tiff.getShort(0x2c6)));
        tiff.putShort(0x8a, (short) 6);
        tiff.putShort(0x2ce, (short) 6);
        Path portrait = Files.write(directory.resolve("portrait.raw"), cameraFile);
        RgbImage preview = new LibRawDecoder().decodePreview(portrait);
        assertEquals(64, preview.width());
        assertEquals(96, preview.height());
    }

    @Test
    void extractsCameraThumbnailInsteadOfDevelopingFullRaw() throws Exception {
        LibRawDecoder decoder = new LibRawDecoder();
        RgbImage preview = decoder.decodePreview(fixture());
        assertTrue(preview.width() <= 256, "Expected an embedded thumbnail, got " + preview.width());
        assertTrue(preview.height() <= 256);
        assertTrue(Arrays.stream(preview.pixels()).distinct().limit(2).count() > 1);
        for (int attempt = 0; attempt < 3; attempt++) {
            assertArrayEquals(preview.pixels(), decoder.decodePreview(fixture()).pixels());
        }
    }

    @Test
    void extractsThumbnailEvenWhenRawSensorDataCannotBeDeveloped() throws Exception {
        byte[] cameraFile = Files.readAllBytes(fixture());
        Path thumbnailOnly = Files.write(directory.resolve("cámara sin sensor.raw"), Arrays.copyOf(cameraFile, 0x4d00));
        LibRawDecoder decoder = new LibRawDecoder();
        assertThrows(IOException.class, () -> decoder.decode(thumbnailOnly));
        assertArrayEquals(decoder.decodePreview(fixture()).pixels(), decoder.decodePreview(thumbnailOnly).pixels());
    }

    @Test
    void fallsBackToFullDevelopmentWhenCameraThumbnailIsMissingOrInvalid() throws Exception {
        byte[] cameraFile = Files.readAllBytes(fixture());
        var tiff = java.nio.ByteBuffer.wrap(cameraFile).order(java.nio.ByteOrder.BIG_ENDIAN);
        assertEquals(0x0111, Short.toUnsignedInt(tiff.getShort(0x76)));
        tiff.putInt(0x7e, 0);
        Path noThumbnail = Files.write(directory.resolve("no-thumbnail.raw"), cameraFile);
        LibRawDecoder decoder = new LibRawDecoder();
        RgbImage full = decoder.decode(noThumbnail);
        for (int attempt = 0; attempt < 3; attempt++) {
            assertArrayEquals(full.pixels(), decoder.decodePreview(noThumbnail).pixels());
        }
        tiff.putInt(0x7e, cameraFile.length - 1);
        Path invalidThumbnail = Files.write(directory.resolve("invalid-thumbnail.raw"), cameraFile);
        assertArrayEquals(full.pixels(), decoder.decodePreview(invalidThumbnail).pixels());
    }

    @Test
    void reportsCorruptPreviewFileWithOpeningContextAndNativeCause() throws Exception {
        Path corrupt = Files.writeString(directory.resolve("corrupt-preview-diagnostic.raw"), "not a raw photograph");
        assertPreviewOpeningFailure(corrupt);
    }

    @Test
    void reportsMissingPreviewFileWithOpeningContextAndNativeCause() {
        assertPreviewOpeningFailure(directory.resolve("missing-preview-diagnostic.raw"));
    }

    private void assertPreviewOpeningFailure(Path path) {
        IOException failure = assertThrows(IOException.class, () -> new LibRawDecoder().decodePreview(path));
        assertAll(
                () -> assertTrue(failure.getMessage().startsWith("Cannot decode preview " + path + ": LibRaw open_file failed"),
                        failure.getMessage()),
                () -> {
                    IOException cause = assertInstanceOf(IOException.class, failure.getCause());
                    assertTrue(cause.getMessage().startsWith("LibRaw open_file failed"), cause.getMessage());
                });
    }

    @Test
    void previewErrorsAreRecoverableAndLeaveDecoderReusable() throws Exception {
        LibRawDecoder decoder = new LibRawDecoder();
        Path corrupt = Files.writeString(directory.resolve("corrupt-preview.raw"), "not a raw photograph");
        assertThrows(IOException.class, () -> decoder.decodePreview(corrupt));
        assertThrows(IOException.class, () -> decoder.decodePreview(directory.resolve("missing.raw")));
        assertThrows(IOException.class, () -> new LibRawDecoder(directory.resolve("missing.so")).decodePreview(fixture()));
        assertTrue(decoder.decodePreview(fixture()).pixels().length > 0);
    }

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
