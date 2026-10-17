package photorawlab.adapters;

import static java.lang.foreign.FunctionDescriptor.*;
import static java.lang.foreign.ValueLayout.*;

import photorawlab.domain.edit.*;

import java.io.IOException;
import java.lang.foreign.*;

public final class Optics implements AutoCloseable {
    private final Arena arena = Arena.ofConfined();
    private final MemorySegment handle;
    private final int width;
    private final MemorySegment row;

    public Optics(LensSettings s, int width, int height) throws IOException {
        this.width = width;
        row = arena.allocate((long) width * 24, 4);
        MemorySegment h = MemorySegment.NULL;
        try {
            if (s.profile() != null && s.mode() != LensSettings.Mode.OFF) {
                h =
                        (MemorySegment)
                                NativeBridge.call(
                                        "pr_optics_new",
                                        of(
                                                ADDRESS,
                                                ADDRESS,
                                                JAVA_FLOAT,
                                                JAVA_INT,
                                                JAVA_INT,
                                                JAVA_FLOAT,
                                                JAVA_FLOAT,
                                                JAVA_INT),
                                        arena.allocateFrom(s.profile().calibration()),
                                        (float) s.cameraCrop(),
                                        width,
                                        height,
                                        (float) s.focal(),
                                        (float) s.aperture(),
                                        (s.distortion() ? 1 : 0)
                                                | (s.chromatic() ? 2 : 0)
                                                | (s.vignetting() ? 4 : 0));
                if (h.address() == 0) throw new IOException("Perfil de lente guardado no válido");
            }
            handle = h;
        } catch (IOException | RuntimeException e) {
            arena.close();
            throw e;
        }
    }

    public int flags() throws IOException {
        return handle.address() == 0
                ? 0
                : (int) NativeBridge.call("pr_optics_flags", of(JAVA_INT, ADDRESS), handle);
    }

    public float[] row(int y) throws IOException {
        if (handle.address() != 0)
            NativeBridge.call(
                    "pr_optics_row",
                    ofVoid(ADDRESS, JAVA_INT, JAVA_INT, ADDRESS),
                    handle,
                    y,
                    width,
                    row);
        else
            for (int x = 0; x < width; x++)
                for (int c = 0; c < 3; c++) {
                    row.setAtIndex(JAVA_FLOAT, x * 6 + c * 2, x);
                    row.setAtIndex(JAVA_FLOAT, x * 6 + c * 2 + 1, y);
                }
        return row.toArray(JAVA_FLOAT);
    }

    public float[] vignette(LinearImage image) throws IOException {
        if (handle.address() == 0 || (flags() & 4) == 0) return image.pixels();
        try (Arena a = Arena.ofConfined()) {
            MemorySegment pixels = a.allocateFrom(JAVA_FLOAT, image.pixels());
            NativeBridge.call(
                    "pr_optics_color",
                    ofVoid(ADDRESS, ADDRESS, JAVA_INT, JAVA_INT),
                    handle,
                    pixels,
                    image.width(),
                    image.height());
            return pixels.toArray(JAVA_FLOAT);
        }
    }

    public void close() throws IOException {
        try {
            NativeBridge.release("pr_optics_free", handle);
        } finally {
            arena.close();
        }
    }
}
