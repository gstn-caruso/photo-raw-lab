package photorawlab.app;

import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import photorawlab.domain.RawImageDecoder;

public final class PreviewRenderer {
    private final RawImageDecoder decoder;

    public PreviewRenderer(RawImageDecoder decoder) { this.decoder = decoder; }

    public BufferedImage render(Path path) throws IOException {
        BufferedImage source = RawImagePanel.toBufferedImage(decoder.decode(path));
        double scale = Math.min(1, Math.min(240.0 / source.getWidth(), 180.0 / source.getHeight()));
        BufferedImage result = new BufferedImage(Math.max(1, (int) Math.round(source.getWidth() * scale)),
                Math.max(1, (int) Math.round(source.getHeight() * scale)), BufferedImage.TYPE_INT_RGB);
        var graphics = result.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.drawImage(source, 0, 0, result.getWidth(), result.getHeight(), null);
        } finally { graphics.dispose(); }
        return result;
    }
}
