package photorawlab.app;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.Test;

class DesktopScaleTest {
    @Test void usesDesktopDpiToSelectIntegerScaleBeforeSwingStartup() {
        Properties properties = linux();
        new DesktopScale(properties, Map.of("DISPLAY", ":0"), () -> "Xft.dpi:\t192\n").apply();
        assertEquals("2", properties.getProperty("sun.java2d.uiScale"));
        properties = linux();
        new DesktopScale(properties, Map.of("DISPLAY", ":0"), () -> "Xft.dpi: 288.0").apply();
        assertEquals("3", properties.getProperty("sun.java2d.uiScale"));
    }

    @Test void normalDpiLeavesJavaAutomaticScaleAvailable() {
        Properties properties = linux();
        new DesktopScale(properties, Map.of("DISPLAY", ":0"), () -> "Xft.dpi: 96").apply();
        assertNull(properties.getProperty("sun.java2d.uiScale"));
    }

    private static Properties linux() {
        Properties properties = new Properties();
        properties.setProperty("os.name", "Linux");
        return properties;
    }
}
