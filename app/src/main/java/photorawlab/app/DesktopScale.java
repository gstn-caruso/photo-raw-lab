package photorawlab.app;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public final class DesktopScale {
    private final Properties properties;
    private final Map<String, String> environment;
    private final XResources resources;

    public DesktopScale(Properties properties, Map<String, String> environment, XResources resources) {
        this.properties = properties;
        this.environment = environment;
        this.resources = resources;
    }

    public static void configure() {
        new DesktopScale(System.getProperties(), System.getenv(), DesktopScale::queryXResources).apply();
    }

    public void apply() {
        if (!properties.getProperty("os.name", "").equals("Linux")
                || environment.getOrDefault("DISPLAY", "").isBlank()
                || properties.containsKey("sun.java2d.uiScale")
                || properties.getProperty("sun.java2d.uiScale.enabled", "true").equalsIgnoreCase("false")
                || environment.containsKey("J2D_UISCALE") || environment.containsKey("GDK_SCALE")) return;
        String text;
        try { text = resources.read(); }
        catch (IOException error) { return; }
        catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            return;
        }
        for (String line : text.lines().toList()) {
            String[] entry = line.split(":", 2);
            if (entry.length != 2 || !entry[0].trim().equals("Xft.dpi")) continue;
            double dpi;
            try { dpi = Double.parseDouble(entry[1].trim()); }
            catch (NumberFormatException error) { return; }
            if (!Double.isFinite(dpi) || dpi < 96 || dpi > 768) return;
            long scale = Math.round(dpi / 96);
            if (scale >= 2) properties.setProperty("sun.java2d.uiScale", Long.toString(scale));
            return;
        }
    }

    private static String queryXResources() throws IOException, InterruptedException {
        Process query = new ProcessBuilder("xrdb", "-query").redirectError(ProcessBuilder.Redirect.DISCARD).start();
        try (var output = query.getInputStream(); var input = query.getOutputStream(); var errors = query.getErrorStream()) {
            try {
                if (!query.waitFor(750, TimeUnit.MILLISECONDS)) throw new IOException("xrdb query timed out");
                if (query.exitValue() != 0) throw new IOException("xrdb query failed");
                return new String(output.readAllBytes(), StandardCharsets.UTF_8);
            } finally { query.destroyForcibly(); }
        }
    }

    @FunctionalInterface
    public interface XResources { String read() throws IOException, InterruptedException; }
}
