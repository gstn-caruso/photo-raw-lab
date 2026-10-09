package photorawlab.domain;

import java.util.List;
import java.util.Map;
import java.util.EnumMap;
import java.util.Optional;
import java.util.Locale;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class LibraryFilter {
    public enum Facet {
        CAPTURE_DATE(photo -> photo.metadata().captured().map(value -> value.toLocalDate().toString())),
        CAMERA(photo -> photo.metadata().camera()),
        LENS(photo -> photo.metadata().lens()),
        FILE_TYPE(photo -> {
            int dot = photo.filename().lastIndexOf('.');
            return dot < 0 || dot == photo.filename().length() - 1 ? Optional.empty()
                    : Optional.of(photo.filename().substring(dot + 1).toUpperCase(Locale.ROOT));
        });

        private final Function<GalleryPhoto, Optional<String>> value;
        Facet(Function<GalleryPhoto, Optional<String>> value) { this.value = value; }
        public Value valueOf(GalleryPhoto photo) { return new Value(value.apply(photo)); }
    }

    public record Value(Optional<String> text) {
        public static Value known(String text) { return new Value(Optional.of(text)); }
        public static Value unknown() { return new Value(Optional.empty()); }
        public String label() { return text.orElse("Sin datos"); }
    }

    private final Map<Facet, Value> selections = new EnumMap<>(Facet.class);

    public void select(Facet facet, Value value) {
        if (value == null) selections.remove(facet);
        else selections.put(facet, value);
    }

    public Optional<Value> selection(Facet facet) { return Optional.ofNullable(selections.get(facet)); }
    public void clear() { selections.clear(); }
    public boolean matches(GalleryPhoto photo) { return matchesExcept(photo, null); }
    public List<GalleryPhoto> apply(List<GalleryPhoto> photos) { return photos.stream().filter(this::matches).toList(); }

    public Map<Value, Long> counts(List<GalleryPhoto> photos, Facet facet) {
        return photos.stream().filter(photo -> matchesExcept(photo, facet))
                .collect(Collectors.groupingBy(facet::valueOf, Collectors.counting()));
    }

    private boolean matchesExcept(GalleryPhoto photo, Facet excluded) {
        return selections.entrySet().stream().allMatch(selection -> selection.getKey() == excluded
                || selection.getValue().equals(selection.getKey().valueOf(photo)));
    }
}
