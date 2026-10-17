package photorawlab.adapters;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import photorawlab.domain.edit.*;

import java.nio.file.*;
import java.util.List;

class JsonEditRepositoryTest {
    @TempDir Path directory;

    @Test
    void restoresExactRecipeMasksAndPinnedLensWithoutChangingOriginal() throws Exception {
        Path raw = Files.writeString(directory.resolve("foto.ARW"), "original");
        String hash = JsonEditRepository.hash(raw);
        var repo = new JsonEditRepository();
        var loaded = repo.load(raw);
        var p =
                new LensProfile(
                        "Sony", "FE 28-70mm f/3.5-5.6 OSS", "calibration", "<lensdatabase/>", 1);
        var l = new LensSettings(LensSettings.Mode.MANUAL, p, true, false, true, 28, 5.6, 1, false);
        var mask =
                new Mask(
                        "pincel",
                        true,
                        Adjustments.NEUTRAL.with(0, 1),
                        List.of(
                                new Mask.Stroke(
                                        Mask.Kind.BRUSH,
                                        false,
                                        .03,
                                        .6,
                                        .5,
                                        List.of(new Point(.2, .4), new Point(.5, .8)))));
        var r =
                Recipe.DEFAULT
                        .withLens(l)
                        .withMasks(List.of(mask))
                        .withAdjustments(Adjustments.NEUTRAL.with(0, 1.25))
                        .withGeometry(new Geometry(.1, .2, .8, .7, 1, 2.5));
        repo.save(raw, r, loaded);
        assertEquals(r, new JsonEditRepository().load(raw).recipe());
        assertEquals(hash, JsonEditRepository.hash(raw));
        assertTrue(Files.exists(directory.resolve("foto.ARW.photoraw.json")));
        assertNull(l.automatic().profile());
    }

    @Test
    void detectsExternalEditsChangedOriginalAndTruncatedJson() throws Exception {
        Path raw = Files.writeString(directory.resolve("foto.ARW"), "original");
        var repo = new JsonEditRepository();
        var old = repo.load(raw);
        var saved = repo.save(raw, Recipe.DEFAULT, old);
        assertThrows(java.io.IOException.class, () -> repo.save(raw, Recipe.DEFAULT, old));
        Files.writeString(raw, "changed");
        assertThrows(java.io.IOException.class, () -> repo.load(raw));
        assertThrows(java.io.IOException.class, () -> repo.save(raw, Recipe.DEFAULT, saved));
        Files.writeString(raw, "original");
        Files.writeString(JsonEditRepository.sidecar(raw), "{broken");
        assertThrows(java.io.IOException.class, () -> repo.load(raw));
    }

    @Test
    void failedAtomicPublicationKeepsPreviousAndCleansTemporary() throws Exception {
        Path raw = Files.writeString(directory.resolve("file.ARW"), "raw");
        var repo = new JsonEditRepository();
        var loaded = repo.load(raw);
        Files.createDirectory(JsonEditRepository.sidecar(raw));
        assertThrows(java.io.IOException.class, () -> repo.save(raw, Recipe.DEFAULT, loaded));
        try (var files = Files.list(directory)) {
            assertFalse(files.anyMatch(p -> p.toString().endsWith(".tmp")));
        }
    }

    @Test
    void staleTemporaryAndReadOnlyDirectoryDoNotReplaceSavedRecipe() throws Exception {
        Path raw = Files.writeString(directory.resolve("safe.ARW"), "raw");
        var repo = new JsonEditRepository();
        var saved = repo.save(raw, Recipe.DEFAULT, repo.load(raw));
        Files.writeString(directory.resolve(".photoraw-interrupted.tmp"), "{partial");
        assertEquals(Recipe.DEFAULT, repo.load(raw).recipe());
        var permissions = Files.getPosixFilePermissions(directory);
        try {
            Files.setPosixFilePermissions(
                    directory,
                    java.nio.file.attribute.PosixFilePermissions.fromString("r-x------"));
            assertThrows(
                    java.io.IOException.class,
                    () ->
                            repo.save(
                                    raw,
                                    Recipe.DEFAULT.withAdjustments(
                                            photorawlab.domain.edit.Adjustments.NEUTRAL.with(0, 1)),
                                    saved));
            assertEquals(Recipe.DEFAULT, repo.load(raw).recipe());
        } finally {
            Files.setPosixFilePermissions(directory, permissions);
        }
    }
}
