package photorawlab.application;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import photorawlab.adapters.ImageOutput;
import photorawlab.domain.edit.*;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

class ExportQueueTest {
    @TempDir Path out;

    @Test
    void corruptFileDoesNotPreventNextExportAndEarlyCancellationIsRetained() throws Exception {
        Path broken = Files.writeString(out.resolve("corrupt.ARW"), "broken");
        Path good = Path.of("src/test/resources/raw/kodak-dc50.kdc");
        try (var queue = new ExportQueue()) {
            var result = new CompletableFuture<ExportQueue.Result>();
            queue.beginBatch();
            queue.submit(
                    List.of(
                            new ExportQueue.Job(broken, Recipe.DEFAULT),
                            new ExportQueue.Job(good, Recipe.DEFAULT)),
                    out,
                    ImageOutput.Format.JPEG,
                    List.of(),
                    s -> {},
                    result::complete);
            var complete = result.get(10, TimeUnit.SECONDS);
            assertEquals(1, complete.completed());
            assertEquals(1, complete.errors().size());
            assertTrue(Files.exists(out.resolve("kodak-dc50.kdc.jpg")));
            var cancelled = new CompletableFuture<ExportQueue.Result>();
            queue.beginBatch();
            queue.cancel();
            queue.submit(
                    List.of(new ExportQueue.Job(good, Recipe.DEFAULT)),
                    out,
                    ImageOutput.Format.JPEG,
                    List.of(),
                    s -> {},
                    cancelled::complete);
            assertTrue(cancelled.get(5, TimeUnit.SECONDS).cancelled());
            assertFalse(Files.exists(out.resolve("kodak-dc50.kdc-1.jpg")));
        }
    }

    @Test
    void failedSaveKeepsDraftUntilRetry() throws Exception {
        EditRepository repo =
                new EditRepository() {
                    boolean fail = true;

                    public Saved load(Path p) {
                        return new Saved(Recipe.DEFAULT, "original", "");
                    }

                    public Saved save(Path p, Recipe r, Saved expected) throws java.io.IOException {
                        if (fail) {
                            fail = false;
                            throw new java.io.IOException("disco lleno");
                        }
                        return new Saved(r, "original", "saved");
                    }
                };
        var session = new EditSession(repo);
        Path path = out.resolve("photo.ARW");
        session.open(path);
        Recipe edited = Recipe.DEFAULT.withAdjustments(Adjustments.NEUTRAL.with(0, 1));
        assertThrows(java.io.IOException.class, () -> session.save(path, edited));
        assertEquals(edited, session.recipe(path));
        assertEquals(1, session.pending());
        session.retry();
        assertEquals(0, session.pending());
    }
}
