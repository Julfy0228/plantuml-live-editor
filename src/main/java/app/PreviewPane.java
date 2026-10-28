package app;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.Scrollable;

public final class PreviewPane extends JComponent implements Scrollable {
    private BufferedImage image;
    private double zoom = 1;
    private boolean fit = true;
    private boolean stale;
    private final JScrollPane scroll;
    private final Consumer<String> zoomLabel;

    public PreviewPane(Consumer<String> zoomLabel) {
        this.zoomLabel = zoomLabel;
        setOpaque(true);
        setBackground(new Color(0x191C22));
        scroll = new JScrollPane(this);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(getBackground());
        scroll.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
        scroll.getVerticalScrollBar().setUnitIncrement(24);
        scroll.getHorizontalScrollBar().setUnitIncrement(24);
        scroll.getViewport().addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent event) {
                refresh();
            }
        });
        scroll.addMouseWheelListener(event -> {
            if (event.isControlDown() || event.isMetaDown()) {
                changeZoom(event.getWheelRotation() < 0 ? 1.15 : 1 / 1.15);
                event.consume();
            }
        });
    }

    public JScrollPane scrollPane() {
        return scroll;
    }

    public void setImage(BufferedImage next) {
        image = next;
        stale = false;
        refresh();
    }

    public void setStale(boolean stale) {
        this.stale = stale;
        repaint();
    }

    public void clear() {
        image = null;
        stale = false;
        refresh();
    }

    public void fit() {
        fit = true;
        scroll.getViewport().setViewPosition(new java.awt.Point());
        refresh();
    }

    public void actualSize() {
        fit = false;
        zoom = 1;
        refresh();
    }

    public void changeZoom(double multiplier) {
        zoom = Math.max(0.1, Math.min(4, scale() * multiplier));
        fit = false;
        refresh();
    }

    private double scale() {
        if (!fit || image == null) {
            return zoom;
        }
        Dimension available = scroll.getViewport().getExtentSize();
        return Math.max(0.02, Math.min(1, Math.min(
                (available.width - 48.0) / image.getWidth(),
                (available.height - 48.0) / image.getHeight())));
    }

    private void refresh() {
        zoomLabel.accept(Math.round(scale() * 100) + "%" + (fit ? " · " + I18n.text("preview.auto") : ""));
        revalidate();
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        if (image == null) {
            return new Dimension(300, 240);
        }
        double factor = scale();
        return new Dimension((int) Math.ceil(image.getWidth() * factor) + 48,
                (int) Math.ceil(image.getHeight() * factor) + 48);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D canvas = (Graphics2D) graphics.create();
        try {
            canvas.setColor(getBackground());
            canvas.fillRect(0, 0, getWidth(), getHeight());
            canvas.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Rectangle visible = getVisibleRect();
            if (image == null) {
                canvas.setColor(new Color(0xA4ADBC));
                String hint = I18n.text("preview.empty");
                canvas.drawString(hint, visible.x + Math.max(16,
                                (visible.width - canvas.getFontMetrics().stringWidth(hint)) / 2),
                        visible.y + Math.max(40, visible.height / 2));
                return;
            }
            int width = Math.max(1, (int) Math.round(image.getWidth() * scale()));
            int height = Math.max(1, (int) Math.round(image.getHeight() * scale()));
            int left = Math.max(24, (getWidth() - width) / 2);
            int top = Math.max(24, (getHeight() - height) / 2);
            canvas.setColor(Color.WHITE);
            canvas.fillRect(left, top, width, height);
            canvas.drawImage(image, left, top, width, height, null);
            if (stale) {
                String label = I18n.text("preview.stale");
                int labelWidth = canvas.getFontMetrics().stringWidth(label);
                int badgeWidth = Math.min(labelWidth + 24, Math.max(0, visible.width - 24));
                int badgeHeight = canvas.getFontMetrics().getHeight() + 12;
                canvas.setColor(new Color(0x2B303B));
                canvas.fillRoundRect(visible.x + 12, visible.y + 8, badgeWidth, badgeHeight, 8, 8);
                canvas.clipRect(visible.x + 18, visible.y + 8, Math.max(0, badgeWidth - 12), badgeHeight);
                canvas.setColor(new Color(0xF1C985));
                canvas.drawString(label, visible.x + 24, visible.y + 14 + canvas.getFontMetrics().getAscent());
            }
        } finally {
            canvas.dispose();
        }
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return new Dimension(560, 440);
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) {
        return 24;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) {
        return Math.max(24, orientation == javax.swing.SwingConstants.VERTICAL ? visible.height - 24 : visible.width - 24);
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return fit || getPreferredSize().width < scroll.getViewport().getWidth();
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return fit || getPreferredSize().height < scroll.getViewport().getHeight();
    }
}

