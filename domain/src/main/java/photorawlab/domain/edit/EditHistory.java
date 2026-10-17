package photorawlab.domain.edit;

import java.util.ArrayDeque;
import java.util.Objects;

/** One commit per completed gesture; bounded session history. */
public final class EditHistory {
    private Recipe current;
    private final ArrayDeque<Recipe> undo = new ArrayDeque<>(), redo = new ArrayDeque<>();

    public EditHistory(Recipe recipe) {
        current = Objects.requireNonNull(recipe);
    }

    public Recipe current() {
        return current;
    }

    public void commit(Recipe recipe) {
        if (current.equals(recipe)) return;
        undo.push(current);
        if (undo.size() > 100) undo.removeLast();
        current = recipe;
        redo.clear();
    }

    public Recipe undo() {
        if (!undo.isEmpty()) {
            redo.push(current);
            current = undo.pop();
        }
        return current;
    }

    public Recipe redo() {
        if (!redo.isEmpty()) {
            undo.push(current);
            current = redo.pop();
        }
        return current;
    }
}
