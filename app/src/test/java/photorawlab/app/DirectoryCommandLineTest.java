package photorawlab.app;

import static org.junit.jupiter.api.Assertions.*;
import java.awt.Component;
import java.awt.Container;
import java.awt.Window;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import javax.swing.JButton;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DirectoryCommandLineTest {
    @TempDir Path directory;

    @Test void directoryArgumentShowsRealRawPreview() throws Exception {
        Path raw = directory.resolve("photo.kdc");
        Files.copy(Path.of(getClass().getResource("/raw/kodak-dc50.kdc").toURI()), raw);
        CompletableFuture<JButton> rendered = new CompletableFuture<>();
        LastDirectory memory = new LastDirectory() {
            public Optional<Path> load() { return Optional.empty(); }
            public void save(Path path) { assertEquals(directory, path); }
        };
        Timer observer = new Timer(25, event -> {
            for (Window window : Window.getWindows()) {
                JButton tile = DirectoryMosaicTest.button(window, "photo.kdc");
                if (tile != null && tile.isShowing() && tile.getIcon() != null) rendered.complete(tile);
            }
        });
        try {
            SwingUtilities.invokeAndWait(observer::start);
            Main.start(new String[] {directory.toString()}, new LibRawDecoder(), memory);
            assertNotNull(rendered.get(12, TimeUnit.SECONDS).getIcon());
        } finally {
            SwingUtilities.invokeAndWait(() -> {
                observer.stop();
                for (Window window : Window.getWindows()) if (window instanceof RawViewerFrame) window.dispose();
            });
        }
    }
}
