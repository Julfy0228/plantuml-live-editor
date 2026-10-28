package app;

import java.awt.BasicStroke;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.Icon;

public record ToolbarIcon(Kind kind) implements Icon {
    public enum Kind { NEW, OPEN, SAVE, SEARCH, COPY, FIT, MINUS, PLUS }

    @Override
    public int getIconWidth() { return 18; }

    @Override
    public int getIconHeight() { return 18; }

    @Override
    public void paintIcon(Component component, Graphics graphics, int left, int top) {
        Graphics2D canvas = (Graphics2D) graphics.create();
        try {
            canvas.translate(left, top);
            canvas.setColor(component.isEnabled() ? component.getForeground() : java.awt.Color.GRAY);
            canvas.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            switch (kind) {
                case NEW -> {
                    canvas.drawRoundRect(3, 1, 11, 15, 2, 2);
                    canvas.drawLine(6, 9, 11, 9);
                    canvas.drawLine(9, 6, 9, 12);
                }
                case OPEN -> {
                    canvas.drawPolyline(new int[] {2, 2, 7, 9, 15, 15}, new int[] {14, 3, 3, 5, 5, 7}, 6);
                    canvas.drawPolygon(new int[] {2, 4, 17, 14}, new int[] {15, 7, 7, 15}, 4);
                }
                case SAVE -> {
                    canvas.drawRoundRect(2, 2, 14, 14, 2, 2);
                    canvas.drawRect(5, 2, 7, 5);
                    canvas.drawRect(5, 11, 8, 5);
                }
                case SEARCH -> {
                    canvas.drawOval(2, 2, 9, 9);
                    canvas.drawLine(10, 10, 16, 16);
                }
                case COPY -> {
                    canvas.drawRoundRect(6, 5, 10, 11, 2, 2);
                    canvas.drawPolyline(new int[] {3, 2, 2, 12, 12}, new int[] {12, 12, 1, 1, 2}, 5);
                }
                case FIT -> {
                    canvas.drawPolyline(new int[] {2, 2, 6}, new int[] {6, 2, 2}, 3);
                    canvas.drawPolyline(new int[] {12, 16, 16}, new int[] {2, 2, 6}, 3);
                    canvas.drawPolyline(new int[] {2, 2, 6}, new int[] {12, 16, 16}, 3);
                    canvas.drawPolyline(new int[] {12, 16, 16}, new int[] {16, 16, 12}, 3);
                }
                case MINUS -> canvas.drawLine(3, 9, 15, 9);
                case PLUS -> {
                    canvas.drawLine(3, 9, 15, 9);
                    canvas.drawLine(9, 3, 9, 15);
                }
            }
        } finally {
            canvas.dispose();
        }
    }
}

