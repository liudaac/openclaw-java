package openclaw.lsp.tool;

import openclaw.lsp.protocol.TextDocumentIdentifier;
import openclaw.lsp.session.LspSession;
import openclaw.sdk.tool.AgentTool;
import openclaw.sdk.tool.ToolExecuteContext;
import openclaw.sdk.tool.ToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * LSP Diagnostics Tool.
 * Gets diagnostics for a document.
 *
 * @author OpenClaw Team
 * @version 2026.3.18
 * @since 2026.3.0
 */
public class LspDiagnosticsTool implements AgentTool {

    private static final Logger logger = LoggerFactory.getLogger(LspDiagnosticsTool.class);

    private final LspSession session;

    public LspDiagnosticsTool(LspSession session) {
        this.session = session;
    }

    @Override
    public String getName() {
        return "lsp_diagnostics_" + session.getServerName();
    }

    @Override
    public String getDescription() {
        return "Get diagnostics via " + session.getServerName() +
                ". Returns errors, warnings, and other diagnostics for the given document.";
    }

    @Override
    public ToolParameters getParameters() {
        return ToolParameters.builder()
                .properties(Map.of(
                        "uri", PropertySchema.string("File URI")
                ))
                .required(List.of("uri"))
                .build();
    }

    @Override
    public CompletableFuture<ToolResult> execute(ToolExecuteContext context) {
        Map<String, Object> args = context.arguments();

        String uri = (String) args.get("uri");

        DocumentDiagnosticParams params = new DocumentDiagnosticParams(
                new TextDocumentIdentifier(uri)
        );

        logger.debug("Diagnostics request: {}", uri);

        return session.getClient()
                .sendRequest("textDocument/diagnostic", params, DocumentDiagnosticReport.class)
                .thenApply(report -> {
                    if (report == null || report.getItems() == null || report.getItems().isEmpty()) {
                        return ToolResult.success("No diagnostics");
                    }
                    return ToolResult.success(formatDiagnostics(report.getItems()));
                })
                .exceptionally(e -> {
                    logger.error("Diagnostics request failed", e);
                    return ToolResult.failure("Diagnostics request failed: " + e.getMessage());
                });
    }

    private String formatDiagnostics(List<Diagnostic> diagnostics) {
        StringBuilder sb = new StringBuilder();
        sb.append("Diagnostics (").append(diagnostics.size()).append("):\n\n");

        int errors = 0;
        int warnings = 0;
        int infos = 0;

        for (Diagnostic d : diagnostics) {
            String severity = d.getSeverity() != null ? d.getSeverity() : "unknown";
            switch (severity) {
                case "Error" -> errors++;
                case "Warning" -> warnings++;
                case "Information" -> infos++;
            }

            sb.append("[").append(severity).append("] ");
            if (d.getRange() != null) {
                sb.append("at ").append(d.getRange().getStart()).append(" ");
            }
            sb.append(d.getMessage());

            if (d.getCode() != null) {
                sb.append(" (").append(d.getCode()).append(")");
            }

            sb.append("\n");
        }

        sb.append("\nSummary: ").append(errors).append(" errors, ")
                .append(warnings).append(" warnings, ")
                .append(infos).append(" infos");

        return sb.toString();
    }

    /**
     * Document diagnostic params.
     */
    public static class DocumentDiagnosticParams {
        private TextDocumentIdentifier textDocument;

        public DocumentDiagnosticParams() {
        }

        public DocumentDiagnosticParams(TextDocumentIdentifier textDocument) {
            this.textDocument = textDocument;
        }

        public TextDocumentIdentifier getTextDocument() {
            return textDocument;
        }

        public void setTextDocument(TextDocumentIdentifier textDocument) {
            this.textDocument = textDocument;
        }
    }

    /**
     * Document diagnostic report.
     */
    public static class DocumentDiagnosticReport {
        private List<Diagnostic> items;

        public List<Diagnostic> getItems() {
            return items;
        }

        public void setItems(List<Diagnostic> items) {
            this.items = items;
        }
    }

    /**
     * Diagnostic.
     */
    public static class Diagnostic {
        private Range range;
        private String severity;
        private String code;
        private String source;
        private String message;

        public Range getRange() {
            return range;
        }

        public void setRange(Range range) {
            this.range = range;
        }

        public String getSeverity() {
            return severity;
        }

        public void setSeverity(String severity) {
            this.severity = severity;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    /**
     * Range.
     */
    public static class Range {
        private Position start;
        private Position end;

        public Position getStart() {
            return start;
        }

        public void setStart(Position start) {
            this.start = start;
        }

        public Position getEnd() {
            return end;
        }

        public void setEnd(Position end) {
            this.end = end;
        }
    }

    /**
     * Position.
     */
    public static class Position {
        private int line;
        private int character;

        public int getLine() {
            return line;
        }

        public void setLine(int line) {
            this.line = line;
        }

        public int getCharacter() {
            return character;
        }

        public void setCharacter(int character) {
            this.character = character;
        }

        @Override
        public String toString() {
            return line + ":" + character;
        }
    }
}
