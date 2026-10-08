package photorawlab.app;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import photorawlab.domain.RgbImage;

class MosaicLifecycleTest {
    @TempDir Path root;

    @Test void obsoleteCompletionDoesNotAnnounceNewDirectoryLoadedOrPublishOldTile() throws Exception {
        Path old = Files.createDirectory(root.resolve("old"));
        Path current = Files.createDirectory(root.resolve("current"));
        Files.createFile(old.resolve("old.raw"));
        Files.createFile(current.resolve("current.raw"));
        CountDownLatch enteredOld = new CountDownLatch(1), releaseOld = new CountDownLatch(1);
        CountDownLatch enteredCurrent = new CountDownLatch(1), releaseCurrent = new CountDownLatch(1);
        CountDownLatch completed = new CountDownLatch(1);
        AtomicInteger notifications = new AtomicInteger();
        RawViewerFrame[] frame = new RawViewerFrame[1];
        LastDirectory memory = new LastDirectory() {
            public Optional<Path> load() { return Optional.empty(); }
            public void save(Path directory) { assertEquals(current, directory); }
        };
        try {
            SwingUtilities.invokeAndWait(() -> {
                frame[0] = new RawViewerFrame(path -> {
                    try {
                        if (path.startsWith(old)) { enteredOld.countDown(); assertTrue(releaseOld.await(5, TimeUnit.SECONDS)); }
                        else { enteredCurrent.countDown(); assertTrue(releaseCurrent.await(5, TimeUnit.SECONDS)); }
                    } catch (InterruptedException error) { throw new java.io.IOException(error); }
                    return new RgbImage(1, 1, new int[] {0xff0000});
                }, memory);
                frame[0].addPropertyChangeListener("loading", event -> { notifications.incrementAndGet(); completed.countDown(); });
                frame[0].openDirectory(old);
            });
            assertTrue(enteredOld.await(5, TimeUnit.SECONDS));
            SwingUtilities.invokeAndWait(() -> frame[0].openDirectory(current));
            releaseOld.countDown();
            assertTrue(enteredCurrent.await(5, TimeUnit.SECONDS));
            assertFalse(completed.await(200, TimeUnit.MILLISECONDS), "Obsolete directory must not announce completion");
            releaseCurrent.countDown();
            assertTrue(completed.await(5, TimeUnit.SECONDS));
            SwingUtilities.invokeAndWait(() -> {
                assertNull(DirectoryMosaicTest.button(frame[0], "old.raw"));
                assertNotNull(DirectoryMosaicTest.button(frame[0], "current.raw").getIcon());
            });
            assertEquals(1, notifications.get());
        } finally {
            releaseOld.countDown(); releaseCurrent.countDown();
            SwingUtilities.invokeAndWait(() -> { if (frame[0] != null) frame[0].dispose(); });
        }
    }
}
