package app;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Insets;
import javax.swing.JButton;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;

final class UiStyle {
    static final int BUTTON_SIZE = 34;
    private UiStyle() {}

    static void compact(JButton button) {
        button.putClientProperty("JButton.minimumWidth", 0);
        button.setMargin(new Insets(0, 8, 0, 8));
    }

    static void square(JButton button) {
        compact(button);
        Dimension size = new Dimension(BUTTON_SIZE, BUTTON_SIZE);
        button.setPreferredSize(size);
        button.setMinimumSize(size);
        button.setMaximumSize(size);
    }

    static Color editorRail(Color background) {
        return new Color(Math.max(0, background.getRed() - 7),
                Math.max(0, background.getGreen() - 7), Math.max(0, background.getBlue() - 7));
    }

    static void scrollPane(JScrollPane pane, Color background) {
        Color rail = editorRail(background);
        for (JScrollBar bar : new JScrollBar[] {pane.getVerticalScrollBar(), pane.getHorizontalScrollBar()}) {
            bar.setBackground(rail);
            bar.putClientProperty("FlatLaf.style", "track: " + hex(rail)
                    + "; thumb: #596466; hoverThumbColor: #6a7779; pressedThumbColor: #7b888a");
        }
    }

    private static String hex(Color color) {
        return String.format("#%06x", color.getRGB() & 0xffffff);
    }
}
