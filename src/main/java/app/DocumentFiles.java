package app;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

public final class DocumentFiles {
    private DocumentFiles() {}

    public static String read(Path path) throws IOException {
        if (Files.size(path) > 2_000_000) {
            throw new IOException(I18n.text("file.tooLarge"));
        }
        String text = Files.readString(path, StandardCharsets.UTF_8);
        return text.startsWith("\uFEFF") ? text.substring(1) : text;
    }

    public static Path withExtension(Path path, String extension) {
        if (path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith("." + extension)) {
            return path;
        }
        return path.resolveSibling(path.getFileName() + "." + extension);
    }

    public static void write(Path path, byte[] bytes) throws IOException {
        Path target = path.toAbsolutePath();
        if (Files.isSymbolicLink(target)) {
            target = target.toRealPath();
        }
        Path temporary = Files.createTempFile(target.getParent(), ".puml-save-", ".tmp");
        try {
            Files.write(temporary, bytes);
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
