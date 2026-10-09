package photorawlab.app;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.stream.IntStream;
import javax.swing.JFrame;
import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

class LibraryGridTest {
    @Test
    void openingAndResizingToTheSameWidthProduceTheSameGridAndReachTheLastRow() throws Exception {
        JFrame[] frames = new JFrame[2];
        try {
            SwingUtilities.invokeAndWait(() -> {
                for (int i = 0; i < frames.length; i++) {
                    DirectoryMosaicPanel mosaic = new DirectoryMosaicPanel(path -> {});
                    mosaic.showFiles(IntStream.range(0, 6).mapToObj(index -> Path.of(index + ".raw")).toList());
                    frames[i] = new JFrame();
                    frames[i].setContentPane(mosaic);
                    frames[i].setSize(i == 0 ? 806 : 850, 500);
                    frames[i].setVisible(true);
                }
            });
            SwingUtilities.invokeAndWait(() -> frames[1].setSize(806, 500));
            SwingUtilities.invokeAndWait(() -> {
                for (JFrame frame : frames) {
                    DirectoryMosaicPanel mosaic = (DirectoryMosaicPanel) frame.getContentPane();
                    JScrollPane scroll = (JScrollPane) mosaic.getComponent(0);
                    assertEquals(0, tiles(mosaic).getComponent(2).getY(), "Three columns should fit at 806");
                    assertEquals(464, tiles(mosaic).getHeight());
                    assertFalse(scroll.getVerticalScrollBar().isVisible());
                    frame.setSize(570, 500);
                }
            });
            SwingUtilities.invokeAndWait(() -> {
                for (JFrame frame : frames) {
                    DirectoryMosaicPanel mosaic = (DirectoryMosaicPanel) frame.getContentPane();
                    JScrollPane scroll = (JScrollPane) mosaic.getComponent(0);
                    assertEquals(696, tiles(mosaic).getHeight());
                    assertTrue(scroll.getVerticalScrollBar().isVisible());
                    scroll.getVerticalScrollBar().setValue(scroll.getVerticalScrollBar().getMaximum());
                    assertEquals(696, scroll.getViewport().getViewRect().y + scroll.getViewport().getExtentSize().height);
                    assertEquals(696, tiles(mosaic).getComponent(5).getY() + tiles(mosaic).getComponent(5).getHeight());
                }
            });
        } finally {
            SwingUtilities.invokeAndWait(() -> {
                for (JFrame frame : frames) if (frame != null) frame.dispose();
            });
        }
    }

    @Test
    @EnabledIfSystemProperty(named = "libraryGridScreenshot", matches = ".+")
    void rendersLibraryGridForVisualInspection() throws Exception {
        Path fixture = Path.of(getClass().getResource("/raw/kodak-dc50.kdc").toURI());
        var image = new LibRawDecoder().decode(fixture);
        BufferedImage landscape = new PreviewRenderer(path -> image).render(fixture);
        BufferedImage portrait = new PreviewRenderer(path -> PreviewOrientation.apply(image, 6)).render(fixture);
        BufferedImage[] screenshot = new BufferedImage[1];
        JFrame[] frame = new JFrame[1];
        try {
            SwingUtilities.invokeAndWait(() -> {
                DirectoryMosaicPanel mosaic = new DirectoryMosaicPanel(path -> {});
                var paths = IntStream.range(0, 11)
                        .mapToObj(i -> Path.of("DSC_%04d.kdc".formatted(i + 1))).toList();
                mosaic.showFiles(paths);
                for (int i = 0; i < paths.size(); i++) {
                    if (i == 8) mosaic.showError(paths.get(i), "Archivo corrupto de ejemplo");
                    else mosaic.showPreview(paths.get(i), i % 3 == 1 ? portrait : landscape);
                }
                frame[0] = new JFrame();
                frame[0].setContentPane(mosaic);
                frame[0].setSize(1080, 730);
                frame[0].setVisible(true);
                frame[0].validate();
            });
            SwingUtilities.invokeAndWait(() -> {
                DirectoryMosaicPanel mosaic = (DirectoryMosaicPanel) frame[0].getContentPane();
                JScrollPane scroll = (JScrollPane) mosaic.getComponent(0);
                assertEquals(696, tiles(mosaic).getHeight());
                assertEquals(0, tiles(mosaic).getComponent(3).getY());
                assertEquals(464, tiles(mosaic).getComponent(10).getY());
                assertFalse(scroll.getVerticalScrollBar().isVisible());
                screenshot[0] = new BufferedImage(mosaic.getWidth(), mosaic.getHeight(), BufferedImage.TYPE_INT_RGB);
                var graphics = screenshot[0].createGraphics();
                try { mosaic.paint(graphics); }
                finally { graphics.dispose(); }
            });
        } finally {
            SwingUtilities.invokeAndWait(() -> { if (frame[0] != null) frame[0].dispose(); });
        }
        assertTrue(ImageIO.write(screenshot[0], "png", Path.of(System.getProperty("libraryGridScreenshot")).toFile()));
    }

