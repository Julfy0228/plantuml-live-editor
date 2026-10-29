package app;

import java.awt.Dimension;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

/** A single baseline: export labels collapse to icons when the preview becomes narrow. */
final class DiagramControls extends JPanel {
    private static final int GAP = 6;
    private static final int PAD = 10;
    private final JButton[] scale;
    private final JLabel zoom;
    private final JButton copy;
    private final JButton save;
    private final String copyText;
    private final String saveText;

    DiagramControls(JButton minus, JLabel zoom, JButton plus, JButton fit,
                    JButton actual, JButton copy, JButton save) {
        super(null);
        this.scale = new JButton[] {minus, plus, fit, actual};
        this.zoom = zoom;
        this.copy = copy;
        this.save = save;
        copyText = copy.getText();
        saveText = save.getText();
        zoom.setHorizontalAlignment(JLabel.CENTER);
        for (JButton button : scale) { UiStyle.square(button); add(button); }
        add(zoom);
        for (JButton button : new JButton[] {copy, save}) {
            UiStyle.compact(button);
            button.getAccessibleContext().setAccessibleName(button.getText());
            add(button);
        }
    }

    @Override public Dimension getPreferredSize() { return new Dimension(620, 50); }
    @Override public Dimension getMinimumSize() { return new Dimension(350, 50); }

    @Override public void doLayout() {
        int size = UiStyle.BUTTON_SIZE;
        int labelWidth = Math.max(76, zoom.getPreferredSize().width + 8);
        int x = PAD;
        int y = (getHeight() - size) / 2;
        scale[0].setBounds(x, y, size, size);
        x += size + GAP;
        zoom.setBounds(x, y, labelWidth, size);
        x += labelWidth + GAP;
        for (int i = 1; i < scale.length; i++) {
            scale[i].setBounds(x, y, size, size);
            x += size + GAP;
        }
        copy.setText(copyText);
        save.setText(saveText);
        int copyWidth = copy.getPreferredSize().width;
        int saveWidth = save.getPreferredSize().width;
        if (x + GAP + copyWidth + GAP + saveWidth + PAD > getWidth()) {
            copy.setText(null);
            save.setText(null);
            copyWidth = saveWidth = size;
        }
        int exportX = Math.max(x + GAP, getWidth() - PAD - copyWidth - GAP - saveWidth);
        copy.setBounds(exportX, y, copyWidth, size);
        save.setBounds(exportX + copyWidth + GAP, y, saveWidth, size);
    }
}
