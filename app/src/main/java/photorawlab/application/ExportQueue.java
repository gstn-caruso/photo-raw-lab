package photorawlab.application;

import photorawlab.adapters.ImageOutput;
import photorawlab.domain.edit.Recipe;
import photorawlab.engine.FloatDevelopmentEngine;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/** A batch owns an immutable recipe snapshot. File failures do not abort subsequent jobs. */
public final class ExportQueue implements AutoCloseable {
    public record Job(Path original, Recipe recipe) {}

    public record Result(int completed, boolean cancelled, List<String> errors) {
        public Result {
            errors = List.copyOf(errors);
        }
    }

    private final ExecutorService worker =
            Executors.newSingleThreadExecutor(r -> new Thread(r, "exportacion"));
    private final AtomicBoolean cancelled = new AtomicBoolean();
    private boolean running;

    public synchronized void submit(
            List<Job> input,
            Path destination,
            ImageOutput.Format format,
            List<String> readErrors,
            Consumer<String> progress,
            Consumer<Result> completion) {
        if (running) throw new IllegalStateException("Ya hay una exportación en curso");
        running = true;
        List<Job> jobs = List.copyOf(input);
        worker.execute(
                () -> {
                    int done = 0;
                    List<String> errors = new ArrayList<>(readErrors);
                    var engine = new FloatDevelopmentEngine();
                    try {
                        for (Job job : jobs) {
                            if (cancelled.get()) break;
                            try {
                                var image = engine.render(job.original(), job.recipe(), 0);
                                new ImageOutput()
                                        .write(
                                                image,
                                                destination,
                                                job.original().getFileName().toString(),
                                                format,
                                                cancelled::get);
                                done++;
                                progress.accept("Exportadas " + done + " / " + jobs.size());
                            } catch (Exception e) {
                                if (!cancelled.get())
                                    errors.add(
                                            job.original().getFileName() + ": " + e.getMessage());
                            }
                        }
                    } finally {
                        synchronized (this) {
                            running = false;
                        }
                        completion.accept(new Result(done, cancelled.get(), errors));
                    }
                });
    }

    public synchronized void beginBatch() {
        if (running) throw new IllegalStateException("Ya hay una exportación en curso");
        cancelled.set(false);
    }

    public void cancel() {
        cancelled.set(true);
    }

    @Override
    public void close() {
        cancel();
        worker.shutdown();
    }
}
