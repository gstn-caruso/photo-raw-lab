package photorawlab.app;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import javax.swing.JPanel;
import photorawlab.domain.RgbImage;

public final class RawImagePanel extends JPanel {
    private BufferedImage image;

    public RawImagePanel() {
        setBackground(Color.DARK_GRAY);
    }

    public static BufferedImage toBufferedImage(RgbImage rgbImage) {
        BufferedImage result = new BufferedImage(rgbImage.width(), rgbImage.height(),
                BufferedImage.TYPE_INT_RGB);
        result.setRGB(0, 0, rgbImage.width(), rgbImage.height(), rgbImage.pixels(),
                0, rgbImage.width());
        return result;
    }

    public void setImage(BufferedImage image) {
        this.image = image;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        if (image == null) return;
        double scale = Math.min((double) getWidth() / image.getWidth(),
                (double) getHeight() / image.getHeight());
        int width = (int) Math.round(image.getWidth() * scale);
        int height = (int) Math.round(image.getHeight() * scale);
        graphics.drawImage(image, (getWidth() - width) / 2, (getHeight() - height) / 2,
                width, height, null);
    }
}
