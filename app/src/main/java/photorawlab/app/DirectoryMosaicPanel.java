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
import java.time.Instant;
import java.awt.FlowLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.SwingUtilities;
import photorawlab.domain.GalleryPhoto;
import photorawlab.domain.GallerySort;
import photorawlab.domain.LibraryFilter;

public final class DirectoryMosaicPanel extends JPanel {
    private final JPanel tiles = new LibraryGrid();
    private final Map<Path, PhotoTile> buttons = new LinkedHashMap<>();
    private final Consumer<Path> openPhoto;
    private List<RawDirectory.Entry> entries = List.of();
    private final LibraryFilter filter = new LibraryFilter();
    private final MetadataFilterPanel metadataFilters = new MetadataFilterPanel(filter, this::reorderTiles);
    private final JLabel count = new JLabel("0 de 0 fotos");
    private final JComboBox<SortOption> criterion = new JComboBox<>(new SortOption[] {
        new SortOption("Nombre de archivo", GallerySort.Criterion.NAME),
        new SortOption("Fecha de modificación", GallerySort.Criterion.MODIFIED),
        new SortOption("Tamaño", GallerySort.Criterion.SIZE)
    });
    private final JComboBox<String> direction = new JComboBox<>(new String[] {"Ascendente", "Descendente"});

    private record SortOption(String label, GallerySort.Criterion criterion) {
        @Override public String toString() { return label; }
    }

    public DirectoryMosaicPanel(Consumer<Path> openPhoto) {
        super(new BorderLayout());
        this.openPhoto = openPhoto;
        setBackground(AppPalette.PANEL);
        tiles.setBackground(AppPalette.BORDER);
        JScrollPane scroll = new JScrollPane(tiles);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        AppPalette.styleScrollPane(scroll);
        add(scroll);
        JPanel controls = new SortControls();
        controls.setBackground(AppPalette.CHROME);
        addSortControl(controls, "Ordenar por:", criterion);
        addSortControl(controls, "Orden:", direction);
        criterion.addActionListener(event -> reorderTiles());
        direction.addActionListener(event -> reorderTiles());
        add(controls, BorderLayout.SOUTH);
        count.setForeground(AppPalette.TEXT);
        controls.add(count);
        add(metadataFilters, BorderLayout.NORTH);
        metadataFilters.refresh(List.of());
    }

    public void showFiles(List<Path> paths) {
        showEntries(paths.stream().map(path -> new RawDirectory.Entry(path,
                new GalleryPhoto(path.getFileName().toString(), Instant.EPOCH, 0))).toList());
    }

    public void showEntries(List<RawDirectory.Entry> entries) {
        this.entries = List.copyOf(entries);
        filter.clear();
        tiles.removeAll();
        buttons.clear();
        for (var entry : entries) {
            Path path = entry.path();
            PhotoTile tile = new PhotoTile(entry.photo());
            tile.addActionListener(event -> openPhoto.accept(path));
            buttons.put(path, tile);
        }
        reorderTiles();
    }

    private void addSortControl(JPanel controls, String text, JComboBox<?> combo) {
        JLabel label = new JLabel(text);
        label.setForeground(AppPalette.TEXT);
        label.setLabelFor(combo);
        combo.getAccessibleContext().setAccessibleName(text);
        AppPalette.styleComboBox(combo);
        controls.add(label);
        controls.add(combo);
    }

    private void reorderTiles() {
        GallerySort sort = new GallerySort(((SortOption) criterion.getSelectedItem()).criterion(),
                direction.getSelectedIndex() == 0 ? GallerySort.Direction.ASCENDING : GallerySort.Direction.DESCENDING);
        tiles.removeAll();
        entries.stream().filter(entry -> filter.matches(entry.photo()))
                .sorted(java.util.Comparator.comparing(RawDirectory.Entry::photo, sort.comparator()))
                .forEach(entry -> {
                    PhotoTile tile = buttons.get(entry.path());
                    tile.showIndex(tiles.getComponentCount() + 1);
                    tiles.add(tile);
                });
        count.setText(tiles.getComponentCount() + " de " + entries.size() + " fotos");
        metadataFilters.refresh(entries.stream().map(RawDirectory.Entry::photo).toList());
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

    private static final class SortControls extends JPanel {
        SortControls() {
            super(new FlowLayout(FlowLayout.LEFT, 8, 5));
        }

        @Override public Dimension getPreferredSize() {
            Dimension singleRow = super.getPreferredSize();
            if (getParent() == null || getParent().getWidth() == 0) return singleRow;
            FlowLayout layout = (FlowLayout) getLayout();
            var insets = getInsets();
            var parentInsets = getParent().getInsets();
            int availableWidth = getParent().getWidth() - parentInsets.left - parentInsets.right
                    - insets.left - insets.right - 2 * layout.getHgap();
            int height = insets.top + insets.bottom + 2 * layout.getVgap();
            int rowWidth = 0, rowHeight = 0;
            for (Component control : getComponents()) {
                Dimension size = control.getPreferredSize();
                if (rowWidth > 0 && rowWidth + layout.getHgap() + size.width > availableWidth) {
                    height += rowHeight + layout.getVgap();
                    rowWidth = 0;
                    rowHeight = 0;
                }
                rowWidth += (rowWidth == 0 ? 0 : layout.getHgap()) + size.width;
                rowHeight = Math.max(rowHeight, size.height);
            }
            return new Dimension(singleRow.width, height + rowHeight);
        }
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
        private static final int CELL_HEIGHT = 280;

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
            JScrollPane scroll = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, this);
            if (scroll == null || scroll.getWidth() == 0) {
                return new Dimension(CELL_WIDTH, rowsForWidth(3 * CELL_WIDTH) * CELL_HEIGHT);
            }
            var insets = scroll.getInsets();
            int width = scroll.getWidth() - insets.left - insets.right;
            int height = scroll.getHeight() - insets.top - insets.bottom;
            if (scroll.getViewportBorder() != null) {
                var border = scroll.getViewportBorder().getBorderInsets(scroll);
                width -= border.left + border.right;
                height -= border.top + border.bottom;
            }
            if (rowsForWidth(width) * CELL_HEIGHT > height) {
                width -= scroll.getVerticalScrollBar().getPreferredSize().width;
            }
            return new Dimension(CELL_WIDTH, rowsForWidth(width) * CELL_HEIGHT);
        }

        private int rowsForWidth(int width) {
            int columns = Math.max(1, width / CELL_WIDTH);
            return (getComponentCount() + columns - 1) / columns;
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
