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
        String text = resources.read();
        for (String line : text.lines().toList()) {
            String[] entry = line.split(":", 2);
            if (entry.length != 2 || !entry[0].trim().equals("Xft.dpi")) continue;
            double dpi = Double.parseDouble(entry[1].trim());
            long scale = Math.round(dpi / 96);
            if (scale >= 2) properties.setProperty("sun.java2d.uiScale", Long.toString(scale));
            return;
        }
    }

    @FunctionalInterface
    public interface XResources { String read(); }
}
