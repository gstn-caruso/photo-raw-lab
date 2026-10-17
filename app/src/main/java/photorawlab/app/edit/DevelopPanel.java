package photorawlab.app.edit;

import photorawlab.adapters.*;
import photorawlab.app.RawMetadataReader;
import photorawlab.application.EditSession;
import photorawlab.domain.PhotoMetadata;
import photorawlab.domain.edit.*;
import photorawlab.domain.edit.Point;
import photorawlab.engine.FloatDevelopmentEngine;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.*;

import javax.swing.*;

public final class DevelopPanel extends JPanel {
    private final ExecutorService io =
            Executors.newSingleThreadExecutor(r -> new Thread(r, "ediciones-io"));
    private final ThreadPoolExecutor render =
            new ThreadPoolExecutor(
                    1,
                    1,
                    0,
                    TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>(),
                    r -> {
                        Thread t = new Thread(r, "revelado-preview");
                        t.setDaemon(true);
                        return t;
                    });
    private final photorawlab.application.ExportQueue exports =
            new photorawlab.application.ExportQueue();
    private final EditSession session = new EditSession(new JsonEditRepository());
    private final FloatDevelopmentEngine engine = new FloatDevelopmentEngine();
    private final Consumer<String> status;
    private final Consumer<Path> navigate;
    private final Map<Path, EditHistory> histories = new HashMap<>();
    private final EditorCanvas canvas;
    private final Histogram histogram = new Histogram();
    private final LensPanel lenses;
    private final JPanel controls = new ControlsColumn();
    private final JSlider[] sliders = new JSlider[10], local = new JSlider[5];
    private final String[] names = {
        "Exposición",
        "Temperatura (relativa)",
        "Matiz",
        "Contraste",
        "Altas luces",
        "Sombras",
        "Blancos",
        "Negros",
        "Saturación",
        "Vibrancia"
    };
    private final int[] localIndices = {0, 3, 5, 4, 8};
    private final JComboBox<String> tool =
            new JComboBox<>(
                    new String[] {
                        "Desplazar",
                        "Recortar",
                        "Balance de blancos",
                        "Pincel",
                        "Borrador",
                        "Gradiente lineal",
                        "Gradiente radial"
                    });
    private final JComboBox<String> maskList = new JComboBox<>();
    private final JCheckBox invert = new JCheckBox("Invertir"),
            subtract = new JCheckBox("Restar trazo"),
            overlay = new JCheckBox("Ver máscara", true);
    private final JSlider radius = new JSlider(1, 250, 40),
            softness = new JSlider(0, 100, 70),
            opacity = new JSlider(1, 100, 100),
            angle = new JSlider(-450, 450, 0);
    private final JList<Path> strip = new JList<>(new DefaultListModel<>());
    private final JToggleButton before = new JToggleButton("Antes / después");
    private Path path;
    private EditHistory history;
    private PhotoMetadata metadata = PhotoMetadata.unknown();
    private LensfunCatalog catalog;
    private boolean detailMode;
    private boolean updating;
    private volatile boolean closed;
    private volatile long version;
    private long openVersion;
    private FloatDevelopmentEngine.Rendered current;
    private BufferedImage currentDisplay, beforeDisplay;
    private FloatDevelopmentEngine.Rendered beforeRender;
    private Runnable loaded = () -> {};
    private boolean exporting;

