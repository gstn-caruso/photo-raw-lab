package photorawlab.app;

import java.awt.Graphics;
import java.util.Locale;
import javax.swing.JButton;
import photorawlab.domain.GalleryPhoto;

final class PhotoTile extends JButton {
    private final String camera;
    private final String lens;
    private final String settings;
    private int index;

    PhotoTile(GalleryPhoto photo) {
        super(photo.filename());
        var metadata = photo.metadata();
        camera = metadata.camera().orElse("Cámara: Sin datos");
        lens = metadata.lens().orElse("Lente: Sin datos");
        settings = metadata.iso().map(value -> "ISO " + value).orElse("ISO —") + " · "
                + metadata.aperture().map(value -> "f/" + decimal(value)).orElse("f/—") + " · "
                + metadata.exposureSeconds().map(PhotoTile::exposure).orElse("Tiempo —") + " · "
                + metadata.focalLength().map(value -> decimal(value) + " mm").orElse("Focal —");
        AppPalette.styleButton(this, AppPalette.TILE);
        setVerticalTextPosition(TOP);
        setHorizontalTextPosition(CENTER);
        setVerticalAlignment(TOP);
        setIconTextGap(8);
        setEnabled(false);
        setToolTipText("Captura: " + metadata.captured().map(Object::toString).orElse("Sin datos")
                + " · Dimensiones: " + metadata.width().flatMap(width -> metadata.height()
                .map(height -> width + " × " + height)).orElse("Sin datos"));
        getAccessibleContext().setAccessibleDescription(camera + "; " + lens + "; " + settings);
    }

    void showIndex(int index) { this.index = index; repaint(); }

    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        graphics.setColor(AppPalette.TEXT);
        graphics.setFont(getFont().deriveFont(11f));
        graphics.drawString(Integer.toString(index), 8, 21);
        drawLine(graphics, camera, 226);
        drawLine(graphics, lens, 242);
        drawLine(graphics, settings, 260);
    }

    private void drawLine(Graphics graphics, String value, int baseline) {
        var metrics = graphics.getFontMetrics();
        String text = value;
        while (!text.isEmpty() && metrics.stringWidth(text) > getWidth() - 24) {
            text = text.substring(0, text.length() - 1);
        }
        if (!text.equals(value) && text.length() > 1) text = text.substring(0, text.length() - 1) + "…";
        graphics.drawString(text, 12, baseline);
    }

    private static String decimal(double value) { return String.format(Locale.ROOT, "%.1f", value).replaceFirst("\\.0$", ""); }

    private static String exposure(double seconds) {
        long denominator = Math.round(1 / seconds);
        if (seconds < 1 && denominator > 1 && 1.0 / denominator == seconds) {
            return "1/" + denominator + " s";
        }
        return java.math.BigDecimal.valueOf(seconds).stripTrailingZeros().toPlainString() + " s";
    }
}
