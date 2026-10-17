package photorawlab.adapters;

import static java.lang.foreign.FunctionDescriptor.*;
import static java.lang.foreign.ValueLayout.*;

import photorawlab.domain.edit.*;

import java.io.IOException;
import java.lang.foreign.*;
import java.nio.file.Path;

public final class LinearRawDecoder {
    public static final Object NATIVE_LOCK = new Object();

    public LinearImage decode(Path path, Adjustments a) throws IOException {
        synchronized (NATIVE_LOCK) {
            try (Arena arena = Arena.ofConfined()) {
                MemorySegment error = arena.allocate(JAVA_INT);
                MemorySegment image =
                        (MemorySegment)
                                NativeBridge.call(
                                        "pr_decode",
                                        of(ADDRESS, ADDRESS, JAVA_FLOAT, JAVA_FLOAT, ADDRESS),
                                        arena.allocateFrom(path.toAbsolutePath().toString()),
                                        (float) a.temperature(),
                                        (float) a.tint(),
                                        error);
                if (image.address() == 0)
                    throw new IOException(
                            "LibRaw: no se pudo revelar "
                                    + path
                                    + " ("
                                    + error.get(JAVA_INT, 0)
                                    + ")");
                try {
                    int w = (int) NativeBridge.call("pr_width", of(JAVA_INT, ADDRESS), image),
                            h = (int) NativeBridge.call("pr_height", of(JAVA_INT, ADDRESS), image);
                    MemorySegment rgb = arena.allocate(Math.multiplyExact((long) w * h, 12), 4);
                    if ((int)
                                    NativeBridge.call(
                                            "pr_to_working",
                                            of(JAVA_INT, ADDRESS, ADDRESS),
                                            image,
                                            rgb)
                            != 0) throw new IOException("No se pudo convertir a Rec.2020");
                    return new LinearImage(w, h, rgb.toArray(JAVA_FLOAT));
                } finally {
                    NativeBridge.release("pr_image_free", image);
                }
            }
        }
    }

    public static short[] srgb(LinearImage image) throws IOException {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment input = arena.allocateFrom(JAVA_FLOAT, image.pixels()),
                    output = arena.allocate((long) image.pixels().length * 2, 2);
            if ((int)
                            NativeBridge.call(
                                    "pr_to_srgb",
                                    of(JAVA_INT, ADDRESS, ADDRESS, JAVA_INT),
                                    input,
                                    output,
                                    image.width() * image.height())
                    != 0) throw new IOException("No se pudo convertir a sRGB");
            return output.toArray(JAVA_SHORT);
        }
    }
}