    public DevelopPanel(Consumer<String> status, Consumer<Path> navigate) {
        super(new BorderLayout());
        this.status = status;
        this.navigate = navigate;
        canvas =
                new EditorCanvas(
                        new EditorCanvas.Gestures() {
                            public void stroke(List<Point> p) {
                                draw(p);
                            }

                            public void crop(double x, double y, double w, double h) {
                                if (history == null) return;
                                var g = history.current().geometry();
                                commit(
                                        history.current()
                                                .withGeometry(
                                                        new Geometry(
                                                                g.x() + x * g.width(),
                                                                g.y() + y * g.height(),
                                                                w * g.width(),
                                                                h * g.height(),
                                                                g.quarterTurns(),
                                                                g.angle())));
                            }

                            public void whiteBalance(double u, double v) {
                                pickWhite(u, v);
                            }
                        });
        add(canvas, BorderLayout.CENTER);
        JPanel actions = new JPanel(new GridLayout(2, 0, 3, 3));
        button(actions, "◀", () -> step(-1));
        button(actions, "▶", () -> step(1));
        button(
                actions,
                "Ajustar",
                () -> {
                    detailMode = false;
                    canvas.fit();
                    schedule();
                });
        button(
                actions,
                "100 %",
                () -> {
                    detailMode = true;
                    canvas.actualPixels();
                    schedule();
                });
        actions.add(before);
        before.addActionListener(e -> compare());
        button(actions, "Deshacer", () -> historyMove(false));
        button(actions, "Rehacer", () -> historyMove(true));
        button(
                actions,
                "Restablecer",
                () -> {
                    if (history == null) return;
                    var lens = LensSettings.DEFAULT;
                    commit(
                            Recipe.DEFAULT.withLens(
                                    new LensSettings(
                                            lens.mode(),
                                            null,
                                            true,
                                            true,
                                            true,
                                            metadata.focalLength().orElse(0d),
                                            metadata.aperture().orElse(0d),
                                            history.current().lens().cameraCrop(),
                                            false)));
                });
        button(actions, "Exportar…", this::chooseExport);
        button(actions, "Cancelar exportación", exports::cancel);
        button(
                actions,
                "Reintentar guardado",
                () ->
                        io.execute(
                                () -> {
                                    try {
                                        session.retry();
                                        message("Ajustes guardados");
                                    } catch (Exception e) {
                                        message(e.getMessage());
                                    }
                                }));
        add(actions, BorderLayout.NORTH);
        controls.setLayout(new BoxLayout(controls, BoxLayout.Y_AXIS));
        controls.add(histogram);
        JPanel global = EditorStyle.column(), geometry = EditorStyle.column();
        controls.add(EditorStyle.accordion("Luz y color", global, true));
        for (int i = 0; i < sliders.length; i++) {
            int index = i;
            sliders[i] = new JSlider(-100, 100, 0);
            addSlider(global, names[i], sliders[i]);
            sliders[i].addChangeListener(
                    e -> {
                        if (!updating && history != null && !sliders[index].getValueIsAdjusting())
                            commit(
                                    history.current()
                                            .withAdjustments(
                                                    history.current()
                                                            .adjustments()
                                                            .with(
                                                                    index,
                                                                    index == 0
                                                                            ? sliders[index]
                                                                                            .getValue()
                                                                                    / 20d
                                                                            : sliders[index]
                                                                                    .getValue())));
                    });
        }
        controls.add(new JLabel("Herramienta"), 0);
        controls.add(tool, 1);
        tool.addActionListener(e -> canvas.setTool((String) tool.getSelectedItem()));
        button(
                geometry,
                "Rotar 90°",
                () -> {
                    if (history != null) {
                        var g = history.current().geometry();
                        commit(
                                history.current()
                                        .withGeometry(
                                                new Geometry(
                                                        0,
                                                        0,
                                                        1,
                                                        1,
                                                        (g.quarterTurns() + 1) % 4,
                                                        g.angle())));
                    }
                });
        addSlider(geometry, "Enderezar", angle);
        angle.addChangeListener(
                e -> {
                    if (!updating && history != null && !angle.getValueIsAdjusting()) {
                        var g = history.current().geometry();
                        commit(
                                history.current()
                                        .withGeometry(
                                                new Geometry(
                                                        g.x(),
                                                        g.y(),
                                                        g.width(),
                                                        g.height(),
                                                        g.quarterTurns(),
                                                        angle.getValue() / 10d)));
                    }
                });
        button(
                geometry,
                "Quitar recorte",
                () -> {
                    if (history != null) {
                        var g = history.current().geometry();
                        commit(
                                history.current()
                                        .withGeometry(
                                                new Geometry(
                                                        0, 0, 1, 1, g.quarterTurns(), g.angle())));
                    }
                });
        controls.add(EditorStyle.accordion("Encuadre y herramientas", geometry, true));
        lenses =
                new LensPanel(
                        task -> {
                            if (!closed) io.execute(task);
                        },
                        s -> {
                            if (!updating && history != null) commit(history.current().withLens(s));
                        });
        controls.add(EditorStyle.accordion("Correcciones de lente", lenses, false));
        JPanel masks = EditorStyle.column();
        controls.add(EditorStyle.accordion("Máscaras", masks, false));
        masks.add(maskList);
        maskList.addActionListener(
                e -> {
                    if (!updating) {
                        refreshLocal();
                        showOverlay();
                    }
                });
        button(masks, "Nueva máscara", this::newMask);
        button(
                masks,
                "Eliminar máscara",
                () -> {
                    if (history == null) return;
                    int i = maskList.getSelectedIndex();
                    if (i < 0) return;
                    var list = new ArrayList<>(history.current().masks());
                    list.remove(i);
                    commit(history.current().withMasks(list));
                });
        masks.add(overlay);
        overlay.addActionListener(e -> showOverlay());
        masks.add(invert);
        invert.addActionListener(
                e ->
                        editMask(
                                m ->
                                        new Mask(
                                                m.name(),
                                                invert.isSelected(),
                                                m.adjustments(),
                                                m.strokes())));
        masks.add(subtract);
        addSlider(masks, "Tamaño", radius);
        radius.addChangeListener(e -> canvas.setRadius(radius.getValue() / 1000d));
        addSlider(masks, "Suavidad", softness);
        addSlider(masks, "Opacidad", opacity);
        for (int i = 0; i < local.length; i++) {
            int index = i;
            local[i] = new JSlider(-100, 100, 0);
            addSlider(masks, names[localIndices[i]], local[i]);
            local[i].addChangeListener(
                    e -> {
                        if (!updating && !local[index].getValueIsAdjusting())
                            editMask(
                                    m ->
                                            new Mask(
                                                    m.name(),
                                                    m.inverted(),
                                                    m.adjustments()
                                                            .with(
                                                                    localIndices[index],
                                                                    index == 0
                                                                            ? local[index]
                                                                                            .getValue()
                                                                                    / 20d
                                                                            : local[index]
                                                                                    .getValue()),
                                                    m.strokes()));
                    });
        }
        JScrollPane scroll = new JScrollPane(controls);
        scroll.setPreferredSize(new Dimension(330, 0));
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        add(scroll, BorderLayout.EAST);
        strip.setLayoutOrientation(JList.HORIZONTAL_WRAP);
        strip.setVisibleRowCount(1);
        strip.setFixedCellWidth(140);
        strip.setFixedCellHeight(76);
        strip.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        strip.setCellRenderer(
                new DefaultListCellRenderer() {
                    public Component getListCellRendererComponent(
                            JList<?> l, Object value, int i, boolean selected, boolean focus) {
                        JLabel label =
                                (JLabel)
                                        super.getListCellRendererComponent(
                                                l, value, i, selected, focus);
                        Path p = (Path) value;
                        label.setText(p.getFileName().toString());
                        label.setToolTipText(p.toString());
                        Icon icon = thumbnails.get(p);
                        label.setIcon(icon);
                        label.setHorizontalTextPosition(JLabel.CENTER);
                        label.setVerticalTextPosition(JLabel.BOTTOM);
                        return label;
                    }
                });
        strip.addMouseListener(
                new java.awt.event.MouseAdapter() {
                    public void mouseClicked(java.awt.event.MouseEvent e) {
                        if (e.getClickCount() == 2 && strip.getSelectedValue() != null)
                            navigate.accept(strip.getSelectedValue());
                    }
                });
        bind(
                strip,
                "ENTER",
                "abrir",
                () -> {
                    if (strip.getSelectedValue() != null) navigate.accept(strip.getSelectedValue());
                });
        JScrollPane film = new JScrollPane(strip);
        film.setPreferredSize(new Dimension(0, 100));
        add(film, BorderLayout.SOUTH);
        EditorStyle.apply(controls);
        EditorStyle.apply(actions);
        EditorStyle.apply(strip);
        histogram.setBackground(new Color(0x292929));
        strip.setBackground(new Color(0x3b3b3b));
        strip.setForeground(Color.WHITE);
        bind(canvas, "LEFT", "anterior", () -> step(-1));
        bind(canvas, "RIGHT", "siguiente", () -> step(1));
        bind(canvas, "control Z", "deshacer", () -> historyMove(false));
        bind(canvas, "control Y", "rehacer", () -> historyMove(true));
        io.execute(
                () -> {
                    try {
                        LensfunCatalog c = new LensfunCatalog();
                        catalog = c;
                        SwingUtilities.invokeLater(
                                () -> {
                                    if (!closed) {
                                        catalog = c;
                                        lenses.catalog(c);
                                    }
                                });
                    } catch (Exception e) {
                        message("Lensfun: " + e.getMessage());
                    }
                });
    }

