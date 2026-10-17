package photorawlab.app.edit;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import photorawlab.adapters.JsonEditRepository;

import java.awt.*;
import java.nio.file.*;
import java.util.concurrent.*;
import java.util.function.BooleanSupplier;

import javax.swing.*;

class DevelopPanelTest {
    @TempDir Path dir;

    @Test
    void sliderGestureAutosavesOnceAndUndoSurvivesNavigation() throws Exception {
        Path raw =
                Files.copy(
                        Path.of("src/test/resources/raw/kodak-dc50.kdc"), dir.resolve("foto.ARW"));
        String hash = JsonEditRepository.hash(raw);
        DevelopPanel[] panel = new DevelopPanel[1];
        JFrame[] frame = new JFrame[1];
        CountDownLatch loaded = new CountDownLatch(1);
        try {
            SwingUtilities.invokeAndWait(
                    () -> {
                        panel[0] = new DevelopPanel(s -> {}, p -> {});
                        frame[0] = new JFrame();
                        frame[0].add(panel[0]);
                        frame[0].setSize(1100, 800);
                        frame[0].setVisible(true);
                        panel[0].open(raw, loaded::countDown);
                    });
            assertTrue(loaded.await(10, TimeUnit.SECONDS));
            SwingUtilities.invokeAndWait(
                    () -> {
                        JSlider exposure = find(panel[0], JSlider.class, "Exposición");
                        exposure.setValueIsAdjusting(true);
                        exposure.setValue(10);
                        exposure.setValue(20);
                    });
            assertFalse(
                    Files.exists(JsonEditRepository.sidecar(raw)),
                    "Only the end of a gesture saves");
            SwingUtilities.invokeAndWait(
                    () -> find(panel[0], JSlider.class, "Exposición").setValueIsAdjusting(false));
            waitFor(() -> exposure(raw) == 1);
            SwingUtilities.invokeAndWait(
                    () -> find(panel[0], JButton.class, "Nueva máscara").doClick());
            waitFor(
                    () -> {
                        try {
                            return new JsonEditRepository().load(raw).recipe().masks().size() == 1;
                        } catch (Exception e) {
                            return false;
                        }
                    });
            SwingUtilities.invokeAndWait(() -> find(panel[0], JButton.class, "Deshacer").doClick());
            waitFor(
                    () -> {
                        try {
                            return new JsonEditRepository().load(raw).recipe().masks().isEmpty();
                        } catch (Exception e) {
                            return false;
                        }
                    });
            SwingUtilities.invokeAndWait(() -> find(panel[0], JButton.class, "Deshacer").doClick());
            waitFor(() -> exposure(raw) == 0);
            SwingUtilities.invokeAndWait(() -> find(panel[0], JButton.class, "Rehacer").doClick());
            waitFor(() -> exposure(raw) == 1);
            CountDownLatch reopened = new CountDownLatch(1);
            SwingUtilities.invokeAndWait(
                    () -> {
                        panel[0].leave();
                        panel[0].open(raw, reopened::countDown);
                    });
            assertTrue(reopened.await(10, TimeUnit.SECONDS));
            SwingUtilities.invokeAndWait(
                    () -> assertEquals(20, find(panel[0], JSlider.class, "Exposición").getValue()));
            assertEquals(hash, JsonEditRepository.hash(raw));
            Path output = Files.createDirectories(Path.of("target/verification"));
            SwingUtilities.invokeAndWait(
                    () -> {
                        try {
                            var image =
                                    new java.awt.image.BufferedImage(
                                            frame[0].getWidth(),
                                            frame[0].getHeight(),
                                            java.awt.image.BufferedImage.TYPE_INT_RGB);
                            var g = image.createGraphics();
                            frame[0].paint(g);
                            g.dispose();
                            javax.imageio.ImageIO.write(
                                    image, "png", output.resolve("develop-window.png").toFile());
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    });
        } finally {
            SwingUtilities.invokeAndWait(
                    () -> {
                        if (panel[0] != null) panel[0].shutdown();
                        if (frame[0] != null) frame[0].dispose();
                    });
        }
    }

    private static double exposure(Path raw) {
        try {
            return new JsonEditRepository().load(raw).recipe().adjustments().exposure();
        } catch (Exception e) {
            return Double.NaN;
        }
    }

    private static void waitFor(BooleanSupplier condition) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(8);
        while (!condition.getAsBoolean() && System.nanoTime() < deadline) Thread.sleep(25);
        assertTrue(condition.getAsBoolean());
    }

    private static <T extends JComponent> T find(Container c, Class<T> type, String text) {
        for (Component child : c.getComponents()) {
            if (type.isInstance(child)) {
                JComponent j = (JComponent) child;
                String name =
                        j instanceof AbstractButton b
                                ? b.getText()
                                : j.getAccessibleContext().getAccessibleName();
                if (text.equals(name)) return type.cast(child);
            }
            if (child instanceof Container nested) {
                T result = find(nested, type, text);
                if (result != null) return result;
            }
        }
        return null;
    }
}
