package photorawlab.app;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;

public final class DirectoryMosaicPanel extends JPanel {
    private final JPanel tiles = new LibraryGrid();
    private final Map<Path, JButton> buttons = new LinkedHashMap<>();
    private final Consumer<Path> openPhoto;

    public DirectoryMosaicPanel(Consumer<Path> openPhoto) {
        super(new BorderLayout());
        this.openPhoto = openPhoto;
        setBackground(AppPalette.PANEL);
        tiles.setBackground(AppPalette.BORDER);
        JScrollPane scroll = new JScrollPane(tiles);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        AppPalette.styleScrollPane(scroll);
        add(scroll);
    }

    public void showFiles(List<Path> paths) {
        tiles.removeAll();
        buttons.clear();
        for (Path path : paths) {
            JButton tile = new JButton(path.getFileName().toString());
            AppPalette.styleButton(tile, AppPalette.TILE);
            tile.setVerticalTextPosition(JButton.TOP);
            tile.setHorizontalTextPosition(JButton.CENTER);
            tile.setVerticalAlignment(JButton.TOP);
            tile.setIconTextGap(8);
            tile.setEnabled(false);
            tile.addActionListener(event -> openPhoto.accept(path));
            buttons.put(path, tile);
            tiles.add(tile);
        }
        revalidate();
        repaint();
    }

    public void showPreview(Path path, BufferedImage preview) {
        JButton tile = buttons.get(path);
        tile.setIcon(new PreviewIcon(preview));
        tile.setEnabled(true);
    }

    public void showError(Path path, String error) {
        buttons.get(path).setToolTipText("No se pudo abrir: " + error);
    }

    private record PreviewIcon(BufferedImage preview) implements Icon {
        @Override
        public int getIconWidth() {
            return 240;
        }

        @Override
        public int getIconHeight() {
            return 180;
        }

        @Override
        public void paintIcon(Component component, Graphics graphics, int x, int y) {
            if (preview != null) {
                graphics.drawImage(preview, x + (getIconWidth() - preview.getWidth()) / 2,
                        y + (getIconHeight() - preview.getHeight()) / 2, null);
            }
        }
    }

    private static final class LibraryGrid extends JPanel implements Scrollable {
        private static final int CELL_WIDTH = 264;
        private static final int CELL_HEIGHT = 232;

        LibraryGrid() {
            super(null);
        }

        private int columns() {
            return Math.max(1, getWidth() / CELL_WIDTH);
        }

        @Override
        public void doLayout() {
            int columns = columns();
            for (int index = 0; index < getComponentCount(); index++) {
                getComponent(index).setBounds(index % columns * CELL_WIDTH,
                        index / columns * CELL_HEIGHT, CELL_WIDTH, CELL_HEIGHT);
            }
        }

        @Override
        public Dimension getPreferredSize() {
            int rows = (getComponentCount() + columns() - 1) / columns();
            return new Dimension(CELL_WIDTH, rows * CELL_HEIGHT);
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return new Dimension(3 * CELL_WIDTH, 2 * CELL_HEIGHT);
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) {
            return 24;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) {
            return Math.max(24, visible.height - CELL_HEIGHT);
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }
}