    private final Map<Path, Icon> thumbnails = new LinkedHashMap<>();

    public void files(List<Path> files, Function<Path, Icon> preview) {
        DefaultListModel<Path> model = (DefaultListModel<Path>) strip.getModel();
        List<Path> selected = strip.getSelectedValuesList();
        model.clear();
        thumbnails.clear();
        for (Path p : files) {
            model.addElement(p);
            Icon icon = preview.apply(p);
            if (icon != null) thumbnails.put(p, icon);
        }
        for (Path p : selected) {
            int i = model.indexOf(p);
            if (i >= 0) strip.addSelectionInterval(i, i);
        }
    }

    public void open(Path file, Runnable loaded) {
        this.loaded = loaded;
        detailMode = false;
        path = file;
        history = null;
        current = null;
        currentDisplay = beforeDisplay = null;
        beforeRender = null;
        before.setSelected(false);
        canvas.showImage(null, null);
        canvas.fit();
        controls.setVisible(false);
        long token = ++openVersion;
        ++version;
        render.getQueue().clear();
        int i = ((DefaultListModel<Path>) strip.getModel()).indexOf(file);
        if (i >= 0) {
            strip.addSelectionInterval(i, i);
            strip.ensureIndexIsVisible(i);
        }
        io.execute(
                () -> {
                    try {
                        var opened = session.open(file);
                        PhotoMetadata m = new RawMetadataReader().read(file);
                        Recipe recipe = opened.recipe();
                        if (recipe.equals(Recipe.DEFAULT)) {
                            var l = recipe.lens();
                            double crop =
                                    catalog == null ? 1 : catalog.cameraCrop(m.camera().orElse(""));
                            recipe =
                                    recipe.withLens(
                                            new LensSettings(
                                                    l.mode(),
                                                    null,
                                                    true,
                                                    true,
                                                    true,
                                                    m.focalLength().orElse(0d),
                                                    m.aperture().orElse(0d),
                                                    crop,
                                                    false));
                        }
                        if (catalog != null
                                && recipe.lens().mode() == LensSettings.Mode.AUTO
                                && recipe.lens().profile() == null) {
                            var candidates =
                                    catalog.automatic(m.camera().orElse(""), m.lens().orElse(""));
                            if (candidates.size() == 1) {
                                var l = recipe.lens();
                                recipe =
                                        recipe.withLens(
                                                new LensSettings(
                                                        l.mode(),
                                                        candidates.getFirst(),
                                                        l.distortion(),
                                                        l.chromatic(),
                                                        l.vignetting(),
                                                        l.focal(),
                                                        l.aperture(),
                                                        l.cameraCrop(),
                                                        l.adapted()));
                            }
                        }
                        if (!recipe.equals(opened.recipe())) {
                            try {
                                session.save(file, recipe);
                            } catch (java.io.IOException e) {
                                message(
                                        file.getFileName()
                                                + " · cambios pendientes: "
                                                + e.getMessage());
                            }
                        }
                        Recipe ready = recipe;
                        SwingUtilities.invokeLater(
                                () -> {
                                    if (closed || token != openVersion) return;
                                    metadata = m;
                                    history =
                                            histories.computeIfAbsent(
                                                    file, k -> new EditHistory(ready));
                                    controls.setVisible(true);
                                    refresh();
                                    schedule();
                                });
                    } catch (Exception e) {
                        SwingUtilities.invokeLater(
                                () -> {
                                    if (token == openVersion && !closed) {
                                        status.accept("No se pudo abrir: " + e.getMessage());
                                        loaded.run();
                                    }
                                });
                    }
                });
    }

