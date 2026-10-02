package photorawlab.app;

import java.nio.file.Path;
import javax.swing.SwingUtilities;

public final class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            RawViewerFrame window = new RawViewerFrame(new LibRawDecoder());
            window.setVisible(true);
            if (args.length > 0) {
                window.openRaw(Path.of(args[0]));
            }
        });
    }
}
