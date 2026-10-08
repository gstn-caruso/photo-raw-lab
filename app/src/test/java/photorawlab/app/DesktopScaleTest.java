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

    @Test void respectsExplicitPropertiesEvenIfScaleIsInvalid() {
        for (String scale : new String[] {"1.5", "invalid", ""}) {
            Properties properties = linux();
            properties.setProperty("sun.java2d.uiScale", scale);
            new DesktopScale(properties, Map.of("DISPLAY", ":0"), () -> {
                fail("Explicit scale must bypass desktop discovery"); return "";
            }).apply();
            assertEquals(scale, properties.getProperty("sun.java2d.uiScale"));
        }
        Properties disabled = linux();
        disabled.setProperty("sun.java2d.uiScale.enabled", "false");
        new DesktopScale(disabled, Map.of("DISPLAY", ":0"), () -> {
            fail("Disabled scaling must bypass discovery"); return "";
        }).apply();
        assertNull(disabled.getProperty("sun.java2d.uiScale"));
    }

    @Test void respectsEnvironmentOverridesAndSkipsNonX11Desktops() {
        for (Map<String, String> environment : java.util.List.of(
                Map.of("DISPLAY", ":0", "J2D_UISCALE", "3"),
                Map.of("DISPLAY", ":0", "GDK_SCALE", "2"),
                Map.<String, String>of(), Map.of("DISPLAY", " "))) {
            Properties properties = linux();
            new DesktopScale(properties, environment, () -> {
                fail("Environment must bypass discovery"); return "";
            }).apply();
            assertNull(properties.getProperty("sun.java2d.uiScale"));
        }
        Properties windows = linux();
        windows.setProperty("os.name", "Windows 11");
        new DesktopScale(windows, Map.of("DISPLAY", ":0"), () -> {
            fail("Other platforms must retain automatic scaling"); return "";
        }).apply();
    }

    @Test void malformedMissingAndOutOfRangeDpiLeaveAutomaticScaling() {
        for (String resources : new String[] {"", "other.Xft.dpi: 192", "Xft.dpiExtra: 192",
                "Xft.dpi: NaN", "Xft.dpi: Infinity", "Xft.dpi: -192", "Xft.dpi: 0",
                "Xft.dpi: 48", "Xft.dpi: 769", "Xft.dpi: invalid", "Xft.dpi: "}) {
            Properties properties = linux();
            assertDoesNotThrow(() -> new DesktopScale(properties, Map.of("DISPLAY", ":0"), () -> resources).apply());
            assertNull(properties.getProperty("sun.java2d.uiScale"), resources);
        }
    }

    @Test void roundsFractionalDpiAndAcceptsWhitespaceAroundExactResourceName() {
        Properties properties = linux();
        new DesktopScale(properties, Map.of("DISPLAY", ":0"), () -> "Xft.antialias: 1\n Xft.dpi : 167.5\n").apply();
        assertEquals("2", properties.getProperty("sun.java2d.uiScale"));
    }

    private static Properties linux() {
        Properties properties = new Properties();
        properties.setProperty("os.name", "Linux");
        return properties;
    }
}