    private void commit(Recipe recipe) {
        if (history == null || updating || history.current().equals(recipe)) return;
        history.commit(recipe);
        Path target = path;
        io.execute(
                () -> {
                    try {
                        session.save(target, recipe);
                        message(target.getFileName() + " · guardado");
                    } catch (Exception e) {
                        message(target.getFileName() + " · cambios pendientes: " + e.getMessage());
                    }
                });
        refresh();
        schedule();
    }

    private void historyMove(boolean redo) {
        if (history == null) return;
        Recipe r = redo ? history.redo() : history.undo();
        Path target = path;
        io.execute(
                () -> {
                    try {
                        session.save(target, r);
                        message("Guardado " + target.getFileName());
                    } catch (Exception e) {
                        message("Cambios pendientes: " + e.getMessage());
                    }
                });
        refresh();
        schedule();
    }

    private void refresh() {
        updating = true;
        Recipe r = history.current();
        for (int i = 0; i < 10; i++)
            sliders[i].setValue((int) Math.round(r.adjustments().value(i) * (i == 0 ? 20 : 1)));
        angle.setValue((int) Math.round(r.geometry().angle() * 10));
        int chosen = maskList.getSelectedIndex();
        maskList.removeAllItems();
        r.masks().forEach(m -> maskList.addItem(m.name()));
        if (maskList.getItemCount() > 0)
            maskList.setSelectedIndex(Math.max(0, Math.min(chosen, maskList.getItemCount() - 1)));
        lenses.show(r.lens(), metadata.camera().orElse(""), metadata.lens().orElse(""));
        updating = false;
        refreshLocal();
    }

