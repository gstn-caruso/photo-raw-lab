package photorawlab.adapters;

import static java.lang.foreign.ValueLayout.*;

import java.io.IOException;
import java.lang.foreign.*;
import java.nio.file.*;

/** FFM calls only the stable ABI exported by our compiled C bridge. */
public final class NativeBridge {
    private static final class Library {
        static final SymbolLookup SYMBOLS = load();

        static SymbolLookup load() {
            try (var in = NativeBridge.class.getResourceAsStream("/native/libphotoraw.so")) {
                if (in == null)
                    throw new IOException("Falta el puente nativo; ejecutar mvn compile");
                Path library = Files.createTempFile("photoraw-", ".so");
                Files.copy(in, library, StandardCopyOption.REPLACE_EXISTING);
                library.toFile().deleteOnExit();
                return SymbolLookup.libraryLookup(library, Arena.global());
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    private static final java.util.concurrent.ConcurrentHashMap<
                    String, java.lang.invoke.MethodHandle>
            FUNCTIONS = new java.util.concurrent.ConcurrentHashMap<>();

    public static Object call(String name, FunctionDescriptor descriptor, Object... args)
            throws IOException {
        try {
            return FUNCTIONS
                    .computeIfAbsent(
                            name,
                            n ->
                                    Linker.nativeLinker()
                                            .downcallHandle(
                                                    Library.SYMBOLS.find(n).orElseThrow(),
                                                    descriptor))
                    .invokeWithArguments(args);
        } catch (Throwable e) {
            if (e instanceof VirtualMachineError vm) throw vm;
            throw new IOException("Error nativo en " + name + ": " + e, e);
        }
    }

    public static String string(MemorySegment pointer) {
        return pointer.address() == 0 ? "" : pointer.reinterpret(32 * 1024 * 1024).getString(0);
    }

    public static void release(String function, MemorySegment p) throws IOException {
        if (p.address() != 0) call(function, FunctionDescriptor.ofVoid(ADDRESS), p);
    }
}
