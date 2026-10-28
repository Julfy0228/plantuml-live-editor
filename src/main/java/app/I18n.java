package app;

import java.util.Locale;
import java.util.ResourceBundle;
import javax.swing.JComponent;
import javax.swing.UIManager;

public final class I18n {
    private static final Locale LANGUAGE = languageFor(Locale.getDefault());
    private static final ResourceBundle MESSAGES = messagesFor(LANGUAGE);

    private I18n() {}

    static Locale languageFor(Locale systemLocale) {
        return "ru".equals(systemLocale.getLanguage()) ? Locale.forLanguageTag("ru") : Locale.ENGLISH;
    }

    static ResourceBundle messagesFor(Locale locale) {
        return ResourceBundle.getBundle("app.messages", languageFor(locale),
                ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_PROPERTIES));
    }

    public static String text(String key, Object... arguments) {
        return String.format(LANGUAGE, MESSAGES.getString(key), arguments);
    }

    public static Locale locale() {
        return LANGUAGE;
    }

    public static void initializeLocale() {
        Locale.setDefault(LANGUAGE);
        JComponent.setDefaultLocale(LANGUAGE);
    }

    public static void installSwingDefaults() {
        initializeLocale();
        for (String key : MESSAGES.keySet()) {
            if (key.startsWith("swing.")) {
                UIManager.put(key.substring("swing.".length()), MESSAGES.getString(key));
            }
        }
    }
}

