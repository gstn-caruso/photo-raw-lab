package photorawlab.app;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.prefs.Preferences;
import javax.swing.JButton;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import photorawlab.domain.RgbImage;

class DirectoryMemoryTest {
    @TempDir Path directory;

    @Test void openingPhotoBeforeAllPreviewsFinishStillRemembersDirectoryOnRestart() throws Exception {
        Path first = Files.createFile(directory.resolve("a.raw"));
        Files.createFile(directory.resolve("b.raw"));
        Preferences node = Preferences.userRoot().node("photo-raw-memory-test-" + java.util.UUID.randomUUID());
        RawViewerFrame[] frame = new RawViewerFrame[1];
        CountDownLatch secondEntered = new CountDownLatch(1), releaseSecond = new CountDownLatch(1);
        CompletableFuture<JButton> preview = new CompletableFuture<>();
        Timer observer = new Timer(25, event -> {
            JButton tile = DirectoryMosaicTest.button(frame[0], "a.raw");
            if (tile != null && tile.isEnabled()) preview.complete(tile);
        });
        try {
            SwingUtilities.invokeAndWait(() -> {
                frame[0] = new RawViewerFrame(path -> {
                    if (!path.equals(first)) {
                        secondEntered.countDown();
                        try { assertTrue(releaseSecond.await(5, TimeUnit.SECONDS)); }
                        catch (InterruptedException error) { throw new java.io.IOException(error); }
                    }
                    return new RgbImage(1, 1, new int[] {0xff0000});
                }, new PreferencesLastDirectory(node));
                observer.start();
                frame[0].openDirectory(directory);
            });
            JButton tile = preview.get(5, TimeUnit.SECONDS);
            assertTrue(secondEntered.await(5, TimeUnit.SECONDS));
            SwingUtilities.invokeAndWait(tile::doClick);
            assertEquals(Optional.of(directory), new StartupDirectory(new PreferencesLastDirectory(node)).select(() -> {
                fail("Restart must reuse the accepted directory even while previews are pending");
                return Optional.empty();
            }));
        } finally {
            releaseSecond.countDown();
            SwingUtilities.invokeAndWait(() -> { observer.stop(); if (frame[0] != null) frame[0].dispose(); });
            node.removeNode();
        }
    }

    @Test void directoryThatCannotBeListedDoesNotReplaceRememberedDirectory() throws Exception {
        Preferences node = Preferences.userRoot().node("photo-raw-memory-test-" + java.util.UUID.randomUUID());
        LastDirectory memory = new PreferencesLastDirectory(node);
        memory.save(directory);
        RawViewerFrame[] frame = new RawViewerFrame[1];
        CountDownLatch complete = new CountDownLatch(1);
        try {
            SwingUtilities.invokeAndWait(() -> {
                frame[0] = new RawViewerFrame(path -> { fail("Missing folder must not decode"); return null; }, memory);
                frame[0].addPropertyChangeListener("loading", event -> complete.countDown());
                frame[0].openDirectory(directory.resolve("missing"));
            });
            assertTrue(complete.await(5, TimeUnit.SECONDS));
            assertEquals(Optional.of(directory), memory.load());
        } finally {
            SwingUtilities.invokeAndWait(() -> { if (frame[0] != null) frame[0].dispose(); });
            node.removeNode();
        }
    }
}
