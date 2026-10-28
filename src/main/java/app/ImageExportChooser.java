package app;

import java.io.File;
import java.nio.file.Path;
import java.util.Locale;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

public final class ImageExportChooser extends JFileChooser {
    private String selectedName;
    private final FileNameExtensionFilter png = new FileNameExtensionFilter(I18n.text("image.png"), "png");
    private final FileNameExtensionFilter svg = new FileNameExtensionFilter(I18n.text("image.svg"), "svg");

    public ImageExportChooser(String filename, Path directory) {
        setLocale(I18n.locale());
        setDialogTitle(I18n.text("image.save"));
        setApproveButtonText(I18n.text("file.save"));
        setAcceptAllFileFilterUsed(false);
        addChoosableFileFilter(png);
        addChoosableFileFilter(svg);
        setFileFilter(png);
        if (directory != null) {
            setCurrentDirectory(directory.toFile());
        }
        setSelectedFile(new File(getCurrentDirectory(), filename + ".png"));
        selectedName = getSelectedFile().getName();
        addPropertyChangeListener(SELECTED_FILE_CHANGED_PROPERTY, event -> {
            if (event.getNewValue() instanceof File selected && !selected.isDirectory()) {
                selectedName = selected.getName();
            }
        });
        addPropertyChangeListener(FILE_FILTER_CHANGED_PROPERTY, event -> {
            File selected = getSelectedFile();
            if (selected == null) {
                selected = new File(getCurrentDirectory(), selectedName);
            }
            setSelectedFile(outputPath(selected.toPath(), extension()).toFile());
        });
    }

    public String extension() {
        return getFileFilter() == svg ? "svg" : "png";
    }

    public Path outputPath() {
        return outputPath(getSelectedFile().toPath(), extension());
    }

    static Path outputPath(Path path, String extension) {
        String name = path.getFileName().toString();
        String lower = name.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".png") || lower.endsWith(".svg")) {
            name = name.substring(0, name.length() - 4);
        }
        return path.resolveSibling(name + "." + extension);
    }
}


