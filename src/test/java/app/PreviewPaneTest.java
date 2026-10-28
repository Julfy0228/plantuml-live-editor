package app;

import static org.junit.jupiter.api.Assertions.*;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;

class PreviewPaneTest {
    private static BufferedImage solid(int width, int height, Color color) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(color);
        graphics.fillRect(0, 0, width, height);
        graphics.dispose();
        return image;
    }

    private static void paint(PreviewPane preview, BufferedImage image) {
        Graphics2D graphics = image.createGraphics();
        preview.paint(graphics);
        graphics.dispose();
    }

    private static int[] pixels(BufferedImage image) {
        return image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
    }

    @Test
    void repaintsBackgroundInsteadOfLeavingOldDiagramAndBadgePixels() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            PreviewPane preview = new PreviewPane(label -> {});
            preview.scrollPane().getViewport().setExtentSize(new java.awt.Dimension(500, 400));
            preview.setSize(500, 400);
            BufferedImage reused = solid(500, 400, Color.MAGENTA);
            preview.setImage(solid(320, 230, Color.GREEN));
            preview.actualSize();
            preview.setStale(true);
            paint(preview, reused);
            preview.setImage(solid(80, 50, Color.BLUE));
            paint(preview, reused);
            BufferedImage fresh = new BufferedImage(500, 400, BufferedImage.TYPE_INT_ARGB);
            paint(preview, fresh);
            assertArrayEquals(pixels(fresh), pixels(reused));
            assertEquals(preview.getBackground().getRGB(), reused.getRGB(4, 4));
        });
    }

    @Test
    void clearingImageAlsoErasesThePreviousRaster() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            PreviewPane preview = new PreviewPane(label -> {});
            preview.setSize(500, 400);
            preview.setImage(solid(320, 230, Color.GREEN));
            preview.actualSize();
            BufferedImage reused = new BufferedImage(500, 400, BufferedImage.TYPE_INT_ARGB);
            paint(preview, reused);
            preview.clear();
            paint(preview, reused);
            BufferedImage fresh = new BufferedImage(500, 400, BufferedImage.TYPE_INT_ARGB);
            paint(preview, fresh);
            assertArrayEquals(pixels(fresh), pixels(reused));
        });
    }

    @Test
    void fitShrinksLargeImagesToViewportAndResetsScrollPosition() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            PreviewPane preview = new PreviewPane(label -> {});
            preview.scrollPane().setSize(500, 400);
            preview.scrollPane().doLayout();
            preview.setImage(solid(1800, 1000, Color.WHITE));
            preview.actualSize();
            preview.scrollPane().getViewport().setViewSize(new java.awt.Dimension(1900, 1100));
            preview.scrollPane().getViewport().setViewPosition(new Point(200, 100));
            preview.fit();
            var extent = preview.scrollPane().getViewport().getExtentSize();
            assertTrue(preview.getPreferredSize().width <= extent.width);
            assertTrue(preview.getPreferredSize().height <= extent.height);
            assertEquals(new Point(), preview.scrollPane().getViewport().getViewPosition());
        });
    }
}