    @Test
    void filenameSitsAboveAFixedPreviewAreaWithBothOrientationsCentered() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            DirectoryMosaicPanel mosaic = new DirectoryMosaicPanel(path -> {});
            Path landscape = Path.of("landscape.raw");
            Path portrait = Path.of("portrait.raw");
            mosaic.showFiles(java.util.List.of(landscape, portrait));
            mosaic.showPreview(landscape, solidPreview(240, 120));
            mosaic.showPreview(portrait, solidPreview(90, 180));
            assertPreviewPlacement((JButton) tiles(mosaic).getComponent(0), 240, 120);
            assertPreviewPlacement((JButton) tiles(mosaic).getComponent(1), 90, 180);
        });
    }

    private static BufferedImage solidPreview(int width, int height) {
        BufferedImage preview = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        var graphics = preview.createGraphics();
        graphics.setColor(java.awt.Color.MAGENTA);
        graphics.fillRect(0, 0, width, height);
        graphics.dispose();
        return preview;
    }

    private static void assertPreviewPlacement(JButton tile, int width, int height) {
        tile.setSize(264, 232);
        assertEquals(240, tile.getIcon().getIconWidth());
        assertEquals(180, tile.getIcon().getIconHeight());
        var insets = tile.getInsets();
        Rectangle view = new Rectangle(insets.left, insets.top,
                tile.getWidth() - insets.left - insets.right,
                tile.getHeight() - insets.top - insets.bottom);
        Rectangle icon = new Rectangle(), text = new Rectangle();
        SwingUtilities.layoutCompoundLabel(tile, tile.getFontMetrics(tile.getFont()),
                tile.getText(), tile.getIcon(), tile.getVerticalAlignment(),
                tile.getHorizontalAlignment(), tile.getVerticalTextPosition(),
                tile.getHorizontalTextPosition(), view, icon, text, tile.getIconTextGap());
        assertTrue(text.y + text.height < icon.y, "Filename must be above preview");
        BufferedImage painted = new BufferedImage(264, 232, BufferedImage.TYPE_INT_RGB);
        var graphics = painted.createGraphics();
        tile.paint(graphics);
        graphics.dispose();
        Rectangle photo = null;
        for (int y = 0; y < painted.getHeight(); y++) {
            for (int x = 0; x < painted.getWidth(); x++) {
                if ((painted.getRGB(x, y) & 0xffffff) == 0xff00ff) {
                    if (photo == null) photo = new Rectangle(x, y, 1, 1);
                    else photo.add(new Rectangle(x, y, 1, 1));
                }
            }
        }
        assertEquals(new Rectangle(icon.x + (240 - width) / 2,
                icon.y + (180 - height) / 2, width, height), photo);
    }

    @Test
    void resizeReflowsColumnsWithoutStretchingRowsOrTheLastPhoto() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JFrame frame = new JFrame();
            try {
                DirectoryMosaicPanel mosaic = new DirectoryMosaicPanel(path -> {});
                mosaic.showFiles(IntStream.range(0, 7).mapToObj(i -> Path.of(i + ".raw")).toList());
                frame.setContentPane(mosaic);
                frame.setSize(850, 650);
                frame.setVisible(true);
                frame.validate();
                Container tiles = tiles(mosaic);
                assertEquals(new Dimension(264, 232), tiles.getComponent(0).getSize());
                assertEquals(0, tiles.getComponent(2).getY());
                assertEquals(232, tiles.getComponent(3).getY());
                assertEquals(0, tiles.getComponent(6).getX());
                assertEquals(new Dimension(264, 232), tiles.getComponent(6).getSize());
                frame.setSize(570, 650);
                frame.validate();
                assertEquals(232, tiles.getComponent(2).getY());
                assertEquals(new Dimension(264, 232), tiles.getComponent(0).getSize());
                JScrollPane scroll = (JScrollPane) mosaic.getComponent(0);
                assertTrue(scroll.getVerticalScrollBar().isVisible());
                assertFalse(scroll.getHorizontalScrollBar().isVisible());
                mosaic.showFiles(java.util.List.of(Path.of("single.raw")));
                frame.validate();
                assertEquals(new Dimension(264, 232), tiles.getComponent(0).getSize());
                assertEquals(0, tiles.getComponent(0).getY());
                assertFalse(scroll.getVerticalScrollBar().isVisible());
            } finally {
                frame.dispose();
            }
        });
    }

    private static Container tiles(DirectoryMosaicPanel mosaic) {
        return (Container) ((JScrollPane) mosaic.getComponent(0)).getViewport().getView();
    }
}
