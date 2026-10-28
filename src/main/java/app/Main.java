package app;

import static app.I18n.text;

import com.formdev.flatlaf.FlatDarkLaf;
import java.awt.Desktop;
import java.awt.GraphicsEnvironment;
import java.nio.file.Path;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public final class Main {
    public static void main(String[] arguments) {
        I18n.initializeLocale();
        System.setProperty("apple.awt.application.name", "PlantUML Live Editor");
        System.setProperty("PLANTUML_SECURITY_PROFILE", "SANDBOX");
        System.setProperty("PLANTUML_LIMIT_SIZE", "4096");
        if (GraphicsEnvironment.isHeadless()) {
            System.err.println(text("app.noDesktop"));
            return;
        }
        FlatDarkLaf.setup();
        I18n.installSwingDefaults();
        UIManager.put("Button.arc", 8);
        UIManager.put("Component.arc", 8);
        UIManager.put("ScrollBar.width", 12);
        JFrame.setDefaultLookAndFeelDecorated(true);
        SwingUtilities.invokeLater(() -> {
            EditorWindow window = new EditorWindow();
            window.setVisible(true);
            if (arguments.length > 0) {
                try {
                    window.open(Path.of(arguments[0]));
                } catch (RuntimeException exception) {
                    JOptionPane.showMessageDialog(window, exception.getMessage(), text("error.open"), JOptionPane.ERROR_MESSAGE);
                }
            }
            if (Desktop.isDesktopSupported()) {
                Desktop desktop = Desktop.getDesktop();
                if (desktop.isSupported(Desktop.Action.APP_OPEN_FILE)) {
                    desktop.setOpenFileHandler(event -> {
                        if (!event.getFiles().isEmpty()) {
                            SwingUtilities.invokeLater(() -> window.open(event.getFiles().get(0).toPath()));
                        }
                    });
                }
                if (desktop.isSupported(Desktop.Action.APP_QUIT_HANDLER)) {
                    desktop.setQuitHandler((event, response) -> SwingUtilities.invokeLater(() -> {
                        if (window.requestClose()) {
                            response.performQuit();
                        } else {
                            response.cancelQuit();
                        }
                    }));
                }
            }
        });
    }
}