    private void refreshLocal() {
        if (history == null) return;
        updating = true;
        int i = maskList.getSelectedIndex();
        Mask m = i >= 0 ? history.current().masks().get(i) : null;
        invert.setEnabled(m != null);
        invert.setSelected(m != null && m.inverted());
        for (int j = 0; j < local.length; j++) {
            local[j].setEnabled(m != null);
            local[j].setValue(
                    m == null
                            ? 0
                            : (int)
                                    Math.round(
                                            m.adjustments().value(localIndices[j])
                                                    * (j == 0 ? 20 : 1)));
        }
        updating = false;
    }

    private void newMask() {
        if (history == null) return;
        var masks = new ArrayList<>(history.current().masks());
        masks.add(new Mask("Máscara " + (masks.size() + 1), false, Adjustments.NEUTRAL, List.of()));
        commit(history.current().withMasks(masks));
        maskList.setSelectedIndex(masks.size() - 1);
        tool.setSelectedItem("Pincel");
    }

    private void editMask(UnaryOperator<Mask> action) {
        if (updating || history == null) return;
        int i = maskList.getSelectedIndex();
        if (i < 0) return;
        var masks = new ArrayList<>(history.current().masks());
        masks.set(i, action.apply(masks.get(i)));
        commit(history.current().withMasks(masks));
    }

    private void draw(List<Point> points) {
        if (history == null) return;
        String chosen = (String) tool.getSelectedItem();
        if (!List.of("Pincel", "Borrador", "Gradiente lineal", "Gradiente radial").contains(chosen))
            return;
        Mask.Kind kind =
                chosen.equals("Gradiente lineal")
                        ? Mask.Kind.LINEAR
                        : chosen.equals("Gradiente radial") ? Mask.Kind.RADIAL : Mask.Kind.BRUSH;
        List<Point> p =
                kind == Mask.Kind.BRUSH ? points : List.of(points.getFirst(), points.getLast());
        Mask.Stroke s =
                new Mask.Stroke(
                        kind,
                        subtract.isSelected() || chosen.equals("Borrador"),
                        radius.getValue() / 1000d,
                        softness.getValue() / 100d,
                        opacity.getValue() / 100d,
                        p);
        if (maskList.getSelectedIndex() < 0) {
            var masks = new ArrayList<>(history.current().masks());
            masks.add(
                    new Mask(
                            "Máscara " + (masks.size() + 1),
                            false,
                            Adjustments.NEUTRAL,
                            List.of(s)));
            commit(history.current().withMasks(masks));
            maskList.setSelectedIndex(masks.size() - 1);
        } else editMask(m -> m.append(s));
    }

    private void schedule() {
        if (history == null || closed) return;
        long token = ++version;
        Recipe recipe = history.current();
        Path file = path;
        int maxSide = detailMode ? 0 : 2048;
        render.getQueue().clear();
        status.accept("Revelando " + file.getFileName() + "…");
        render.execute(
                () -> {
                    try {
                        var result = engine.renderMapped(file, recipe, maxSide);
                        if (token != version) return;
                        BufferedImage display = ImageOutput.display(result.image());
                        int[][] bins = Histogram.calculate(display);
                        SwingUtilities.invokeLater(
                                () -> {
                                    if (token != version || closed) return;
                                    current = result;
                                    currentDisplay = display;
                                    histogram.setBins(bins);
                                    canvas.showImage(display, result);
                                    if (detailMode) canvas.actualPixels();
                                    before.setSelected(false);
                                    showOverlay();
                                    canvas.setToolTipText(
                                            result.originalWidth()
                                                    + " × "
                                                    + result.originalHeight()
                                                    + " · preview hasta 2048 px");
                                    status.accept(file.getFileName().toString());
                                    loaded.run();
                                });
                    } catch (Exception e) {
                        SwingUtilities.invokeLater(
                                () -> {
                                    if (token == version && !closed) {
                                        status.accept("Error de revelado: " + e.getMessage());
                                        loaded.run();
                                    }
                                });
                    }
                });
    }

