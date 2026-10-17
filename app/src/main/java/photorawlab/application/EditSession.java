package photorawlab.application;

import photorawlab.domain.edit.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

/** All methods called on the serial I/O worker, never from Swing. Failed saves retain drafts. */
public final class EditSession {
    public record Photo(Path path, Recipe recipe, boolean dirty) {}

    private record Draft(EditRepository.Saved saved, Recipe recipe) {}

    private final EditRepository repository;
    private final Map<Path, Draft> drafts = new HashMap<>();

    public EditSession(EditRepository repository) {
        this.repository = repository;
    }

    public Photo open(Path path) throws IOException {
        Draft existing = drafts.get(path);
        if (existing == null) {
            var saved = repository.load(path);
            existing = new Draft(saved, saved.recipe());
            drafts.put(path, existing);
        }
        return new Photo(
                path, existing.recipe(), !existing.recipe().equals(existing.saved().recipe()));
    }

    public void save(Path path, Recipe recipe) throws IOException {
        Draft draft = drafts.get(path);
        if (draft == null) throw new IOException("La foto no está abierta");
        drafts.put(path, new Draft(draft.saved(), recipe));
        EditRepository.Saved saved = repository.save(path, recipe, draft.saved());
        drafts.put(path, new Draft(saved, recipe));
    }

    public Recipe recipe(Path path) throws IOException {
        return open(path).recipe();
    }

    public int pending() {
        return (int)
                drafts.values().stream()
                        .filter(d -> !d.recipe().equals(d.saved().recipe()))
                        .count();
    }

    public void retry() throws IOException {
        IOException failure = null;
        for (var entry : List.copyOf(drafts.entrySet()))
            if (!entry.getValue().recipe().equals(entry.getValue().saved().recipe())) {
                try {
                    save(entry.getKey(), entry.getValue().recipe());
                } catch (IOException e) {
                    failure = e;
                }
            }
        if (failure != null) throw failure;
    }
}
