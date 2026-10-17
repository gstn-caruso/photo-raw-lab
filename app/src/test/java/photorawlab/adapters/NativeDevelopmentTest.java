package photorawlab.adapters;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import photorawlab.domain.edit.*;
import photorawlab.engine.FloatDevelopmentEngine;

import java.nio.file.*;
import java.util.*;

class NativeDevelopmentTest {
    @TempDir Path dir;

    @Test
    void realRawIsLinearHighPrecisionAndReproducible() throws Exception {
        Path fixture = Path.of("src/test/resources/raw/kodak-dc50.kdc");
        String hash = JsonEditRepository.hash(fixture);
        var decoder = new LinearRawDecoder();
        var image = decoder.decode(fixture, Adjustments.NEUTRAL);
        assertTrue(image.width() > 100);
        assertTrue(image.height() > 100);
        Set<Float> values = new HashSet<>();
        for (float v : image.pixels()) {
            assertTrue(Float.isFinite(v));
            values.add(v);
        }
        assertTrue(values.size() > 256);
        assertArrayEquals(image.pixels(), decoder.decode(fixture, Adjustments.NEUTRAL).pixels());
        assertEquals(hash, JsonEditRepository.hash(fixture));
        var preview = new FloatDevelopmentEngine().render(fixture, Recipe.DEFAULT, 128);
        assertEquals(128, Math.max(preview.width(), preview.height()));
    }

    @Test
    void floatExposureRetainsHighlightsAndLcmsConvertsNeutralRamp() throws Exception {
        var source =
                new LinearImage(3, 1, new float[] {.1f, .1f, .1f, .5f, .5f, .5f, .8f, .8f, .8f});
        var out =
                FloatDevelopmentEngine.process(
                                source,
                                Recipe.DEFAULT.withAdjustments(Adjustments.NEUTRAL.with(0, 1)),
                                true,
                                3,
                                1)
                        .image();
        assertEquals(1.6, out.pixels()[6], 1e-6);
        assertEquals(.2, out.pixels()[0], 1e-6);
        short[] srgb = LinearRawDecoder.srgb(source);
        assertEquals(48192, Short.toUnsignedInt(srgb[3]), 60);
        assertEquals(Short.toUnsignedInt(srgb[3]), Short.toUnsignedInt(srgb[4]), 3);
    }

    @Test
    void nativeCatalogContainsRealManufacturersSearchAndSavedCalibration() throws Exception {
        var db = new LensfunCatalog();
        assertTrue(
                db.manufacturers()
                        .containsAll(
                                List.of("Sony", "Canon", "Nikon", "Sigma", "Tamron", "Fujifilm")));
        var profiles = db.search("Sony", "28-70", "ILCE-7M3", false);
        assertFalse(profiles.isEmpty());
        var p = profiles.getFirst();
        assertTrue(p.calibration().contains("distortion"));
        var s = new LensSettings(LensSettings.Mode.MANUAL, p, true, true, true, 28, 5.6, 1, false);
        assertTrue(db.available(s).distortion());
        assertFalse(
                db.available(new LensSettings(s.mode(), p, true, true, true, 0, 0, 1, false))
                        .distortion());
        try (Optics o = new Optics(s, 100, 80)) {
            float[] row = o.row(0);
            assertEquals(600, row.length);
            assertNotEquals(0, row[0]);
        }
        assertTrue(db.automatic("ILCE-7M3", "FE").size() > 1);
        assertTrue(db.automatic("", p.model()).isEmpty());
        assertTrue(db.search("Sony", "28-70", "UNKNOWN", false).isEmpty());
        assertFalse(db.search("Sony", "28-70", "UNKNOWN", true).isEmpty());
        var repo = new JsonEditRepository();
        Path raw = Files.writeString(dir.resolve("sony.ARW"), "raw");
        repo.save(raw, Recipe.DEFAULT.withLens(s), repo.load(raw));
        var reopened = repo.load(raw).recipe().lens();
        assertEquals(s, reopened);
        try (Optics o = new Optics(reopened, 100, 80)) {
            assertTrue((o.flags() & 1) != 0);
        }
    }

    @Test
    void partialCalibrationDisablesUnavailableCorrections() throws Exception {
        String xml =
                "<lensdatabase"
                    + " version=\"1\"><lens><maker>Test</maker><model>Partial</model><mount>Sony"
                    + " E</mount><cropfactor>1</cropfactor><calibration><distortion model=\"poly3\""
                    + " focal=\"28\" k1=\"-0.02\"/></calibration></lens></lensdatabase>";
        var profile = new LensProfile("Test", "Partial", "v1", xml, 1);
        var available =
                new LensfunCatalog()
                        .available(
                                new LensSettings(
                                        LensSettings.Mode.MANUAL,
                                        profile,
                                        true,
                                        true,
                                        true,
                                        28,
                                        5.6,
                                        1,
                                        false));
        assertTrue(available.distortion());
        assertFalse(available.chromatic());
        assertFalse(available.vignetting());
    }

    @Test
    void tiffIs16BitProfiledAndExportsNeverOverwrite() throws Exception {
        var image =
                new LinearImage(
                        2, 2, new float[] {.1f, .2f, .3f, .4f, .5f, .6f, .7f, .8f, .9f, 1, 1, 1});
        var writer = new ImageOutput();
        Path first = writer.write(image, dir, "test", ImageOutput.Format.TIFF, () -> false),
                second = writer.write(image, dir, "test", ImageOutput.Format.TIFF, () -> false);
        assertNotEquals(first, second);
        var read = javax.imageio.ImageIO.read(first.toFile());
        assertArrayEquals(new int[] {16, 16, 16}, read.getColorModel().getComponentSize());
        assertEquals(2, read.getWidth());
        try (var in = javax.imageio.ImageIO.createImageInputStream(first.toFile())) {
            var reader = javax.imageio.ImageIO.getImageReaders(in).next();
            try {
                reader.setInput(in);
                var tags =
                        javax.imageio.plugins.tiff.TIFFDirectory.createFromMetadata(
                                reader.getImageMetadata(0));
                assertNotNull(tags.getTIFFField(34675));
            } finally {
                reader.dispose();
            }
        }
        Path jpeg = writer.write(image, dir, "test", ImageOutput.Format.JPEG, () -> false);
        assertNotNull(javax.imageio.ImageIO.read(jpeg.toFile()));
        assertThrows(
                java.io.IOException.class,
                () -> writer.write(image, dir, "cancel", ImageOutput.Format.TIFF, () -> true));
        try (var files = Files.list(dir)) {
            assertFalse(files.anyMatch(p -> p.toString().endsWith(".tmp")));
        }
    }
}
