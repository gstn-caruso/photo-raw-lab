package photorawlab.app;

import java.nio.file.Path;
import java.nio.file.Files;
import java.util.prefs.Preferences;
import javax.swing.SwingUtilities;
import photorawlab.domain.RawImageDecoder;

public final class Main {

    public static void main(String[] args) {
        DesktopScale.configure();
        start(args, new LibRawDecoder(), new PreferencesLastDirectory(Preferences.userNodeForPackage(Main.class)));
    }

    static void start(String[] args, RawImageDecoder decoder, LastDirectory lastDirectory) {
        SwingUtilities.invokeLater(() -> {
            RawViewerFrame window = new RawViewerFrame(decoder, lastDirectory);
            window.setVisible(true);
            if (args.length > 0) {
                Path path = Path.of(args[0]);
                if (Files.isDirectory(path)) window.openDirectory(path);
                else window.openRaw(path);
            } else window.openStartupDirectory();
        });
    }
}
