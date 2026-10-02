package photorawlab.app;

import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import photorawlab.domain.RawImageDecoder;
import photorawlab.domain.RgbImage;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;

public final class LibRawDecoder implements RawImageDecoder {
    // OpenMP workers can outlive a decode; their library must remain loaded until process exit.
    private static final ConcurrentHashMap<String, SymbolLookup> LIBRARIES = new ConcurrentHashMap<>();
    private final String library;

    public LibRawDecoder() {
        library = "libraw.so.23";
    }

    public LibRawDecoder(Path library) {
        this.library = Objects.requireNonNull(library, "library").toString();
    }

    @Override
    public RgbImage decode(Path path) throws IOException {
        Objects.requireNonNull(path, "path");
        try (Arena arena = Arena.ofConfined(); NativeSession session = new NativeSession(library)) {
            return session.develop(path, arena);
        } catch (IOException failure) {
            throw new IOException("Cannot decode " + path + ": " + failure.getMessage(), failure);
        } catch (IllegalArgumentException | UnsatisfiedLinkError failure) {
            throw new IOException("Cannot load LibRaw library " + library + ". Install LibRaw 0.21: "
                    + failure.getMessage(), failure);
        }
    }

    private static final class NativeSession implements AutoCloseable {
        private final MethodHandle openFile;
        private final MethodHandle unpack;
        private final MethodHandle process;
        private final MethodHandle makeBitmap;
        private final MethodHandle clearBitmap;
        private final MethodHandle closeHandler;
        private final MethodHandle outputBits;
        private final MethodHandle outputColor;
        private final MethodHandle errorText;
        private final MemorySegment handler;
        private MemorySegment bitmap = MemorySegment.NULL;

        private NativeSession(String library) throws IOException {
            SymbolLookup symbols = LIBRARIES.computeIfAbsent(library,
                    name -> SymbolLookup.libraryLookup(name, Arena.global()));
            MethodHandle init = function(symbols, "libraw_init", FunctionDescriptor.of(ADDRESS, JAVA_INT));
            openFile = function(symbols, "libraw_open_file", FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS));
            unpack = function(symbols, "libraw_unpack", FunctionDescriptor.of(JAVA_INT, ADDRESS));
            process = function(symbols, "libraw_dcraw_process", FunctionDescriptor.of(JAVA_INT, ADDRESS));
            makeBitmap = function(symbols, "libraw_dcraw_make_mem_image", FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS));
            clearBitmap = function(symbols, "libraw_dcraw_clear_mem", FunctionDescriptor.ofVoid(ADDRESS));
            closeHandler = function(symbols, "libraw_close", FunctionDescriptor.ofVoid(ADDRESS));
            outputBits = function(symbols, "libraw_set_output_bps", FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT));
            outputColor = function(symbols, "libraw_set_output_color", FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT));
            errorText = function(symbols, "libraw_strerror", FunctionDescriptor.of(ADDRESS, JAVA_INT));
            handler = (MemorySegment) invoke(init, 0);
            if (handler.address() == 0) {
                throw new IOException("LibRaw could not allocate a decoder");
            }
        }

        private RgbImage develop(Path path, Arena arena) throws IOException {
            MemorySegment filename = arena.allocateFrom(path.toAbsolutePath().toString());
            check("open_file", (int) invoke(openFile, handler, filename));
            invoke(outputBits, handler, 8);
            invoke(outputColor, handler, 1);
            check("unpack", (int) invoke(unpack, handler));
            check("dcraw_process", (int) invoke(process, handler));
            MemorySegment error = arena.allocate(JAVA_INT);
            bitmap = (MemorySegment) invoke(makeBitmap, handler, error);
            check("dcraw_make_mem_image", error.get(JAVA_INT, 0));
            return ProcessedBitmap.copyToRgb(bitmap);
        }

        private void check(String operation, int code) throws IOException {
            if (code != 0) {
                MemorySegment message = (MemorySegment) invoke(errorText, code);
                String description = message.address() == 0 ? "unknown error" : message.reinterpret(512).getString(0);
                throw new IOException("LibRaw " + operation + " failed (" + code + "): " + description);
            }
        }

        @Override
        public void close() throws IOException {
            try {
                if (bitmap.address() != 0) {
                    invoke(clearBitmap, bitmap);
                }
            } finally {
                invoke(closeHandler, handler);
            }
        }

        private static MethodHandle function(SymbolLookup symbols, String name, FunctionDescriptor signature) throws IOException {
            MemorySegment symbol = symbols.find(name).orElseThrow(() -> new IOException("LibRaw symbol missing: " + name));
            return Linker.nativeLinker().downcallHandle(symbol, signature);
        }

        private static Object invoke(MethodHandle function, Object... arguments) throws IOException {
            try {
                return function.invokeWithArguments(arguments);
            } catch (Error failure) {
                throw failure;
            } catch (Throwable failure) {
                throw new IOException("LibRaw native call failed", failure);
            }
        }
    }
}
