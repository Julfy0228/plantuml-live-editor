package app;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.io.TempDirFactory;
import org.junit.jupiter.api.extension.AnnotatedElementContext;
import org.junit.jupiter.api.extension.ExtensionContext;

class DocumentFilesTest {
    @TempDir(factory = ProjectTempDirFactory.class) Path directory;

    public static final class ProjectTempDirFactory implements TempDirFactory {
        @Override
        public Path createTempDirectory(AnnotatedElementContext element, ExtensionContext context) throws Exception {
            Path base = Path.of("target", "test-work");
            Files.createDirectories(base);
            return Files.createTempDirectory(base, "files-");
        }
    }

    @Test
    void writesAndReplacesUnicodeWithoutChangingCodeFences() throws Exception {
        Path document = directory.resolve("схема.puml");
        String source = "```plantuml\n@startuml\nАлиса -> Боб: Привет\n@enduml\n```\n";
        DocumentFiles.write(document, source.getBytes(StandardCharsets.UTF_8));
        assertEquals(source, DocumentFiles.read(document));
        DocumentFiles.write(document, "@startuml\n@enduml\n".getBytes(StandardCharsets.UTF_8));
        assertEquals("@startuml\n@enduml\n", DocumentFiles.read(document));
        try (var entries = Files.list(directory)) {
            assertEquals(1, entries.count());
        }
    }

    @Test
    void stripsUtf8BomOnlyWhenReadingFiles() throws Exception {
        Path document = directory.resolve("bom.puml");
        Files.writeString(document, "\uFEFF@startuml\r\n@enduml\r\n");
        assertEquals("@startuml\r\n@enduml\r\n", DocumentFiles.read(document));
    }

    @Test
    void handlesExtensionCaseAndExistingSuffix() {
        assertEquals(Path.of("file.PUML"), DocumentFiles.withExtension(Path.of("file.PUML"), "puml"));
        assertEquals(Path.of("file.puml"), DocumentFiles.withExtension(Path.of("file"), "puml"));
        assertEquals(Path.of("image.svg"), DocumentFiles.withExtension(Path.of("image"), "svg"));
    }

    @Test
    void failedSaveKeepsExistingDocument() throws Exception {
        Path document = directory.resolve("existing.puml");
        Files.writeString(document, "original");
        assertThrows(Exception.class, () -> DocumentFiles.write(directory.resolve("missing/file.puml"), new byte[0]));
        assertEquals("original", Files.readString(document));
    }
}


