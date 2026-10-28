package app;

import static app.I18n.text;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.BadLocationException;
import org.fife.ui.rsyntaxtextarea.AbstractTokenMakerFactory;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.fife.ui.rsyntaxtextarea.TokenMakerFactory;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.fife.ui.rtextarea.SearchContext;
import org.fife.ui.rtextarea.SearchEngine;
import org.fife.ui.rtextarea.SearchResult;

public final class EditorWindow extends JFrame {
    static final String EXAMPLE = "@startuml\nAlice -> Bob : hello\n@enduml\n";
    private final RSyntaxTextArea editor = new RSyntaxTextArea();
    private final JLabel filename = new JLabel();
    private final JLabel position = new JLabel(" ");
    private final JLabel status = new JLabel(" ");
    private final JLabel zoomLabel = new JLabel("100%");
    private final JLabel imageDimensions = new JLabel();
    private final PreviewPane preview = new PreviewPane(zoomLabel::setText);
    private final RenderQueue<String, DiagramRenderer.Result> renders;
    private final Timer debounce;
    private final JButton copy;
    private final JButton saveImage;
    private final JPanel searchPanel = new JPanel(new BorderLayout(6, 6));
    private final JTextField searchText = new JTextField(14);
    private final JTextField replaceText = new JTextField(14);
    private final JCheckBox caseSensitive = new JCheckBox("Aa");
    private final JPanel replaceRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
    private Path file;
    private String savedText = EXAMPLE;
    private boolean dirty;
    private boolean loading;
    private long revision;
    private DiagramRenderer.Result rendered;
    private Object errorHighlight;
    private int errorLine = -1;

