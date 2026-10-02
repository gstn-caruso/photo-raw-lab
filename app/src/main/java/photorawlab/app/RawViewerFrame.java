package photorawlab.app;

import java.awt.BorderLayout;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;
import photorawlab.domain.RawImageDecoder;

public final class RawViewerFrame extends JFrame {
    private final RawImageDecoder decoder;
    private final RawImagePanel imagePanel = new RawImagePanel();
    private final JButton openButton = new JButton("Abrir archivo…");
    private final JLabel status = new JLabel("Elegí una fotografía RAW");
    private final JProgressBar progress = new JProgressBar();
    private boolean loading;
    private boolean disposed;

    public RawViewerFrame(RawImageDecoder decoder) {
        super("Photo RAW Lab");
        this.decoder = decoder;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1000, 700);
        setLocationByPlatform(true);
        JPanel toolbar = new JPanel(new BorderLayout(12, 0));
        toolbar.add(openButton, BorderLayout.WEST);
        toolbar.add(status, BorderLayout.CENTER);
        toolbar.add(progress, BorderLayout.EAST);
        progress.setVisible(false);
        add(toolbar, BorderLayout.NORTH);
        add(imagePanel, BorderLayout.CENTER);
        openButton.addActionListener(event -> chooseRaw());
    }

    private void chooseRaw() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Fotografías RAW",
                "raw", "cr2", "cr3", "crw", "nef", "nrw", "arw", "dng", "kdc",
                "dcr", "orf", "rw2", "raf", "pef", "srw", "3fr", "fff", "iiq"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            openRaw(chooser.getSelectedFile().toPath());
        }
    }

    public void openRaw(Path path) {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("Abrir RAW requiere el EDT");
        }
        if (loading || disposed) return;
        loading = true;
        openButton.setEnabled(false);
        progress.setIndeterminate(true);
        progress.setVisible(true);
        status.setText("Cargando " + path.getFileName() + "…");
        new SwingWorker<BufferedImage, Void>() {
            @Override
            protected BufferedImage doInBackground() throws Exception {
                return RawImagePanel.toBufferedImage(decoder.decode(path));
            }

            @Override
            protected void done() {
                try {
                    if (disposed) return;
                    imagePanel.setImage(get());
                    status.setText(path.getFileName().toString());
                } catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException error) {
                    status.setText("No se pudo abrir: " + error.getCause().getMessage());
                } finally {
                    loading = false;
                    if (!disposed) {
                        progress.setVisible(false);
                        openButton.setEnabled(true);
                    }
                    RawViewerFrame.this.firePropertyChange("loading", true, false);
                }
            }
        }.execute();
    }

    @Override
    public void dispose() {
        disposed = true;
        super.dispose();
    }
}
