package openclaw.lsp.tool;

import openclaw.lsp.protocol.*;
import openclaw.lsp.session.LspSession;
import openclaw.sdk.tool.AgentTool;
import openclaw.sdk.tool.ToolExecuteContext;
import openclaw.sdk.tool.ToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * LSP Hover Tool.
 * Provides hover information for symbols.
 *
 * @author OpenClaw Team
 * @version 2026.3.18
 * @since 2026.3.0
 */
public class LspHoverTool implements AgentTool {

    private static final Logger logger = LoggerFactory.getLogger(LspHoverTool.class);

    private final LspSession session;

    public LspHoverTool(LspSession session) {
        this.session = session;
    }

    @Override
    public String getName() {
        return "lsp_hover_" + session.getServerName();
    }

    @Override
    public String getDescription() {
        return "Get hover information for a symbol via " + session.getServerName() +
                ". Returns documentation and type information for the symbol at the given position.";
    }

    @Override
    public ToolParameters getParameters() {
        return ToolParameters.builder()
                .property("uri", PropertySchema.string("File URI (e.g., file:///path/to/file.ts)"))
                .property("line", PropertySchema.integer("Line number (0-based)"))
                .property("character", PropertySchema.integer("Character offset (0-based)"))
                .required("uri", "line", "character")
                .build();
    }

    @Override
    public CompletableFuture<ToolResult> execute(ToolExecuteContext context) {
        Map<String, Object> args = context.arguments();

        String uri = (String) args.get("uri");
        int line = (Integer) args.get("line");
        int character = (Integer) args.get("character");

        TextDocumentPositionParams params = new TextDocumentPositionParams(
                new TextDocumentIdentifier(uri),
                new Position(line, character)
        );

        logger.debug("Hover request: {} at {}:{}", uri, line, character);

        return session.getClient()
                .sendRequest("textDocument/hover", params, Hover.class)
                .thenApply(hover -> {
                    if (hover == null || hover.getContents() == null) {
                        return ToolResult.success("No hover information available");
                    }
                    return ToolResult.success(formatHover(hover));
                })
                .exceptionally(e -> {
                    logger.error("Hover request failed", e);
                    return ToolResult.failure("Hover request failed: " + e.getMessage());
                });
    }

    private String formatHover(Hover hover) {
        StringBuilder sb = new StringBuilder();

        String contents = hover.getContentsAsString();
        if (contents != null && !contents.isEmpty()) {
            sb.append(contents);
        }

        if (hover.getRange() != null) {
            sb.append("\n\n(Range: ").append(hover.getRange().getStart())
                    .append(" - ").append(hover.getRange().getEnd()).append(")");
        }

        return sb.toString();
    }
}
