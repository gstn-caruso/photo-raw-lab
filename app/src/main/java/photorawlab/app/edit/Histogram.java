package photorawlab.app.edit;

import java.awt.*;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

public final class Histogram extends JPanel {
    private int[][] bins = new int[3][256];

    public Histogram() {
        setPreferredSize(new Dimension(280, 90));
        setBackground(new Color(35, 35, 35));
        setToolTipText("Histograma sRGB de la imagen editada; extremos indican recorte de salida");
    }

    public void setBins(int[][] data) {
        bins = data;
        repaint();
    }

    public static int[][] calculate(BufferedImage image) {
        int[][] b = new int[3][256];
        for (int y = 0; y < image.getHeight(); y++)
            for (int x = 0; x < image.getWidth(); x++) {
                int p = image.getRGB(x, y);
                b[0][p >> 16 & 255]++;
                b[1][p >> 8 & 255]++;
                b[2][p & 255]++;
            }
        return b;
    }

    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Color[] colors = {
            new Color(255, 110, 110), new Color(100, 220, 140), new Color(110, 160, 255)
        };
        int max = 1;
        for (var b : bins) for (int n : b) max = Math.max(max, n);
        for (int c = 0; c < 3; c++) {
            g.setColor(colors[c]);
            for (int x = 1; x < 256; x++)
                g.drawLine(
                        (x - 1) * getWidth() / 256,
                        getHeight() - bins[c][x - 1] * getHeight() / max,
                        x * getWidth() / 256,
                        getHeight() - bins[c][x] * getHeight() / max);
        }
    }
}
