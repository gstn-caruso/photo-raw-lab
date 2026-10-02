import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import javax.xml.parsers.DocumentBuilderFactory;

public class RendererTest {
    public static void main(String[] args) throws Exception {
        rendersSwingByDefault();
        rendersEveryFlavor();
        escapesJavaLiterals();
        rejectsUnknownFlavor();
    }

    private static void rendersSwingByDefault() throws Exception {
        Path project = copyTemplate();
        Renderer.render(project, Map.of(
                "slug", "sample-app",
                "name", "Sample App",
                "description", "A/B & C",
                "owner", "gstn-caruso",
                "author", "Test",
                "email", "test@example.com"));
        require(Files.exists(project.resolve("app/src/main/java/sampleapp/app/Main.java")));
        require(Files.readString(project.resolve("README.md")).contains("# Sample App"));
        var projectXml = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(project.resolve("pom.xml").toFile());
        require(projectXml.getElementsByTagName("description").item(0).getTextContent().equals("A/B & C"));
        require(!Files.exists(project.resolve(".template")));
        require(Files.exists(project.resolve("CHANGELOG.md")));
        String workflow = Files.readString(project.resolve(".github/workflows/ci.yml"));
        require(workflow.contains("gh release create"));
        require(workflow.contains("sample-app-app-0.1.${{ github.run_number }}.jar"));
        require(workflow.contains("sample-app_0.1.${{ github.run_number }}_amd64.deb"));
        require(workflow.contains("--target ${{ github.sha }}"));
        require(workflow.contains("  build:\n"));
        require(workflow.contains("install -D app/target/sample-app-app-0.1.${{ github.run_number }}.jar"));
        require(workflow.contains("--input ${{ runner.temp }}/jpackage-input"));
        require(workflow.contains("_amd64.deb CHANGELOG.md --title"));
    }

    private static void rendersEveryFlavor() throws Exception {
        for (String flavor : new String[] {"swing", "libgdx", "spring-ssr"}) {
            Path project = copyTemplate();
            Renderer.render(project, Map.of(
                    "slug", "sample-app",
                    "name", "Sample App",
                    "description", "Sample",
                    "owner", "gstn-caruso",
                    "flavor", flavor));
            require(Files.exists(project.resolve("domain/pom.xml")));
            require(Files.exists(project.resolve(flavor.equals("libgdx") ? "game/pom.xml" : "app/pom.xml")));
        }
    }

    private static void rejectsUnknownFlavor() throws Exception {
        Path project = copyTemplate();
        try {
            Renderer.render(project, Map.of(
                    "slug", "sample-app",
                    "name", "Sample App",
                    "description", "Sample",
                    "owner", "gstn-caruso",
                    "flavor", "other"));
            throw new AssertionError("Expected unknown flavor to fail");
        } catch (IllegalArgumentException expected) {
            require(expected.getMessage().contains("flavor"));
        }
    }

    private static void escapesJavaLiterals() throws Exception {
        for (String flavor : new String[] {"swing", "spring-ssr"}) {
            Path project = copyTemplate();
            Renderer.render(project, Map.of(
                    "slug", "sample-app",
                    "name", "Café Mi \"App\" \\ Demo & Co",
                    "description", "A/B & C",
                    "owner", "gstn-caruso",
                    "flavor", flavor));
            var build = new ProcessBuilder("mvn", "-q", "-B", "test")
                    .directory(project.toFile())
                    .inheritIO()
                    .start();
            require(build.waitFor() == 0);
        }
    }

    private static Path copyTemplate() throws Exception {
        Path source = Path.of("").toAbsolutePath();
        Path destination = Files.createTempDirectory("java-template-");
        try (var paths = Files.walk(source)) {
            for (Path path : paths.toList()) {
                Path relative = source.relativize(path);
                if (relative.getNameCount() > 0 && (relative.getName(0).toString().equals(".git")
                        || relative.getName(0).toString().equals(".worktrees"))
                        || relative.toString().contains("target/")) {
                    continue;
                }
                Path target = destination.resolve(relative);
                if (Files.isDirectory(path)) {
                    Files.createDirectories(target);
                } else {
                    Files.createDirectories(target.getParent());
                    Files.copy(path, target);
                }
            }
        }
        return destination;
    }

    private static void require(boolean condition) {
        if (!condition) {
            throw new AssertionError("Renderer contract failed");
        }
    }
}
