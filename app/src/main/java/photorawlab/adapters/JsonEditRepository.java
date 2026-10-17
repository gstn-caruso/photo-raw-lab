package photorawlab.adapters;

import com.fasterxml.jackson.databind.*;

import photorawlab.domain.edit.*;

import java.io.*;
import java.nio.channels.*;
import java.nio.file.*;
import java.security.*;
import java.util.HexFormat;

/** Optimistic revision checks plus an interprocess lock and atomic same-directory replacement. */
public final class JsonEditRepository implements EditRepository {
    private final ObjectMapper mapper =
            new ObjectMapper()
                    .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
                    .enable(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES)
                    .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);

    public record Document(int schema, String originalHash, Recipe recipe) {}

    public static Path sidecar(Path raw) {
        return raw.resolveSibling(raw.getFileName() + ".photoraw.json");
    }

    public Saved load(Path raw) throws IOException {
        String original = hash(raw);
        Path side = sidecar(raw);
        if (!Files.exists(side)) return new Saved(Recipe.DEFAULT, original, "");
        if (Files.size(side) > 32 * 1024 * 1024)
            throw new IOException("Archivo de ajustes demasiado grande: " + side);
        byte[] bytes = Files.readAllBytes(side);
        try {
            Document doc = mapper.readValue(bytes, Document.class);
            if (doc.schema() != 1 || doc.recipe() == null)
                throw new IOException("Versión de ajustes no compatible");
            if (!original.equals(doc.originalHash()))
                throw new IOException("El original cambió desde la edición: " + raw);
            return new Saved(doc.recipe(), original, hash(bytes));
        } catch (IllegalArgumentException e) {
            throw new IOException("Ajustes dañados: " + side, e);
        }
    }

    public Saved save(Path raw, Recipe recipe, Saved expected) throws IOException {
        Path side = sidecar(raw), lockPath = side.resolveSibling(side.getFileName() + ".lock");
        try (FileChannel channel =
                        FileChannel.open(
                                lockPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                FileLock lock = channel.tryLock()) {
            if (lock == null) throw new IOException("Otra instancia está guardando esta foto");
            if (!hash(raw).equals(expected.originalHash()))
                throw new IOException("El original cambió; ajustes pendientes conservados");
            String revision = Files.exists(side) ? hash(side) : "";
            if (!revision.equals(expected.revision()))
                throw new IOException("Ajustes modificados externamente; no se sobrescribieron");
            byte[] bytes =
                    mapper.writerWithDefaultPrettyPrinter()
                            .writeValueAsBytes(new Document(1, expected.originalHash(), recipe));
            Path temporary =
                    Files.createTempFile(side.toAbsolutePath().getParent(), ".photoraw-", ".tmp");
            try {
                try (FileChannel output = FileChannel.open(temporary, StandardOpenOption.WRITE)) {
                    java.nio.ByteBuffer buffer = java.nio.ByteBuffer.wrap(bytes);
                    while (buffer.hasRemaining()) output.write(buffer);
                    output.force(true);
                }
                // Recheck immediately before publication, including non-cooperating writers.
                if (!(Files.exists(side) ? hash(side) : "").equals(revision))
                    throw new IOException("Conflicto de guardado externo");
                Files.move(
                        temporary,
                        side,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
                try (FileChannel dir =
                        FileChannel.open(
                                side.toAbsolutePath().getParent(), StandardOpenOption.READ)) {
                    dir.force(true);
                }
                return new Saved(recipe, expected.originalHash(), hash(bytes));
            } finally {
                Files.deleteIfExists(temporary);
            }
        } catch (OverlappingFileLockException e) {
            throw new IOException("Otra instancia está guardando esta foto", e);
        }
    }

    public static String hash(Path path) throws IOException {
        MessageDigest digest = digest();
        try (InputStream in = Files.newInputStream(path)) {
            byte[] buffer = new byte[1024 * 1024];
            int n;
            while ((n = in.read(buffer)) != -1) digest.update(buffer, 0, n);
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static String hash(byte[] data) {
        return HexFormat.of().formatHex(digest().digest(data));
    }

    private static MessageDigest digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError(e);
        }
    }
}
