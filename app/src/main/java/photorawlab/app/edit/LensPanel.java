package photorawlab.app.edit;

import photorawlab.adapters.LensfunCatalog;
import photorawlab.domain.edit.*;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

import javax.swing.*;

/** Searches and calibration probes run on the I/O worker, with stale-result rejection. */
final class LensPanel extends JPanel {
    private final JComboBox<String> mode =
            new JComboBox<>(new String[] {"Automático", "Manual", "Desactivado"});
    private final JComboBox<String> maker = new JComboBox<>(), model = new JComboBox<>();
    private final JComboBox<LensProfile> profile = new JComboBox<>();
    private final JTextField query = new JTextField();
    private final JCheckBox adapted = new JCheckBox("Incluir otras monturas"),
            distortion = new JCheckBox("Distorsión"),
            chromatic = new JCheckBox("Aberración cromática"),
            vignetting = new JCheckBox("Viñeteo");
    private final JSpinner focal = new JSpinner(new SpinnerNumberModel(0d, 0d, 10000d, 1d)),
            aperture = new JSpinner(new SpinnerNumberModel(0d, 0d, 256d, .1d));
    private final JLabel status = new JLabel("Cargando Lensfun…");
    private final Executor io;
    private final Consumer<LensSettings> change;
    private LensfunCatalog catalog;
    private String camera = "", detected = "";
    private LensSettings settings = LensSettings.DEFAULT;
    private boolean updating;
    private long generation;
    private List<LensProfile> candidates = List.of();

