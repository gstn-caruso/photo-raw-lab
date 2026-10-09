package photorawlab.app;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import photorawlab.domain.GalleryPhoto;
import photorawlab.domain.LibraryFilter;
import photorawlab.domain.LibraryFilter.Facet;
import photorawlab.domain.LibraryFilter.Value;

final class MetadataFilterPanel extends JPanel {
    private final LibraryFilter filter;
    private final Runnable changed;
    private final Map<Facet, JList<Option>> lists = new EnumMap<>(Facet.class);
    private boolean updating;
    private record Option(Value value, long count) {
        @Override public String toString() { return (value == null ? "Todas" : value.label()) + " (" + count + ")"; }
    }

    MetadataFilterPanel(LibraryFilter filter, Runnable changed) {
        super(new BorderLayout(0, 4));
        this.filter = filter;
        this.changed = changed;
        setBackground(AppPalette.CHROME);
        setBorder(javax.swing.BorderFactory.createEmptyBorder(6, 8, 6, 8));
        var title = new JLabel("Filtro de biblioteca · Metadatos");
        title.setForeground(AppPalette.TEXT);
        add(title, BorderLayout.NORTH);
        var columns = new Columns();
        columns.setBackground(AppPalette.BORDER);
        add(columns);
        addFacet(columns, Facet.CAPTURE_DATE, "Fecha de captura");
        addFacet(columns, Facet.CAMERA, "Cámara");
        addFacet(columns, Facet.LENS, "Lente");
        addFacet(columns, Facet.FILE_TYPE, "Tipo de archivo");
    }

    private void addFacet(JPanel columns, Facet facet, String name) {
        var column = new JPanel(new BorderLayout(0, 4));
        column.setBackground(AppPalette.PANEL);
        column.setBorder(javax.swing.BorderFactory.createEmptyBorder(4, 6, 4, 6));
        var label = new JLabel(name);
        label.setForeground(AppPalette.TEXT);
        column.add(label, BorderLayout.NORTH);
        var list = new JList<Option>(new DefaultListModel<>());
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setBackground(AppPalette.PANEL);
        list.setForeground(AppPalette.TEXT);
        list.setFixedCellHeight(18);
        list.setVisibleRowCount(3);
        list.getAccessibleContext().setAccessibleName(name);
        label.setLabelFor(list);
        list.setCellRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> owner, Object value,
                    int index, boolean selected, boolean focus) {
                var component = super.getListCellRendererComponent(owner, value, index, selected, focus);
                component.setBackground(selected ? AppPalette.TILE : AppPalette.PANEL);
                component.setForeground(AppPalette.TEXT);
                return component;
            }
        });
        list.addListSelectionListener(event -> {
            if (!updating && !event.getValueIsAdjusting() && list.getSelectedValue() != null) {
                filter.select(facet, list.getSelectedValue().value());
                changed.run();
            }
        });
        var scroll = new JScrollPane(list);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        AppPalette.styleScrollPane(scroll);
        column.add(scroll);
        lists.put(facet, list);
        columns.add(column);
    }

    void refresh(List<GalleryPhoto> photos) {
        updating = true;
        try {
            for (var facet : Facet.values()) {
                var counts = filter.counts(photos, facet);
                var selected = filter.selection(facet).orElse(null);
                var values = new java.util.ArrayList<>(counts.keySet());
                if (!values.contains(Value.unknown())) values.add(Value.unknown());
                if (selected != null && !values.contains(selected)) values.add(selected);
                values.sort(java.util.Comparator.comparing(Value::label));
                var model = (DefaultListModel<Option>) lists.get(facet).getModel();
                model.clear();
                model.addElement(new Option(null, counts.values().stream().mapToLong(Long::longValue).sum()));
                int selectedIndex = 0;
                for (var value : values) {
                    model.addElement(new Option(value, counts.getOrDefault(value, 0L)));
                    if (value.equals(selected)) selectedIndex = model.size() - 1;
                }
                lists.get(facet).setSelectedIndex(selectedIndex);
            }
        } finally { updating = false; }
    }

    private static final class Columns extends JPanel {
        Columns() { super(new GridLayout(1, 4, 1, 1)); }
        private int columns() {
            int width = getParent() == null || getParent().getParent() == null ? 1080 : getParent().getParent().getWidth();
            return width >= 840 ? 4 : width >= 440 ? 2 : 1;
        }
        @Override public Dimension getPreferredSize() { return new Dimension(840, (4 / columns()) * 96); }
        @Override public void doLayout() {
            ((GridLayout) getLayout()).setRows(4 / columns());
            ((GridLayout) getLayout()).setColumns(columns());
            super.doLayout();
        }
    }
}
