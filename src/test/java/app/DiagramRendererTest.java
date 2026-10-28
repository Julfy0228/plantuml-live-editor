package app;

import static org.junit.jupiter.api.Assertions.*;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DiagramRendererTest {
    @ParameterizedTest
    @ValueSource(strings = {
            "@startuml\nAlice -> Bob: Привет\n@enduml",
            "@startuml\nclass User\nclass Account\nUser --> Account\n@enduml",
            "@startuml\n[Editor] --> [PlantUML]\n@enduml",
            "@startuml\nstart\n:Вставить код;\nstop\n@enduml",
            "@startjson\n{\"name\":\"editor\",\"offline\":true}\n@endjson"
    })
    void producesRealPngAndSvgWithoutExternalGraphviz(String source) throws Exception {
        var result = new DiagramRenderer(true).render(source);
        assertTrue(result.success(), result.message());
        assertNotNull(ImageIO.read(new ByteArrayInputStream(result.png())));
        String svg = new String(result.svg(), StandardCharsets.UTF_8);
        assertTrue(svg.contains("<svg"));
        assertTrue(result.image().getWidth() > 0);
        assertTrue(result.image().getHeight() > 0);
    }

    @Test
    void defaultEngineCanRenderClassDiagram() {
        var result = new DiagramRenderer().render("@startuml\nclass User\nclass Account\nUser --> Account\n@enduml");
        assertTrue(result.success(), result.message());
    }

    @Test
    void syntaxErrorIncludesSourceLine() {
        var result = new DiagramRenderer(true).render("@startuml\nclass User\nthis is invalid ???\n@enduml");
        assertFalse(result.success());
        assertEquals(2, result.errorLine());
        assertNull(result.png());
        assertNull(result.svg());
    }

    @Test
    void rejectsEmptyIncompleteAndOversizedInput() {
        DiagramRenderer renderer = new DiagramRenderer(true);
        assertFalse(renderer.render("   ").success());
        assertFalse(renderer.render("hello").success());
        assertFalse(renderer.render("x".repeat(500_001)).success());
    }

    @Test
    void multipleDiagramsAreExplicitlyReported() {
        var result = new DiagramRenderer(true).render("@startuml\nA -> B\n@enduml\n@startuml\nC -> D\n@enduml");
        assertTrue(result.success(), result.message());
        assertTrue(result.message().contains(I18n.text("render.multiple")));
    }
    @Test
    void incompleteGroupsDoNotPreventSubsequentValidRenders() {
        DiagramRenderer renderer = new DiagramRenderer(true);
        for (String fragment : new String[] {
                "@startuml\npackage A {\n[A] --> [B]\n@enduml",
                "@startuml\n}\n[A] --> [B]\n@enduml",
                "@startuml\npackage A {\n}\n}\n[A] --> [B]\n@enduml"
        }) {
            renderer.render(fragment);
            var recovered = renderer.render("@startuml\nAlice -> Bob : hello\n@enduml");
            assertTrue(recovered.success(), recovered.message());
        }
    }
}



