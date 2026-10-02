package photorawlab.app;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

class PackagedLauncherIT {
    @Test
    @Timeout(30)
    void packagedApplicationDisplaysTheRealRawPhotograph() throws Exception {
        Path raw = Path.of(getClass().getResource("/raw/kodak-dc50.kdc").toURI());
        Path output = Files.createDirectories(Path.of("target", "verification"));
        String launcher = System.getProperty("photo.raw.launcher");
        ProcessBuilder command = launcher == null
                ? new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                    "-jar", Path.of("target", System.getProperty("photo.raw.jar")).toString(), raw.toString())
                : new ProcessBuilder(launcher, raw.toString());
        Process process = command.redirectErrorStream(true)
                .redirectOutput(output.resolve("launcher.log").toFile()).start();
        try {
            Robot robot = new Robot();
            Rectangle screen = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);
            BufferedImage screenshot;
            int colors;
            do {
                assertTrue(process.isAlive(), "Launcher exited: " + Files.readString(output.resolve("launcher.log")));
                screenshot = robot.createScreenCapture(screen);
                colors = photographicColors(screenshot);
                if (colors > 32) break;
                robot.delay(50);
            } while (System.nanoTime() < deadline);
            ImageIO.write(screenshot, "png", output.resolve("packaged-window.png").toFile());
            assertTrue(colors > 32, "Launcher must render a photograph, observed " + colors + " colors");
        } finally {
            process.destroy();
            if (!process.waitFor(5, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                process.waitFor(5, TimeUnit.SECONDS);
            }
        }
    }

    private int photographicColors(BufferedImage image) {
        Set<Integer> colors = new HashSet<>();
        for (int y = 200; y < Math.min(600, image.getHeight()); y += 10) {
            for (int x = 200; x < Math.min(800, image.getWidth()); x += 10) {
                colors.add(image.getRGB(x, y));
            }
        }
        return colors.size();
    }
}
