import photorawlab.app.*;

import java.nio.file.*;
import java.util.*;

import javax.swing.*;

class MeasureLibrary {
    public static void main(String[] a) throws Exception {
        Path source = Path.of(a[0]), dir = Files.createTempDirectory("photoraw-1000-");
        List<Path> originals;
        try (var list = Files.list(source)) {
            originals = list.filter(p -> p.toString().endsWith(".ARW")).sorted().toList();
        }
        for (int i = 0; i < 1000; i++)
            Files.createSymbolicLink(
                    dir.resolve(String.format("%04d.ARW", i)), originals.get(i % originals.size()));
        long start = System.nanoTime();
        var entries = new RawDirectory().snapshot(dir);
        System.out.println(
                "1000 entries, 184 distinct real RAW reused with symlinks; metadata seconds="
                        + (System.nanoTime() - start) / 1e9);
        DirectoryMosaicPanel[] mosaic = new DirectoryMosaicPanel[1];
        JFrame[] frame = new JFrame[1];
        start = System.nanoTime();
        SwingUtilities.invokeAndWait(
                () -> {
                    mosaic[0] = new DirectoryMosaicPanel(p -> {});
                    frame[0] = new JFrame();
                    frame[0].add(mosaic[0]);
                    frame[0].setSize(1100, 800);
                    mosaic[0].showEntries(entries);
                    frame[0].setVisible(true);
                });
        System.out.println("EDT grid construction seconds=" + (System.nanoTime() - start) / 1e9);
        var renderer = new PreviewRenderer(new LibRawDecoder());
        start = System.nanoTime();
        int fail = 0;
        for (var entry : entries) {
            try {
                var image = renderer.render(entry.path());
                SwingUtilities.invokeAndWait(() -> mosaic[0].showPreview(entry.path(), image));
            } catch (Exception e) {
                fail++;
            }
        }
        System.out.println(
                "1000 previews seconds=" + (System.nanoTime() - start) / 1e9 + " errors=" + fail);
        System.gc();
        System.out.println(
                "retained heap MiB="
                        + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory())
                                / 1048576);
        SwingUtilities.invokeAndWait(() -> frame[0].dispose());
        try (var paths = Files.list(dir)) {
            for (Path path : paths.toList()) Files.delete(path);
        }
        Files.delete(dir);
    }
}
