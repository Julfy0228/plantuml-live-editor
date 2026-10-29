package app;

import static org.junit.jupiter.api.Assertions.*;
import com.formdev.flatlaf.FlatDarkLaf;
import java.awt.Component;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;

class ControlsTest {
    @Test void controlsStayOnOneBaselineWithoutOverlappingAtDifferentWidths() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            FlatDarkLaf.setup();
            JButton minus = icon(ToolbarIcon.Kind.MINUS);
            JButton plus = icon(ToolbarIcon.Kind.PLUS);
            JButton fit = icon(ToolbarIcon.Kind.FIT);
            JButton actual = new JButton("1:1");
            JButton copy = new JButton("Копировать", new ToolbarIcon(ToolbarIcon.Kind.COPY));
            JButton save = new JButton("Сохранить изображение", new ToolbarIcon(ToolbarIcon.Kind.SAVE));
            DiagramControls panel = new DiagramControls(minus, new JLabel("100% · авто"), plus, fit, actual, copy, save);
            for (int width : new int[] {400, 550, 900, 400}) {
                panel.setSize(width, 50);
                panel.doLayout();
                for (Component component : panel.getComponents()) {
                    assertEquals(34, component.getHeight());
                    assertEquals(8, component.getY());
                    assertTrue(component.getX() >= 0);
                    assertTrue(component.getX() + component.getWidth() <= width);
                    for (Component other : panel.getComponents()) {
                        if (component != other) assertFalse(component.getBounds().intersects(other.getBounds()));
                    }
                }
                assertEquals(34, minus.getWidth());
                assertEquals(34, actual.getWidth());
                if (width == 400) assertNull(copy.getText());
                if (width == 900) assertEquals("Копировать", copy.getText());
                try {
                    BufferedImage image = new BufferedImage(width, 50, BufferedImage.TYPE_INT_RGB);
                    var graphics = image.createGraphics();
                    panel.paint(graphics);
                    graphics.dispose();
                    Files.createDirectories(Path.of("target/ui-check"));
                    ImageIO.write(image, "png", Path.of("target/ui-check/controls-" + width + ".png").toFile());
                } catch (Exception failure) { throw new RuntimeException(failure); }
            }
        });
    }

    private JButton icon(ToolbarIcon.Kind kind) { return new JButton(new ToolbarIcon(kind)); }

    @Test void searchCountsLiteralNonOverlappingMatchesWithCaseOption() {
        assertEquals(3, SearchCounts.literal("Bob bob BOB", "bob", false));
        assertEquals(1, SearchCounts.literal("Bob bob BOB", "bob", true));
        assertEquals(2, SearchCounts.literal("aaaaa", "aa", true));
        assertEquals(0, SearchCounts.literal("abc", "", false));
        assertEquals(2, SearchCounts.literal("ёж ЁЖ", "ёж", false));
    }

    @Test void searchStatusUsesRussianAndEnglishPluralRules() {
        Locale ru = Locale.forLanguageTag("ru");
        for (int n : new int[] {0, 5, 11, 12, 14, 25, 111, 112})
            assertEquals("Поиск · " + n + " совпадений", I18n.count("search.matches", n, ru));
        for (int n : new int[] {1, 21, 101})
            assertEquals("Поиск · " + n + " совпадение", I18n.count("search.matches", n, ru));
        for (int n : new int[] {2, 3, 4, 22, 104})
            assertEquals("Поиск · " + n + " совпадения", I18n.count("search.matches", n, ru));
        assertEquals("Search · 1 match", I18n.count("search.matches", 1, Locale.US));
        assertEquals("Search · 0 matches", I18n.count("search.matches", 0, Locale.US));
    }
}