    public EditorWindow() {
        super("PlantUML Live Editor");
        renders = new RenderQueue<>(new DiagramRenderer()::render,
                failure -> DiagramRenderer.Result.failure(text("render.unexpected", failure.getClass().getSimpleName()), -1),
                SwingUtilities::invokeLater, this::acceptRender);
        debounce = new Timer(350, event -> submitRender());
        debounce.setRepeats(false);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setMinimumSize(new Dimension(800, 540));
        setSize(1180, 760);
        setLocationRelativeTo(null);
        setupEditor();

        Action newFile = action(text("file.new"), () -> {
            if (confirmDiscard()) {
                load(null, EXAMPLE);
            }
        });
        Action openFile = action(text("file.open"), this::chooseOpen);
        Action saveFile = action(text("file.save"), () -> save(false));
        Action saveAs = action(text("file.saveAs"), () -> save(true));
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);
        toolbar.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 12));
        toolbar.add(iconButton(newFile, ToolbarIcon.Kind.NEW, text("file.new") + " · " + shortcutName("N")));
        toolbar.add(iconButton(openFile, ToolbarIcon.Kind.OPEN, text("file.open") + " · " + shortcutName("O")));
        toolbar.add(iconButton(saveFile, ToolbarIcon.Kind.SAVE, text("file.save") + " · " + shortcutName("S")));
        toolbar.addSeparator(new Dimension(16, 1));
        toolbar.add(filename);
        toolbar.add(Box.createHorizontalGlue());
        toolbar.add(iconButton(action(text("search.find"), () -> showSearch(false)), ToolbarIcon.Kind.SEARCH,
                text("search.find") + " · " + shortcutName("F")));
        add(toolbar, BorderLayout.NORTH);

        RTextScrollPane editorScroll = new RTextScrollPane(editor);
        editorScroll.setLineNumbersEnabled(true);
        editorScroll.setBorder(null);
        editorScroll.getGutter().setBackground(new Color(0x1C2028));
        editorScroll.getGutter().setLineNumberColor(new Color(0x8995A9));
        editorScroll.getGutter().setBorderColor(new Color(0x303745));
        JPanel left = new JPanel(new BorderLayout());
        left.add(editorScroll, BorderLayout.CENTER);
        setupSearch();
        left.add(searchPanel, BorderLayout.SOUTH);

        JPanel right = new JPanel(new BorderLayout());
        right.add(preview.scrollPane(), BorderLayout.CENTER);
        JPanel controls = new JPanel(new BorderLayout(8, 8));
        controls.setBorder(BorderFactory.createEmptyBorder(8, 10, 10, 10));
        JPanel scaleControls = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        scaleControls.add(iconButton(action(text("preview.zoomOut"), () -> preview.changeZoom(1 / 1.2)),
                ToolbarIcon.Kind.MINUS, text("preview.zoomOut") + " · " + text("preview.wheel")));
        zoomLabel.setPreferredSize(new Dimension(88, 28));
        zoomLabel.setHorizontalAlignment(JLabel.CENTER);
        scaleControls.add(zoomLabel);
        scaleControls.add(iconButton(action(text("preview.zoomIn"), () -> preview.changeZoom(1.2)), ToolbarIcon.Kind.PLUS, text("preview.zoomIn")));
        scaleControls.add(iconButton(action(text("preview.fit"), preview::fit), ToolbarIcon.Kind.FIT, text("preview.fit")));
        JButton actual = new JButton(action("1:1", preview::actualSize));
        actual.setToolTipText(text("preview.actualSize"));
        scaleControls.add(actual);
        JPanel exports = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        copy = new JButton(action(text("image.copy"), this::copyImage));
        copy.setIcon(new ToolbarIcon(ToolbarIcon.Kind.COPY));
        copy.setToolTipText(text("image.copyTooltip"));
        saveImage = new JButton(action(text("image.save"), this::export));
        saveImage.setIcon(new ToolbarIcon(ToolbarIcon.Kind.SAVE));
        exports.add(copy);
        exports.add(saveImage);
        controls.add(scaleControls, BorderLayout.NORTH);
        controls.add(exports, BorderLayout.SOUTH);
        right.add(controls, BorderLayout.SOUTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        split.setBorder(null);
        split.setResizeWeight(0.45);
        split.setDividerSize(7);
        left.setMinimumSize(new Dimension(350, 200));
        right.setMinimumSize(new Dimension(400, 200));
        add(split, BorderLayout.CENTER);
        SwingUtilities.invokeLater(() -> split.setDividerLocation(0.45));

        JPanel footer = new JPanel(new BorderLayout(16, 0));
        footer.setBorder(BorderFactory.createEmptyBorder(7, 12, 7, 12));
        footer.add(position, BorderLayout.WEST);
        footer.add(status, BorderLayout.CENTER);
        footer.add(imageDimensions, BorderLayout.EAST);
        status.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent event) {
                goToError();
            }
        });
        add(footer, BorderLayout.SOUTH);
        bind(KeyEvent.VK_N, 0, newFile);
        bind(KeyEvent.VK_O, 0, openFile);
        bind(KeyEvent.VK_S, 0, saveFile);
        bind(KeyEvent.VK_S, InputEvent.SHIFT_DOWN_MASK, saveAs);
        bind(KeyEvent.VK_F, 0, action(text("search.find"), () -> showSearch(false)));
        bind(KeyEvent.VK_H, 0, action(text("search.replace"), () -> showSearch(true)));
        bind(KeyEvent.VK_R, 0, action(text("preview.refresh"), () -> {
            debounce.stop();
            changed();
            debounce.stop();
            submitRender();
        }));
        getRootPane().registerKeyboardAction(event -> hideSearch(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                requestClose();
            }
        });
        load(null, EXAMPLE);
    }

    private static Action action(String name, Runnable runnable) {
        return new AbstractAction(name) {
            @Override
            public void actionPerformed(ActionEvent event) {
                runnable.run();
            }
        };
    }

    private static JButton iconButton(Action action, ToolbarIcon.Kind icon, String tooltip) {
        JButton button = new JButton(action);
        button.setText(null);
        button.setIcon(new ToolbarIcon(icon));
        button.setToolTipText(tooltip);
        button.getAccessibleContext().setAccessibleName((String) action.getValue(Action.NAME));
        button.setPreferredSize(new Dimension(36, 34));
        return button;
    }

    private static String shortcutName(String key) {
        return (System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("mac") ? "Cmd+" : "Ctrl+") + key;
    }

    private void bind(int key, int modifiers, Action action) {
        KeyStroke stroke = KeyStroke.getKeyStroke(key, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx() | modifiers);
        String name = action.getValue(Action.NAME).toString();
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(stroke, name);
        getRootPane().getActionMap().put(name, action);
        editor.getInputMap().put(stroke, name);
        editor.getActionMap().put(name, action);
    }

    private void setupEditor() {
        AbstractTokenMakerFactory factory = (AbstractTokenMakerFactory) TokenMakerFactory.getDefaultInstance();
        factory.putMapping("text/plantuml", PlantUmlTokenMaker.class.getName());
        editor.setSyntaxEditingStyle("text/plantuml");
        editor.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 15));
        editor.setTabSize(2);
        editor.setTabsEmulated(true);
        editor.setAutoIndentEnabled(true);
        editor.setBracketMatchingEnabled(true);
        editor.setAntiAliasingEnabled(true);
        editor.setCodeFoldingEnabled(false);
        try (var theme = RSyntaxTextArea.class.getResourceAsStream("/org/fife/ui/rsyntaxtextarea/themes/dark.xml")) {
            if (theme != null) {
                Theme.load(theme).apply(editor);
            }
        } catch (Exception exception) {
            editor.setBackground(new Color(0x1C2028));
            editor.setForeground(new Color(0xDEE5F1));
            editor.setCaretColor(Color.WHITE);
        }
        editor.setMargin(new java.awt.Insets(12, 8, 12, 8));
        editor.addCaretListener(event -> updatePosition());
        editor.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) { changed(); }
            @Override
            public void removeUpdate(DocumentEvent event) { changed(); }
            @Override
            public void changedUpdate(DocumentEvent event) {}
        });
    }

    private void changed() {
        if (loading) {
            return;
        }
        dirty = !editor.getText().equals(savedText);
        updateTitle();
        revision = renders.invalidate();
        setExportEnabled(false);
        preview.setStale(rendered != null);
        clearError();
        setStatus(text("status.waiting"), false);
        debounce.restart();
    }

    private void submitRender() {
        setStatus(text("status.rendering"), false);
        renders.submit(revision, editor.getText());
    }

    private void acceptRender(long version, DiagramRenderer.Result result) {
        if (version != revision) {
            return;
        }
        if (result.success()) {
            rendered = result;
            clearError();
            preview.setImage(result.image());
            imageDimensions.setText(result.image().getWidth() + " × " + result.image().getHeight());
            setExportEnabled(true);
            setStatus(result.message(), false);
        } else {
            errorLine = result.errorLine();
            setExportEnabled(false);
            preview.setStale(rendered != null);
            String message = errorLine >= 0 ? text("status.line", errorLine + 1, result.message()) : result.message();
            setStatus(message, true);
            if (errorLine >= 0 && errorLine < editor.getLineCount()) {
                try {
                    errorHighlight = editor.addLineHighlight(errorLine, new Color(0x58313B));
                } catch (BadLocationException ignored) {
                    errorLine = -1;
                }
            }
        }
    }

    private void setExportEnabled(boolean enabled) {
        copy.setEnabled(enabled);
        saveImage.setEnabled(enabled);
    }

    private void setStatus(String text, boolean error) {
        status.setText(text.replace('\n', ' '));
        status.setToolTipText(text);
        status.setForeground(new Color(error ? 0xF2A3A8 : 0xAAB8C8));
    }

    private void clearError() {
        if (errorHighlight != null) {
            editor.removeLineHighlight(errorHighlight);
            errorHighlight = null;
        }
        errorLine = -1;
    }

    private void goToError() {
        if (errorLine >= 0 && errorLine < editor.getLineCount()) {
            try {
                editor.setCaretPosition(editor.getLineStartOffset(errorLine));
                editor.requestFocusInWindow();
            } catch (BadLocationException ignored) {
                Toolkit.getDefaultToolkit().beep();
            }
        }
    }

    private void updatePosition() {
        try {
            int caret = editor.getCaretPosition();
            int line = editor.getLineOfOffset(caret);
            position.setText(text("status.position", line + 1, caret - editor.getLineStartOffset(line) + 1));
        } catch (BadLocationException ignored) {
            position.setText("UTF-8");
        }
    }

    private void updateTitle() {
        String name = file == null ? text("file.untitled") : file.getFileName().toString();
        filename.setText(name + (dirty ? " ●" : ""));
        filename.setToolTipText(file == null ? text("file.newDocument") : file.toAbsolutePath().toString());
        setTitle((dirty ? "● " : "") + name + " — PlantUML Live Editor");
    }

    private void load(Path path, String text) {
        debounce.stop();
        revision = renders.invalidate();
        loading = true;
        try {
            file = path;
            savedText = text;
            editor.setText(text);
            editor.setCaretPosition(0);
            editor.discardAllEdits();
            dirty = false;
            rendered = null;
            preview.clear();
            imageDimensions.setText("");
            clearError();
            setExportEnabled(false);
        } finally {
            loading = false;
        }
        updateTitle();
        updatePosition();
        debounce.restart();
        editor.requestFocusInWindow();
    }

    public void open(Path path) {
        if (!confirmDiscard()) {
            return;
        }
        try {
            String text = DocumentFiles.read(path);
            load(path.toAbsolutePath(), text);
        } catch (Exception exception) {
            showError(text("error.open"), exception);
        }
    }

    private JFileChooser chooser(String extension) {
        JFileChooser chooser = new JFileChooser();
        chooser.setLocale(I18n.locale());
        chooser.setFileFilter(new FileNameExtensionFilter(extension.toUpperCase(Locale.ROOT) + " (*." + extension + ")", extension));
        if (file != null) {
            chooser.setCurrentDirectory(file.toAbsolutePath().getParent().toFile());
        }
        return chooser;
    }

    private void chooseOpen() {
        JFileChooser chooser = chooser("puml");
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            open(chooser.getSelectedFile().toPath());
        }
    }

    private boolean save(boolean saveAs) {
        Path target = file;
        if (target == null || saveAs) {
            JFileChooser chooser = chooser("puml");
            chooser.setSelectedFile(new java.io.File(file == null ? "diagram.puml" : file.getFileName().toString()));
            if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
                return false;
            }
            target = DocumentFiles.withExtension(chooser.getSelectedFile().toPath(), "puml");
            if (!confirmOverwrite(target)) {
                return false;
            }
        }
        try {
            String text = editor.getText();
            DocumentFiles.write(target, text.getBytes(StandardCharsets.UTF_8));
            file = target.toAbsolutePath();
            savedText = text;
            dirty = false;
            updateTitle();
            setStatus(text("status.saved", file.getFileName()), false);
            return true;
        } catch (Exception exception) {
            showError(text("error.save"), exception);
            return false;
        }
    }

    private boolean confirmOverwrite(Path path) {
        return !Files.exists(path) || JOptionPane.showConfirmDialog(this,
                text("confirm.overwrite", path.getFileName()), text("confirm.exists"),
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION;
    }

    private boolean confirmDiscard() {
        if (!dirty) {
            return true;
        }
        Object[] choices = {text("file.save"), text("confirm.discard"), text("cancel")};
        int result = JOptionPane.showOptionDialog(this, text("confirm.saveChanges"),
                text("confirm.unsaved"), JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,
                null, choices, choices[0]);
        return result == 0 ? save(false) : result == 1;
    }

    public boolean requestClose() {
        if (!confirmDiscard()) {
            return false;
        }
        debounce.stop();
        renders.close();
        dispose();
        return true;
    }

    private void copyImage() {
        if (!copy.isEnabled() || rendered == null) {
            return;
        }
        BufferedImage image = rendered.image();
        try {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new Transferable() {
                @Override
                public DataFlavor[] getTransferDataFlavors() { return new DataFlavor[] {DataFlavor.imageFlavor}; }
                @Override
                public boolean isDataFlavorSupported(DataFlavor flavor) { return DataFlavor.imageFlavor.equals(flavor); }
                @Override
                public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
                    if (!isDataFlavorSupported(flavor)) {
                        throw new UnsupportedFlavorException(flavor);
                    }
                    return image;
                }
            }, null);
            setStatus(text("status.copied"), false);
        } catch (IllegalStateException | SecurityException exception) {
            showError(text("error.copy"), exception);
        }
    }

    private void export() {
        if (rendered == null || !saveImage.isEnabled()) {
            return;
        }
        DiagramRenderer.Result snapshot = rendered;
        long snapshotRevision = revision;
        String name = file == null ? "diagram" : file.getFileName().toString().replaceFirst("(?i)\\.puml$", "");
        ImageExportChooser chooser = new ImageExportChooser(name,
                file == null ? null : file.toAbsolutePath().getParent());
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        Path target = chooser.outputPath();
        String extension = chooser.extension();
        if (!confirmOverwrite(target) || revision != snapshotRevision) {
            return;
        }
        try {
            DocumentFiles.write(target, extension.equals("png") ? snapshot.png() : snapshot.svg());
            setStatus(text("status.exported", target.getFileName()), false);
        } catch (Exception exception) {
            showError(text("error.export"), exception);
        }
    }

    private void showError(String title, Exception exception) {
        JOptionPane.showMessageDialog(this, exception.getMessage(), title, JOptionPane.ERROR_MESSAGE);
    }

    private void setupSearch() {
        searchPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        JPanel searchRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        searchText.setToolTipText(text("search.query"));
        searchText.getAccessibleContext().setAccessibleName(text("search.find"));
        searchRow.add(searchText);
        searchRow.add(new JButton(action(text("search.next"), () -> search(false, false))));
        caseSensitive.setToolTipText(text("search.matchCase"));
        caseSensitive.getAccessibleContext().setAccessibleName(text("search.matchCase"));
        searchRow.add(caseSensitive);
        JButton closeSearch = new JButton(action("×", this::hideSearch));
        closeSearch.setToolTipText(text("search.close"));
        closeSearch.getAccessibleContext().setAccessibleName(text("search.close"));
        searchRow.add(closeSearch);
        replaceText.getAccessibleContext().setAccessibleName(text("search.replaceWith"));
        replaceText.setToolTipText(text("search.replaceWith"));
        replaceRow.add(replaceText);
        replaceRow.add(new JButton(action(text("search.replace"), () -> search(true, false))));
        replaceRow.add(new JButton(action(text("search.all"), () -> search(true, true))));
        searchPanel.add(searchRow, BorderLayout.NORTH);
        searchPanel.add(replaceRow, BorderLayout.SOUTH);
        searchText.addActionListener(event -> search(false, false));
        searchPanel.setVisible(false);
    }

    private void showSearch(boolean replace) {
        searchPanel.setVisible(true);
        replaceRow.setVisible(replace);
        String selected = editor.getSelectedText();
        if (selected != null && !selected.contains("\n")) {
            searchText.setText(selected);
        }
        searchPanel.revalidate();
        searchText.requestFocusInWindow();
        searchText.selectAll();
    }

    private void hideSearch() {
        searchPanel.setVisible(false);
        editor.requestFocusInWindow();
    }

    private void search(boolean replace, boolean all) {
        if (searchText.getText().isEmpty()) {
            return;
        }
        SearchContext context = new SearchContext();
        context.setSearchFor(searchText.getText());
        context.setReplaceWith(replaceText.getText());
        context.setMatchCase(caseSensitive.isSelected());
        context.setSearchWrap(true);
        context.setMarkAll(false);
        SearchResult result = all ? SearchEngine.replaceAll(editor, context)
                : replace ? SearchEngine.replace(editor, context) : SearchEngine.find(editor, context);
        setStatus(result.wasFound() ? (all ? text("search.replacements", result.getCount()) : text("search.found"))
                : text("search.notFound"), false);
    }
}




