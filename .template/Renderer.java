import java.io.IOException;
import java.nio.charset.CharacterCodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Year;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Renderer {
    public static void main(String[] arguments) throws IOException {
        try {
            render(Path.of("").toAbsolutePath(), options(arguments));
        } catch (IllegalArgumentException failure) {
            System.err.println(failure.getMessage());
            System.err.println("Usage: java .template/Renderer.java --slug <slug> --name <name> --description <description> --owner <owner> [--flavor swing|libgdx|spring-ssr]");
            System.exit(2);
        }
    }

    static void render(Path root, Map<String, String> arguments) throws IOException {
        Map<String, String> values = new HashMap<>(arguments);
        for (String required : List.of("slug", "name", "description", "owner")) {
            if (values.getOrDefault(required, "").isBlank()) {
                throw new IllegalArgumentException("Missing required option: --" + required);
            }
        }
        String flavor = values.getOrDefault("flavor", "swing");
        Path template = root.resolve(".template");
        Path flavorDirectory = template.resolve("flavors").resolve(flavor);
        if (!List.of("swing", "libgdx", "spring-ssr").contains(flavor) || !Files.isDirectory(flavorDirectory)) {
            throw new IllegalArgumentException("Unknown flavor: " + flavor);
        }
        String slug = values.get("slug");
        if (!slug.matches("[a-z][a-z0-9]*(?:-[a-z0-9]+)*")) {
            throw new IllegalArgumentException("Invalid slug: " + slug);
        }
        values.putIfAbsent("package", slug.replace("-", ""));
        values.putIfAbsent("class", Arrays.stream(slug.split("-"))
                .map(part -> part.substring(0, 1).toUpperCase() + part.substring(1))
                .reduce("", String::concat));
        values.putIfAbsent("group_id", "io.github." + values.get("owner").replace("-", ""));
        values.putIfAbsent("author", "");
        values.putIfAbsent("email", "");
        values.putIfAbsent("year", Integer.toString(Year.now().getValue()));
        values.put("app_module", flavor.equals("libgdx") ? "game" : "app");
        copy(template.resolve("common"), root);
        copy(flavorDirectory, root);
        delete(template);
        Files.deleteIfExists(root.resolve(".github/workflows/template-ci.yml"));
        replacePlaceholders(root, values);
    }

    private static Map<String, String> options(String[] arguments) {
        if (arguments.length % 2 != 0) {
            throw new IllegalArgumentException("Options need values");
        }
        Map<String, String> options = new HashMap<>();
        for (int index = 0; index < arguments.length; index += 2) {
            if (!arguments[index].startsWith("--")) {
                throw new IllegalArgumentException("Unknown option: " + arguments[index]);
            }
            String key = arguments[index].substring(2);
            if (!List.of("slug", "name", "description", "owner", "flavor", "package", "class", "group-id", "author", "email", "year").contains(key)) {
                throw new IllegalArgumentException("Unknown option: " + arguments[index]);
            }
            options.put(key.replace('-', '_'), arguments[index + 1]);
        }
        return options;
    }

    private static void copy(Path source, Path destination) throws IOException {
        try (var paths = Files.walk(source)) {
            for (Path path : paths.toList()) {
                Path target = destination.resolve(source.relativize(path));
                if (Files.isDirectory(path)) {
                    Files.createDirectories(target);
                } else {
                    Files.createDirectories(target.getParent());
                    Files.copy(path, target, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private static void delete(Path root) throws IOException {
        try (var paths = Files.walk(root)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(path);
            }
        }
    }

    private static void replacePlaceholders(Path root, Map<String, String> values) throws IOException {
        try (var paths = Files.walk(root)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Path relative = root.relativize(path);
                if (relative.getNameCount() > 0 && (relative.getName(0).toString().equals(".git")
                        || relative.getName(0).toString().equals(".worktrees"))
                        || relative.toString().contains("target/")) {
                    continue;
                }
                Path renamed = path.resolveSibling(replace(path.getFileName().toString(), values));
                if (!renamed.equals(path)) {
                    Files.move(path, renamed);
                    path = renamed;
                }
                if (Files.isRegularFile(path)) {
                    try {
                        String original = Files.readString(path);
                        if (original.contains("{{")) {
                            Files.writeString(path, replace(original, values,
                                    path.toString().endsWith(".xml") || path.toString().endsWith(".html"),
                                    path.toString().endsWith(".java")));
                        }
                    } catch (CharacterCodingException ignored) {
                    }
                }
            }
        }
    }

    private static String replace(String text, Map<String, String> values) {
        return replace(text, values, false, false);
    }

    private static String replace(String text, Map<String, String> values, boolean xml, boolean javaSource) {
        String replaced = text;
        for (var entry : values.entrySet()) {
            String value = entry.getValue();
            if (xml) {
                value = value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                        .replace("\"", "&quot;").replace("'", "&apos;");
            }
            if (javaSource) {
                value = value.replace("\\", "\\\\").replace("\"", "\\\"")
                        .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
            }
            replaced = replaced.replace("{{" + entry.getKey() + "}}", value);
        }
        if (replaced.matches("(?s).*\\{\\{[a-z_]+}}.*")) {
            throw new IllegalArgumentException("Unresolved placeholder: " + replaced);
        }
        return replaced;
    }
}
