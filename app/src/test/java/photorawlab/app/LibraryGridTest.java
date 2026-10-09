package photorawlab.app;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Container;
import java.awt.Dimension;
import java.nio.file.Path;
import java.util.stream.IntStream;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;

class LibraryGridTest {
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
