package photorawlab.app;

import java.util.Map;
import java.util.Properties;

public final class DesktopScale {
    private final Properties properties;
    private final Map<String, String> environment;
    private final XResources resources;

    public DesktopScale(Properties properties, Map<String, String> environment, XResources resources) {
        this.properties = properties;
        this.environment = environment;
        this.resources = resources;
    }

    public void apply() {
        if (!properties.getProperty("os.name", "").equals("Linux")
                || environment.getOrDefault("DISPLAY", "").isBlank()
                || properties.containsKey("sun.java2d.uiScale")
                || properties.getProperty("sun.java2d.uiScale.enabled", "true").equalsIgnoreCase("false")
                || environment.containsKey("J2D_UISCALE") || environment.containsKey("GDK_SCALE")) return;
        String text = resources.read();
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

    @FunctionalInterface
    public interface XResources { String read(); }
}
