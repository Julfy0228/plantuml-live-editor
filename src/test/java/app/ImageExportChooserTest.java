package app;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Path;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;

class ImageExportChooserTest {
    @Test
    void selectingFormatUpdatesFilenameAndBytesFormatTogether() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            I18n.installSwingDefaults();
            ImageExportChooser chooser = new ImageExportChooser("diagram", null);
            assertEquals(2, chooser.getChoosableFileFilters().length);
            assertEquals("png", chooser.extension());
            assertTrue(chooser.getSelectedFile().getName().endsWith(".png"));
            chooser.setFileFilter(chooser.getChoosableFileFilters()[1]);
            assertEquals("svg", chooser.extension());
            assertEquals("diagram.svg", chooser.getSelectedFile().getName());
            assertEquals(chooser.getSelectedFile().toPath(), chooser.outputPath());
            chooser.setFileFilter(chooser.getChoosableFileFilters()[0]);
            assertEquals("diagram.png", chooser.getSelectedFile().getName());
        });
    }

    @Test
    void avoidsDoubleExtensionsAndPreservesFolders() {
        assertEquals(Path.of("folder", "name.svg"), ImageExportChooser.outputPath(Path.of("folder", "name.PNG"), "svg"));
        assertEquals(Path.of("name.png"), ImageExportChooser.outputPath(Path.of("name.svg"), "png"));
        assertEquals(Path.of("name.svg"), ImageExportChooser.outputPath(Path.of("name"), "svg"));
        assertEquals(Path.of("name.test.png"), ImageExportChooser.outputPath(Path.of("name.test"), "png"));
    }
}

