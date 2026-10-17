package photorawlab.app.edit;

import java.awt.*;

import javax.swing.*;

/** Quiet photographic surround, with the clear form groups and accordions of the reference. */
final class EditorStyle {
    static final Color SURFACE = new Color(0xf5f5f5),
            INK = new Color(0x292929),
            LINE = new Color(0xd6d6d6),
            ACCENT = new Color(0x1473e6);

    static JPanel column() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(SURFACE);
        p.setBorder(BorderFactory.createEmptyBorder(8, 12, 12, 12));
        return p;
    }

    static JPanel accordion(String title, JComponent body, boolean expanded) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(SURFACE);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        JToggleButton heading = new JToggleButton((expanded ? "▾  " : "▸  ") + title, expanded);
        heading.setHorizontalAlignment(SwingConstants.LEFT);
        heading.setFont(new Font(Font.DIALOG, Font.BOLD, 14));
        heading.setBackground(SURFACE);
        heading.setForeground(INK);
        heading.setFocusPainted(true);
        heading.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(1, 0, 0, 0, LINE),
                        BorderFactory.createEmptyBorder(11, 12, 11, 12)));
        panel.add(heading, BorderLayout.NORTH);
        panel.add(body, BorderLayout.CENTER);
        body.setVisible(expanded);
        heading.addActionListener(
                e -> {
                    body.setVisible(heading.isSelected());
                    heading.setText((heading.isSelected() ? "▾  " : "▸  ") + title);
                    panel.revalidate();
                });
        return panel;
    }

    static void apply(Component c) {
        c.setFont(new Font(Font.DIALOG, Font.PLAIN, 13));
        if (c instanceof JComponent j) j.setAlignmentX(Component.LEFT_ALIGNMENT);
        c.setForeground(INK);
        if (c instanceof JPanel || c instanceof JSlider || c instanceof JCheckBox)
            c.setBackground(SURFACE);
        if (c instanceof JLabel label) label.setBorder(BorderFactory.createEmptyBorder(5, 0, 3, 0));
        if (c instanceof AbstractButton b) {
            b.setUI(new javax.swing.plaf.basic.BasicButtonUI());
            b.setOpaque(true);
            b.setBackground(SURFACE);
            b.setBorder(
                    BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(LINE),
                            BorderFactory.createEmptyBorder(7, 9, 7, 9)));
            if (b instanceof JCheckBox) b.setUI(new javax.swing.plaf.basic.BasicCheckBoxUI());
            if (b instanceof JToggleButton && !(b instanceof JCheckBox))
                b.setFont(new Font(Font.DIALOG, Font.BOLD, 13));
        }
        if (c instanceof JSlider slider) {
            slider.setUI(
                    new javax.swing.plaf.basic.BasicSliderUI(slider) {
                        public void paintTrack(Graphics g) {
                            g.setColor(LINE);
                            g.fillRect(
                                    trackRect.x,
                                    trackRect.y + trackRect.height / 2 - 1,
                                    trackRect.width,
                                    3);
                        }

                        public void paintThumb(Graphics g) {
                            g.setColor(ACCENT);
                            g.fillOval(thumbRect.x + 2, thumbRect.y + 3, 12, 12);
                        }
                    });
        }
        if (c instanceof JButton b) {
            b.setFocusPainted(true);
            b.setMargin(new Insets(5, 9, 5, 9));
        }
        if (c instanceof JList<?> l) {
            l.setSelectionBackground(ACCENT);
            l.setSelectionForeground(Color.WHITE);
        }
        if (c instanceof Container container)
            for (Component child : container.getComponents()) apply(child);
    }
}
