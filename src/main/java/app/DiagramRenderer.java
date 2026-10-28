package app;

import static app.I18n.text;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.imageio.ImageIO;
import net.sourceforge.plantuml.FileFormat;
import net.sourceforge.plantuml.FileFormatOption;
import net.sourceforge.plantuml.SourceStringReader;
import net.sourceforge.plantuml.core.Diagram;
import net.sourceforge.plantuml.dot.GraphvizRuntimeEnvironment;
import net.sourceforge.plantuml.crash.CrashDiagram;
import net.sourceforge.plantuml.error.PSystemError;
import net.sourceforge.plantuml.preproc.Defines;

public final class DiagramRenderer {
    public record Result(BufferedImage image, byte[] png, byte[] svg, String message, int errorLine, boolean success) {
        public static Result failure(String message, int line) {
            return new Result(null, null, null, message, line, false);
        }
    }

    private Boolean useSmetana;
    private final boolean forceSmetana;

    public DiagramRenderer() {
        this(Boolean.getBoolean("puml.smetana"));
    }

    public DiagramRenderer(boolean forceSmetana) {
        this.forceSmetana = forceSmetana;
    }

    private boolean selectSmetana() {
        if (forceSmetana) {
            return true;
        }
        if (useSmetana == null) {
            try {
                File executable = GraphvizRuntimeEnvironment.getInstance().getDotExe();
                useSmetana = forceSmetana || executable == null || !executable.isFile() || !executable.canExecute();
            } catch (RuntimeException exception) {
                useSmetana = true;
            }
        }
        return useSmetana;
    }

    public Result render(String source) {
        if (source.isBlank()) {
            return Result.failure(text("render.empty"), -1);
        }
        if (source.length() > 500_000) {
            return Result.failure(text("render.tooLarge"), -1);
        }
        long started = System.nanoTime();
        try {
            boolean smetana = selectSmetana();
            List<String> config = smetana ? List.of("!pragma layout smetana") : List.of();
            SourceStringReader reader = new SourceStringReader(Defines.createEmpty(), source, config);
            if (reader.getBlocks().isEmpty()) {
                return Result.failure(text("render.noBlock"), -1);
            }
            Diagram diagram = reader.getBlocks().get(0).getDiagram();
            if (diagram instanceof PSystemError error) {
                int line = error.getLineLocation() == null ? -1 : error.getLineLocation().getPosition();
                String message = error.getFirstError().getError()
                        .replace("Syntax Error?", text("render.syntax"))
                        .replace("Assumed diagram type:", text("render.assumedType"));
                return Result.failure(message.contains("plantuml@gmail.com") ? text("render.internal") : message, line);
            }
            if (diagram instanceof CrashDiagram) {
                return Result.failure(text("render.internal"), -1);
            }
            ByteArrayOutputStream png = new ByteArrayOutputStream();
            var pngData = diagram.exportDiagram(png, 0, new FileFormatOption(FileFormat.PNG));
            if (pngData.getStatus() >= 400) {
                return Result.failure(text("render.failed"), -1);
            }
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(png.toByteArray()));
            if (image == null) {
                return Result.failure(text("render.invalidImage"), -1);
            }
            ByteArrayOutputStream svg = new ByteArrayOutputStream();
            var svgData = diagram.exportDiagram(svg, 0, new FileFormatOption(FileFormat.SVG));
            if (svgData.getStatus() >= 400 || !svg.toString(StandardCharsets.UTF_8).contains("<svg")) {
                return Result.failure(text("render.svgFailed"), -1);
            }
            long millis = (System.nanoTime() - started) / 1_000_000;
            String multiple = reader.getBlocks().size() > 1 || diagram.getNbImages() > 1 ? text("render.multiple") : "";
            return new Result(image, png.toByteArray(), svg.toByteArray(),
                    text("render.done", millis) + (smetana ? text("render.smetana") : "") + multiple, -1, true);
        } catch (Exception | LinkageError exception) {
            String detail = exception.getMessage();
            return Result.failure(exception instanceof IllegalStateException ? text("render.internal")
                    : text("render.error", detail == null ? exception.getClass().getSimpleName() : detail), -1);
        }
    }
}




