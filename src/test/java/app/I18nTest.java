package app;

import static org.junit.jupiter.api.Assertions.*;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Properties;
import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import org.junit.jupiter.api.Test;

class I18nTest {
    @Test
    void selectsSystemLanguageAndFallsBackToEnglish() {
        assertEquals("ru", I18n.languageFor(Locale.forLanguageTag("ru-RU")).getLanguage());
        assertEquals("ru", I18n.languageFor(Locale.forLanguageTag("ru-KZ")).getLanguage());
        assertEquals(Locale.ENGLISH, I18n.languageFor(Locale.US));
        assertEquals(Locale.ENGLISH, I18n.languageFor(Locale.FRENCH));
    }

    @Test
    void russianAndEnglishHaveTheSameTranslationKeys() throws Exception {
        Properties english = new Properties();
        Properties russian = new Properties();
        try (var source = new InputStreamReader(getClass().getResourceAsStream("/app/messages.properties"), StandardCharsets.UTF_8)) {
            english.load(source);
        }
        try (var source = new InputStreamReader(getClass().getResourceAsStream("/app/messages_ru.properties"), StandardCharsets.UTF_8)) {
            russian.load(source);
        }
        assertEquals(english.keySet(), russian.keySet());
        assertEquals("Save", I18n.messagesFor(Locale.US).getString("file.save"));
        assertEquals("Сохранить", I18n.messagesFor(Locale.forLanguageTag("ru")).getString("file.save"));
        assertEquals("Save", I18n.messagesFor(Locale.FRENCH).getString("file.save"));
    }

    @Test
    void fileChooserAndOptionButtonsUseTheApplicationLanguage() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            I18n.installSwingDefaults();
            JFileChooser chooser = new JFileChooser();
            assertEquals(I18n.locale(), chooser.getLocale());
            assertEquals(I18n.text("file.open"), chooser.getUI().getApproveButtonText(chooser));
            assertEquals(I18n.text("cancel"), UIManager.getString("FileChooser.cancelButtonText"));
            assertEquals(I18n.text("swing.FileChooser.fileNameLabelText"), UIManager.getString("FileChooser.fileNameLabelText"));
            assertEquals(I18n.text("swing.OptionPane.yesButtonText"), UIManager.getString("OptionPane.yesButtonText"));
        });
    }

    @Test
    void newDocumentMatchesTheMinimalPlantUmlExample() {
        assertEquals("@startuml\nAlice -> Bob : hello\n@enduml\n", EditorWindow.EXAMPLE);
    }
}

