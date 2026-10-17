package photorawlab.app.edit;

import java.awt.*;

import javax.swing.*;

/** The tool column follows its viewport width; long lens names never widen the editor. */
final class ControlsColumn extends JPanel implements Scrollable {
    public Dimension getPreferredScrollableViewportSize() {
        return new Dimension(330, 600);
    }

    public int getScrollableUnitIncrement(Rectangle r, int axis, int direction) {
        return 24;
    }

    public int getScrollableBlockIncrement(Rectangle r, int axis, int direction) {
        return Math.max(24, r.height - 48);
    }

    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    public boolean getScrollableTracksViewportHeight() {
        return false;
    }
}
