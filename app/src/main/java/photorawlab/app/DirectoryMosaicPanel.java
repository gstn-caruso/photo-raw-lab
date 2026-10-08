package photorawlab.app;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

public final class DirectoryMosaicPanel extends JPanel {
    private final JPanel tiles = new JPanel(new GridLayout(0, 3, 12, 12));
    private final Map<Path, JButton> buttons = new LinkedHashMap<>();
    private final Consumer<Path> openPhoto;

    public DirectoryMosaicPanel(Consumer<Path> openPhoto) {
        super(new BorderLayout());
        this.openPhoto = openPhoto;
        add(new JScrollPane(tiles));
    }

    public void showFiles(List<Path> paths) {
        tiles.removeAll();
        buttons.clear();
        for (Path path : paths) {
            JButton tile = new JButton(path.getFileName().toString());
            tile.setVerticalTextPosition(JButton.BOTTOM);
            tile.setHorizontalTextPosition(JButton.CENTER);
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
        tile.setIcon(new ImageIcon(preview));
        tile.setEnabled(true);
    }

    public void showError(Path path, String error) {
        buttons.get(path).setToolTipText("No se pudo abrir: " + error);
    }
}