    private void showOverlay() {
        if (current == null || currentDisplay == null || before.isSelected()) return;
        int selected = maskList.getSelectedIndex();
        if (!overlay.isSelected() || selected < 0 || history == null) {
            canvas.showImage(currentDisplay, current);
            return;
        }
        Mask mask = history.current().masks().get(selected);
        var mapped = current;
        var display = currentDisplay;
        long token = version;
        render.execute(
                () -> {
                    BufferedImage colored =
                            new BufferedImage(
                                    display.getWidth(),
                                    display.getHeight(),
                                    BufferedImage.TYPE_INT_RGB);
                    float[] coordinates = mapped.originalCoordinates();
                    double aspect = (double) mapped.originalWidth() / mapped.originalHeight();
                    for (int y = 0; y < display.getHeight(); y++)
                        for (int x = 0; x < display.getWidth(); x++) {
                            int i = (y * display.getWidth() + x) * 2, p = display.getRGB(x, y);
                            double a = mask.weight(coordinates[i], coordinates[i + 1], aspect) * .4;
                            int r = (int) ((p >> 16 & 255) * (1 - a) + 255 * a),
                                    g = (int) ((p >> 8 & 255) * (1 - a) + 55 * a),
                                    b = (int) ((p & 255) * (1 - a) + 55 * a);
                            colored.setRGB(x, y, r << 16 | g << 8 | b);
                        }
                    SwingUtilities.invokeLater(
                            () -> {
                                if (!closed
                                        && token == version
                                        && current == mapped
                                        && overlay.isSelected()
                                        && maskList.getSelectedIndex() == selected
                                        && !before.isSelected()) canvas.showImage(colored, mapped);
                            });
                });
    }

    private void compare() {
        if (current == null) return;
        if (!before.isSelected()) {
            showOverlay();
            return;
        }
        if (beforeDisplay != null) {
            canvas.showImage(beforeDisplay, beforeRender);
            return;
        }
        long token = version;
        Path file = path;
        render.execute(
                () -> {
                    try {
                        var r = engine.renderMapped(file, Recipe.DEFAULT, 2048);
                        var image = ImageOutput.display(r.image());
                        SwingUtilities.invokeLater(
                                () -> {
                                    if (token == version && !closed) {
                                        beforeDisplay = image;
                                        beforeRender = r;
                                        if (before.isSelected()) canvas.showImage(image, r);
                                    }
                                });
                    } catch (Exception e) {
                        message(e.getMessage());
                    }
                });
    }

    private void pickWhite(double u, double v) {
        if (current == null || history == null) return;
        int
                x =
                        Math.max(
                                0,
                                Math.min(
                                        current.image().width() - 1,
                                        (int) (u * current.image().width()))),
                y =
                        Math.max(
                                0,
                                Math.min(
                                        current.image().height() - 1,
                                        (int) (v * current.image().height()))),
                i = (y * current.image().width() + x) * 3;
        float[] pixels = current.image().pixels();
        double r = Math.max(.0001, pixels[i]),
                g = Math.max(.0001, pixels[i + 1]),
                b = Math.max(.0001, pixels[i + 2]);
        Adjustments a = history.current().adjustments();
        commit(
                history.current()
                        .withAdjustments(
                                a.with(
                                                1,
                                                Math.clamp(
                                                        a.temperature() + Math.log(b / r) / .012,
                                                        -100,
                                                        100))
                                        .with(
                                                2,
                                                Math.clamp(
                                                        a.tint()
                                                                + Math.log(g / Math.sqrt(r * b))
                                                                        / .004,
                                                        -100,
                                                        100))));
    }

    private void step(int direction) {
        var model = (DefaultListModel<Path>) strip.getModel();
        int i = model.indexOf(path) + direction;
        if (i >= 0 && i < model.size()) navigate.accept(model.get(i));
    }