    LensPanel(Executor io, Consumer<LensSettings> change) {
        this.io = io;
        this.change = change;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(8, 12, 12, 12));
        maker.setEditable(true);
        maker.setPrototypeDisplayValue("Fabricante de lente");
        model.setPrototypeDisplayValue("Modelo de lente");
        status.setMinimumSize(new Dimension(0, 24));
        status.setMaximumSize(new Dimension(300, 48));
        status.addPropertyChangeListener("text", e -> status.setToolTipText(status.getText()));
        row("Modo", mode);
        row("Fabricante", maker);
        row("Buscar modelo / focal", query);
        row("Modelo", model);
        row("Perfil", profile);
        add(adapted);
        add(distortion);
        add(chromatic);
        add(vignetting);
        row("Focal (mm; 0 = sin dato)", focal);
        row("Apertura (0 = sin dato)", aperture);
        add(status);
        mode.addActionListener(
                e -> {
                    if (updating) return;
                    LensSettings.Mode m = LensSettings.Mode.values()[mode.getSelectedIndex()];
                    settings =
                            new LensSettings(
                                    m,
                                    m == LensSettings.Mode.AUTO ? null : settings.profile(),
                                    settings.distortion(),
                                    settings.chromatic(),
                                    settings.vignetting(),
                                    number(focal),
                                    number(aperture),
                                    settings.cameraCrop(),
                                    adapted.isSelected());
                    if (m != LensSettings.Mode.AUTO) change.accept(settings);
                    search();
                });
        maker.addActionListener(e -> search());
        query.addActionListener(e -> search());
        javax.swing.Timer timer = new javax.swing.Timer(300, e -> search());
        timer.setRepeats(false);
        query.getDocument()
                .addDocumentListener(
                        new javax.swing.event.DocumentListener() {
                            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                                timer.restart();
                            }

                            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                                timer.restart();
                            }

                            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                                timer.restart();
                            }
                        });
        adapted.addActionListener(
                e -> {
                    if (updating) return;
                    settings =
                            new LensSettings(
                                    settings.mode(),
                                    null,
                                    settings.distortion(),
                                    settings.chromatic(),
                                    settings.vignetting(),
                                    number(focal),
                                    number(aperture),
                                    settings.cameraCrop(),
                                    adapted.isSelected());
                    change.accept(settings);
                    search();
                });
        model.addActionListener(
                e -> {
                    if (!updating) variants();
                });
        profile.addActionListener(
                e -> {
                    if (!updating && settings.mode() == LensSettings.Mode.MANUAL)
                        commit((LensProfile) profile.getSelectedItem());
                });
        for (JCheckBox box : List.of(distortion, chromatic, vignetting))
            box.addActionListener(
                    e -> {
                        if (!updating) commit(settings.profile());
                    });
        focal.addChangeListener(
                e -> {
                    if (!updating) {
                        commit(settings.profile());
                        availability();
                    }
                });
        aperture.addChangeListener(
                e -> {
                    if (!updating) {
                        commit(settings.profile());
                        availability();
                    }
                });
    }

    void catalog(LensfunCatalog catalog) {
        this.catalog = catalog;
        updating = true;
        maker.removeAllItems();
        maker.addItem("");
        catalog.manufacturers().forEach(maker::addItem);
        updating = false;
        search();
    }

    void show(LensSettings s, String camera, String detected) {
        ++generation;
        settings = s;
        this.camera = camera;
        this.detected = detected;
        updating = true;
        mode.setSelectedIndex(s.mode().ordinal());
        focal.setValue(s.focal());
        aperture.setValue(s.aperture());
        adapted.setSelected(s.adapted());
        distortion.setSelected(s.distortion());
        chromatic.setSelected(s.chromatic());
        vignetting.setSelected(s.vignetting());
        maker.setSelectedItem(s.profile() == null ? "" : s.profile().maker());
        model.removeAllItems();
        profile.removeAllItems();
        if (s.profile() != null) {
            model.addItem(s.profile().model());
            profile.addItem(s.profile());
        }
        updating = false;
        search();
    }

    private void commit(LensProfile p) {
        settings =
                new LensSettings(
                        settings.mode(),
                        p,
                        distortion.isSelected(),
                        chromatic.isSelected(),
                        vignetting.isSelected(),
                        number(focal),
                        number(aperture),
                        settings.cameraCrop(),
                        adapted.isSelected());
        change.accept(settings);
        availability();
    }

    private void search() {
        if (updating || catalog == null) return;
        long token = ++generation;
        boolean manual = settings.mode() == LensSettings.Mode.MANUAL;
        maker.setEnabled(manual);
        query.setEnabled(manual);
        model.setEnabled(manual);
        profile.setEnabled(manual);
        adapted.setEnabled(manual);
        if (settings.mode() == LensSettings.Mode.OFF) {
            status.setText("Correcciones desactivadas");
            distortion.setEnabled(false);
            chromatic.setEnabled(false);
            vignetting.setEnabled(false);
            return;
        }
        if (!manual && settings.profile() != null) {
            status.setText("Guardado: " + settings.profile().model());
            availability();
            return;
        }
        String manufacturer = Objects.toString(maker.getSelectedItem(), ""),
                text = query.getText(),
                cam = camera,
                det = detected;
        boolean other = adapted.isSelected();
        io.execute(
                () -> {
                    try {
                        List<LensProfile> found =
                                manual
                                        ? catalog.search(manufacturer, text, cam, other)
                                        : catalog.automatic(cam, det);
                        SwingUtilities.invokeLater(
                                () -> {
                                    if (token != generation) return;
                                    candidates = found;
                                    updating = true;
                                    model.removeAllItems();
                                    model.addItem("Elegir modelo…");
                                    found.stream()
                                            .map(LensProfile::model)
                                            .distinct()
                                            .forEach(model::addItem);
                                    if (settings.profile() != null) {
                                        model.addItem(settings.profile().model());
                                        model.setSelectedItem(settings.profile().model());
                                    }
                                    updating = false;
                                    if (!manual && found.size() == 1) {
                                        commit(found.getFirst());
                                        updating = true;
                                        profile.removeAllItems();
                                        profile.addItem(found.getFirst());
                                        updating = false;
                                        status.setText("Detectado: " + found.getFirst().model());
                                    } else {
                                        if (!manual) change.accept(settings);
                                        variants();
                                        status.setText(
                                                found.isEmpty()
                                                        ? "Sin coincidencia; usar manual / otras"
                                                                + " monturas"
                                                        : !manual
                                                                ? found.size()
                                                                        + " candidatas; elegir en"
                                                                        + " modo manual"
                                                                : found.size() + " perfiles");
                                    }
                                    availability();
                                });
                    } catch (Exception e) {
                        SwingUtilities.invokeLater(
                                () -> {
                                    if (token == generation) status.setText(e.getMessage());
                                });
                    }
                });
    }

    private void variants() {
        updating = true;
        profile.removeAllItems();
        Object selected = model.getSelectedItem();
        for (LensProfile p : candidates) if (p.model().equals(selected)) profile.addItem(p);
        if (settings.profile() != null && settings.profile().model().equals(selected)) {
            boolean existing = false;
            for (int i = 0; i < profile.getItemCount(); i++)
                if (profile.getItemAt(i).equals(settings.profile())) existing = true;
            if (!existing) profile.insertItemAt(settings.profile(), 0);
            profile.setSelectedItem(settings.profile());
        }
        updating = false;
        // Manual selection is explicit: a model selection selects its first calibration.
        if (settings.mode() == LensSettings.Mode.MANUAL
                && profile.getItemCount() > 0
                && (settings.profile() == null || !settings.profile().model().equals(selected)))
            commit((LensProfile) profile.getSelectedItem());
    }

    private void availability() {
        if (catalog == null) return;
        LensSettings expected = settings;
        io.execute(
                () -> {
                    try {
                        var a = catalog.available(expected);
                        SwingUtilities.invokeLater(
                                () -> {
                                    if (!settings.equals(expected)) return;
                                    boolean enabled = expected.mode() != LensSettings.Mode.OFF;
                                    distortion.setEnabled(enabled && a.distortion());
                                    chromatic.setEnabled(enabled && a.chromatic());
                                    vignetting.setEnabled(enabled && a.vignetting());
                                });
                    } catch (Exception e) {
                        SwingUtilities.invokeLater(() -> status.setText(e.getMessage()));
                    }
                });
    }

    private void row(String title, JComponent component) {
        add(new JLabel(title));
        component.setMaximumSize(new Dimension(310, 28));
        component.getAccessibleContext().setAccessibleName(title);
        add(component);
    }

    private static double number(JSpinner spinner) {
        return ((Number) spinner.getValue()).doubleValue();
    }
}
