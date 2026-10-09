package photorawlab.app;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.prefs.Preferences;
import java.util.concurrent.ExecutionException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.plaf.basic.BasicProgressBarUI;
import photorawlab.domain.RawImageDecoder;

public final class RawViewerFrame extends JFrame {
    private final RawImageDecoder decoder;
    private final LastDirectory lastDirectory;
    private final RawImagePanel imagePanel = new RawImagePanel();
    private final DirectoryMosaicPanel mosaic = new DirectoryMosaicPanel(this::openRaw);
    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final JButton openButton = new JButton("Abrir archivo…");
    private final JButton directoryButton = new JButton("Abrir carpeta…");
    private final JButton backButton = new JButton("Volver al mosaico");
    private final Object decodeLock = new Object();
    private final JLabel status = new JLabel("Elegí una fotografía RAW");
    private final JProgressBar progress = new JProgressBar();
    private boolean loading;
    private boolean disposed;
    private volatile long generation;
    private Path directory;
    private boolean mosaicComplete;

    public RawViewerFrame(RawImageDecoder decoder) {
        this(decoder, new PreferencesLastDirectory(Preferences.userNodeForPackage(Main.class)));
    }

    public RawViewerFrame(RawImageDecoder decoder, LastDirectory lastDirectory) {
        super("Photo RAW Lab");
        this.decoder = decoder;
        this.lastDirectory = lastDirectory;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1000, 700);
        setLocationByPlatform(true);
        JPanel toolbar = new JPanel(new BorderLayout(12, 0));
        JPanel actions = new JPanel();
        toolbar.setBackground(AppPalette.CHROME);
        actions.setBackground(AppPalette.CHROME);
        content.setBackground(AppPalette.PANEL);
        status.setForeground(AppPalette.TEXT);
        AppPalette.styleButton(openButton, AppPalette.PANEL);
        AppPalette.styleButton(directoryButton, AppPalette.PANEL);
        AppPalette.styleButton(backButton, AppPalette.PANEL);
        progress.setUI(new BasicProgressBarUI());
        progress.setBackground(AppPalette.CHROME);
        progress.setForeground(AppPalette.HIGHLIGHT);
        progress.setBorder(BorderFactory.createLineBorder(AppPalette.BORDER));
        actions.add(openButton);
        actions.add(directoryButton);
        actions.add(backButton);
        backButton.setEnabled(false);
        toolbar.add(actions, BorderLayout.WEST);
        toolbar.add(status, BorderLayout.CENTER);
        toolbar.add(progress, BorderLayout.EAST);
        progress.setVisible(false);
        add(toolbar, BorderLayout.NORTH);
        content.add(imagePanel, "photo");
        content.add(mosaic, "mosaic");
        add(content, BorderLayout.CENTER);
        openButton.addActionListener(event -> chooseRaw());
        directoryButton.addActionListener(event -> chooseDirectory());
        backButton.addActionListener(event -> showMosaic());
    }

    private void chooseRaw() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Fotografías RAW",
                RawDirectory.extensions()));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            openRaw(chooser.getSelectedFile().toPath());
        }
    }

    private void chooseDirectory() {
        pickDirectory().ifPresent(this::openDirectory);
    }

    private Optional<Path> pickDirectory() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            return Optional.of(chooser.getSelectedFile().toPath());
        }
        return Optional.empty();
    }

    public void openStartupDirectory() {
        requireEdt();
        new StartupDirectory(lastDirectory).select(this::pickDirectory).ifPresent(this::openDirectory);
    }

    public void openDirectory(Path path) {
        requireEdt();
        if (disposed) return;
        mosaic.showFiles(List.of());
        loadDirectory(path);
    }

    private void loadDirectory(Path path) {
        requireEdt();
        if (disposed) return;
        long request = ++generation;
        directory = path;
        mosaicComplete = false;
        imagePanel.setImage(null);
        cards.show(content, "mosaic");
        backButton.setEnabled(false);
        mosaic.refreshEntries(List.of());
        beginLoading("Leyendo " + path + "…");
        new SwingWorker<Integer, PreviewUpdate>() {
            @Override protected Integer doInBackground() throws Exception {
                List<RawDirectory.Entry> files = new RawDirectory().snapshot(path);
                publish(new PreviewUpdate(files, null, null, null));
                PreviewRenderer renderer = new PreviewRenderer(decoder);
                for (var entry : files) {
                    Path file = entry.path();
                    if (generation != request) return files.size();
                    try {
                        BufferedImage preview;
                        synchronized (decodeLock) {
                            if (generation != request) return files.size();
                            preview = renderer.render(file);
                        }
                        publish(new PreviewUpdate(null, file, preview, null));
                    } catch (Exception error) {
                        publish(new PreviewUpdate(null, file, null, error.getMessage()));
                    }
                }
                return files.size();
            }

            @Override protected void process(List<PreviewUpdate> updates) {
                if (disposed || generation != request) return;
                for (PreviewUpdate update : updates) {
                    if (update.files() != null) {
                        lastDirectory.save(path);
                        mosaic.refreshEntries(update.files());
                    }
                    else if (update.image() != null) mosaic.showPreview(update.path(), update.image());
                    else mosaic.showError(update.path(), update.error());
                }
            }

            @Override protected void done() {
                try {
                    if (disposed || generation != request) return;
                    int count = get();
                    mosaicComplete = true;
                    status.setText(count == 0 ? "No hay fotografías RAW en " + path : path + " · " + count + " fotografías RAW");
                } catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException error) {
                    status.setText("No se pudo leer la carpeta: " + error.getCause().getMessage());
                } finally { finishLoading(request); }
            }
        }.execute();
    }

    private record PreviewUpdate(List<RawDirectory.Entry> files, Path path, BufferedImage image, String error) {}

    private void showMosaic() {
        if (!mosaicComplete) {
            loadDirectory(directory);
            return;
        }
        ++generation;
        imagePanel.setImage(null);
        cards.show(content, "mosaic");
        backButton.setEnabled(false);
        status.setText(directory.toString());
        finishLoading(generation);
    }

    private void requireEdt() {
        if (!SwingUtilities.isEventDispatchThread()) throw new IllegalStateException("Abrir RAW requiere el EDT");
    }

    private void beginLoading(String message) {
        loading = true;
        openButton.setEnabled(false);
        progress.setIndeterminate(true);
        progress.setVisible(true);
        status.setText(message);
    }

    private void finishLoading(long request) {
        if (!disposed && generation == request) {
            loading = false;
            progress.setVisible(false);
            openButton.setEnabled(true);
        }
        if (disposed || generation == request) firePropertyChange("loading", true, false);
    }

    public void openRaw(Path path) {
        requireEdt();
        if (disposed) return;
        long request = ++generation;
        beginLoading("Cargando " + path.getFileName() + "…");
        backButton.setEnabled(directory != null);
        new SwingWorker<BufferedImage, Void>() {
            @Override
            protected BufferedImage doInBackground() throws Exception {
                synchronized (decodeLock) {
                    if (generation != request) return null;
                    return RawImagePanel.toBufferedImage(decoder.decode(path));
                }
            }

            @Override
            protected void done() {
                try {
                    if (disposed || generation != request) return;
                    imagePanel.setImage(get());
                    cards.show(content, "photo");
                    status.setText(path.getFileName().toString());
                } catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException error) {
                    status.setText("No se pudo abrir: " + error.getCause().getMessage());
                } finally {
                    finishLoading(request);
                }
            }
        }.execute();
    }

    @Override
    public void dispose() {
        disposed = true;
        ++generation;
        imagePanel.setImage(null);
        mosaic.showFiles(List.of());
        super.dispose();
    }
}
