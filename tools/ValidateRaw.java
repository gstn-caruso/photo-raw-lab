import photorawlab.adapters.*;
import photorawlab.app.RawMetadataReader;
import photorawlab.domain.edit.*;
import photorawlab.engine.*;

import java.nio.file.*;
import java.util.*;

class ValidateRaw {
    public static void main(String[] args) throws Exception {
        if (args.length < 3)
            throw new IllegalArgumentException(
                    "Uso: ValidateRaw.java origen destino archivo.ARW [archivo.ARW ...]");
        Path source = Path.of(args[0]), out = Files.createDirectories(Path.of(args[1]));
        var catalog = new LensfunCatalog();
        var repo = new JsonEditRepository();
        var names = Arrays.asList(args).subList(2, args.length);
        for (String file : names) {
            if (Path.of(file).isAbsolute() || Path.of(file).getNameCount() != 1)
                throw new IllegalArgumentException(
                        "Se requiere un nombre de archivo, no una ruta: " + file);
            Path original = source.resolve(file);
            String hash = JsonEditRepository.hash(original);
            Path copy = out.resolve(file);
            if (Files.exists(copy)) throw new FileAlreadyExistsException(copy.toString());
            Files.copy(original, copy);
            var m = new RawMetadataReader().read(copy);
            var candidates = catalog.automatic(m.camera().orElse(""), m.lens().orElse(""));
            System.out.println(
                    file
                            + " "
                            + m.camera()
                            + " "
                            + m.lens()
                            + " "
                            + m.focalLength()
                            + " f/"
                            + m.aperture()
                            + " candidates="
                            + candidates.size());
            var lens =
                    new LensSettings(
                            LensSettings.Mode.AUTO,
                            candidates.size() == 1 ? candidates.getFirst() : null,
                            true,
                            true,
                            true,
                            m.focalLength().orElse(0d),
                            m.aperture().orElse(0d),
                            catalog.cameraCrop(m.camera().orElse("")),
                            false);
            var mask =
                    new Mask(
                            "Local",
                            false,
                            Adjustments.NEUTRAL.with(0, .4),
                            List.of(
                                    new Mask.Stroke(
                                            Mask.Kind.BRUSH,
                                            false,
                                            .1,
                                            .8,
                                            .6,
                                            List.of(new Point(.3, .3), new Point(.6, .6)))));
            Recipe recipe =
                    Recipe.DEFAULT
                            .withLens(lens)
                            .withAdjustments(Adjustments.NEUTRAL.with(0, .5))
                            .withGeometry(new Geometry(.1, .1, .8, .8, 1, 1.5))
                            .withMasks(List.of(mask));
            repo.save(copy, recipe, repo.load(copy));
            var restored = new JsonEditRepository().load(copy).recipe();
            if (!restored.equals(recipe)) throw new AssertionError("restore");
            long start = System.nanoTime();
            var engine = new FloatDevelopmentEngine();
            var preview = engine.render(copy, recipe, 2048);
            double seconds = (System.nanoTime() - start) / 1e9;
            var again = new FloatDevelopmentEngine().render(copy, restored, 2048);
            if (!Arrays.equals(preview.pixels(), again.pixels()))
                throw new AssertionError("nondeterminism");
            new ImageOutput()
                    .write(preview, out, file + "-preview", ImageOutput.Format.JPEG, () -> false);
            System.out.println(
                    "preview="
                            + preview.width()
                            + "x"
                            + preview.height()
                            + " seconds="
                            + seconds
                            + " available="
                            + catalog.available(lens));
            if (file.equals(names.getFirst())) {
                start = System.nanoTime();
                var full = engine.render(copy, recipe, 0);
                new ImageOutput()
                        .write(full, out, file + "-full", ImageOutput.Format.TIFF, () -> false);
                new ImageOutput()
                        .write(full, out, file + "-full", ImageOutput.Format.JPEG, () -> false);
                System.out.println(
                        "full="
                                + full.width()
                                + "x"
                                + full.height()
                                + " seconds="
                                + (System.nanoTime() - start) / 1e9);
            }
            if (!hash.equals(JsonEditRepository.hash(original))
                    || !hash.equals(JsonEditRepository.hash(copy)))
                throw new AssertionError("original modified");
            System.out.println(
                    "sha256=" + hash + " unchanged; exact recipe and pixel roundtrip OK");
        }
    }
}