    private void chooseExport() {
        if (path == null || history == null || exporting) return;
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        Object format =
                JOptionPane.showInputDialog(
                        this,
                        "Formato sRGB",
                        "Exportar selección",
                        JOptionPane.PLAIN_MESSAGE,
                        null,
                        ImageOutput.Format.values(),
                        ImageOutput.Format.JPEG);
        if (format == null) return;
        List<Path> selected = strip.getSelectedValuesList();
        if (selected.isEmpty()) selected = List.of(path);
        export(selected, chooser.getSelectedFile().toPath(), (ImageOutput.Format) format);
    }

    public void export(List<Path> files, Path destination, ImageOutput.Format format) {
        if (exporting) return;
        exporting = true;
        exports.beginBatch();
        io.execute(
                () -> {
                    List<photorawlab.application.ExportQueue.Job> jobs = new ArrayList<>();
                    List<String> errors = new ArrayList<>();
                    for (Path file : files) {
                        try {
                            jobs.add(
                                    new photorawlab.application.ExportQueue.Job(
                                            file, session.recipe(file)));
                        } catch (Exception e) {
                            errors.add(file.getFileName() + ": " + e.getMessage());
                        }
                    }
                    if (closed) return;
                    exports.submit(
                            jobs,
                            destination,
                            format,
                            errors,
                            this::message,
                            result ->
                                    SwingUtilities.invokeLater(
                                            () -> {
                                                exporting = false;
                                                if (closed) return;
                                                status.accept(
                                                        "Exportación: "
                                                                + result.completed()
                                                                + " archivos"
                                                                + (result.cancelled()
                                                                        ? " · cancelada"
                                                                        : "")
                                                                + (result.errors().isEmpty()
                                                                        ? ""
                                                                        : " · "
                                                                                + result.errors()
                                                                                        .size()
                                                                                + " errores"));
                                                if (!result.errors().isEmpty())
                                                    JOptionPane.showMessageDialog(
                                                            this,
                                                            String.join("\n", result.errors()),
                                                            "Errores de exportación",
                                                            JOptionPane.ERROR_MESSAGE);
                                            }));
                });
    }

    public void leave() {
        ++openVersion;
        ++version;
        render.getQueue().clear();
    }

    /** Drain autosaves before closing. A failed save leaves the window open for retry. */
    public void requestClose(Runnable close) {
        io.execute(
                () -> {
                    try {
                        session.retry();
                        SwingUtilities.invokeLater(close);
                    } catch (Exception e) {
                        message("No se cerró: hay cambios pendientes. " + e.getMessage());
                    }
                });
    }

    public void shutdown() {
        closed = true;
        ++version;
        ++openVersion;
        render.shutdown();
        exports.cancel();
        io.shutdown();
        exports.close();
    }

    private void message(String message) {
        SwingUtilities.invokeLater(
                () -> {
                    if (!closed) status.accept(message);
                });
    }

    private static void button(JPanel panel, String name, Runnable action) {
        JButton button = new JButton(name);
        button.addActionListener(e -> action.run());
        panel.add(button);
    }

    private static void addSlider(JPanel panel, String title, JSlider slider) {
        JLabel label = new JLabel(title);
        label.setLabelFor(slider);
        JPanel row = new JPanel(new BorderLayout());
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        JLabel value = new JLabel();
        Runnable display =
                () ->
                        value.setText(
                                title.equals("Exposición")
                                        ? String.format(
                                                java.util.Locale.ROOT,
                                                "%+.2f EV",
                                                slider.getValue() / 20d)
                                        : title.equals("Enderezar")
                                                ? String.format(
                                                        java.util.Locale.ROOT,
                                                        "%+.1f°",
                                                        slider.getValue() / 10d)
                                                : title.equals("Tamaño")
                                                        ? String.format(
                                                                java.util.Locale.ROOT,
                                                                "%.1f %%",
                                                                slider.getValue() / 10d)
                                                        : Integer.toString(slider.getValue()));
        slider.addChangeListener(e -> display.run());
        display.run();
        row.add(label, BorderLayout.WEST);
        row.add(value, BorderLayout.EAST);
        panel.add(row);
        slider.getAccessibleContext().setAccessibleName(title);
        slider.setMaximumSize(new Dimension(310, 35));
        panel.add(slider);
    }

    private static void bind(JComponent c, String key, String name, Runnable action) {
        c.getInputMap().put(KeyStroke.getKeyStroke(key), name);
        c.getActionMap()
                .put(
                        name,
                        new AbstractAction() {
                            public void actionPerformed(java.awt.event.ActionEvent e) {
                                action.run();
                            }
                        });
    }
}
