package {{package}}.game;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

public final class DesktopLauncher {

    private static final int WINDOW_WIDTH = 1280;
    private static final int WINDOW_HEIGHT = 720;
    private static final int COLOR_BITS_PER_CHANNEL = 8;
    private static final int DEPTH_BITS = 16;
    private static final int STENCIL_BITS = 0;
    private static final int MSAA_SAMPLES = 4;

    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("{{name}}");
        config.setWindowedMode(WINDOW_WIDTH, WINDOW_HEIGHT);
        config.useVsync(true);
        config.setBackBufferConfig(
            COLOR_BITS_PER_CHANNEL,
            COLOR_BITS_PER_CHANNEL,
            COLOR_BITS_PER_CHANNEL,
            COLOR_BITS_PER_CHANNEL,
            DEPTH_BITS,
            STENCIL_BITS,
            MSAA_SAMPLES);
        new Lwjgl3Application(new {{class}}Game(), config);
    }
}
