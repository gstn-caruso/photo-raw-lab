package photorawlab.app;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.plaf.basic.BasicArrowButton;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.plaf.basic.BasicGraphicsUtils;
import javax.swing.plaf.basic.BasicScrollBarUI;

final class AppPalette {
    static final Color CHROME = new Color(0x333333);
    static final Color PANEL = new Color(0x4d4d4d);
    static final Color TILE = new Color(0x6b6b6b);
    static final Color HIGHLIGHT = new Color(0x929292);
    static final Color TEXT = new Color(0xd6d6d6);
    static final Color BORDER = new Color(0x404040);

    private AppPalette() {}

    static void styleButton(JButton button, Color background) {
        button.setUI(new BasicButtonUI() {
            @Override
            protected void paintButtonPressed(Graphics graphics, AbstractButton button) {
                graphics.setColor(HIGHLIGHT);
                graphics.fillRect(0, 0, button.getWidth(), button.getHeight());
            }

            @Override
            protected void paintText(Graphics graphics, AbstractButton button, Rectangle bounds, String text) {
                graphics.setColor(TEXT);
                BasicGraphicsUtils.drawStringUnderlineCharAt(graphics, text,
                        button.getDisplayedMnemonicIndex(), bounds.x,
                        bounds.y + graphics.getFontMetrics().getAscent());
            }

            @Override
            protected void paintFocus(Graphics graphics, AbstractButton button, Rectangle view,
                    Rectangle text, Rectangle icon) {
                graphics.setColor(TEXT);
                graphics.drawRect(3, 3, button.getWidth() - 7, button.getHeight() - 7);
            }
        });
        button.setBackground(background);
        button.setForeground(TEXT);
        button.setOpaque(true);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));
    }

    static void styleScrollPane(JScrollPane scroll) {
        scroll.setBorder(BorderFactory.createLineBorder(BORDER));
        scroll.setBackground(PANEL);
        scroll.getViewport().setBackground(PANEL);
        styleScrollBar(scroll.getVerticalScrollBar());
        styleScrollBar(scroll.getHorizontalScrollBar());
    }

    private static void styleScrollBar(JScrollBar scrollBar) {
        scrollBar.setUI(new BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                trackColor = CHROME;
                thumbColor = TILE;
                thumbHighlightColor = HIGHLIGHT;
                thumbLightShadowColor = BORDER;
                thumbDarkShadowColor = BORDER;
            }

            @Override
            protected JButton createDecreaseButton(int direction) {
                return new BasicArrowButton(direction, PANEL, BORDER, TEXT, TILE);
            }

            @Override
            protected JButton createIncreaseButton(int direction) {
                return new BasicArrowButton(direction, PANEL, BORDER, TEXT, TILE);
            }
        });
        scrollBar.setBackground(CHROME);
    }
}
