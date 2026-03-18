package openclaw.lsp.tool;

import openclaw.lsp.protocol.*;
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
 * LSP Definition Tool.
 * Provides go-to-definition functionality.
 *
 * @author OpenClaw Team
 * @version 2026.3.18
 * @since 2026.3.0
 */
public class LspDefinitionTool implements AgentTool {

    private static final Logger logger = LoggerFactory.getLogger(LspDefinitionTool.class);

    private final LspSession session;

    public LspDefinitionTool(LspSession session) {
        this.session = session;
    }

    @Override
    public String getName() {
        return "lsp_definition_" + session.getServerName();
    }

    @Override
    public String getDescription() {
        return "Go to definition via " + session.getServerName() +
                ". Returns the definition location(s) for the symbol at the given position.";
    }

    @Override
    public ToolParameters getParameters() {
        return ToolParameters.builder()
                .property("uri", PropertySchema.string("File URI"))
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

        logger.debug("Definition request: {} at {}:{}", uri, line, character);

        return session.getClient()
                .sendRequest("textDocument/definition", params, LocationList.class)
                .thenApply(locations -> {
                    if (locations == null || locations.isEmpty()) {
                        return ToolResult.success("No definition found");
                    }
                    return ToolResult.success(formatLocations(locations));
                })
                .exceptionally(e -> {
                    logger.error("Definition request failed", e);
                    return ToolResult.failure("Definition request failed: " + e.getMessage());
                });
    }

    private String formatLocations(List<Location> locations) {
        StringBuilder sb = new StringBuilder();
        sb.append("Definitions:\n\n");

        for (int i = 0; i < locations.size(); i++) {
            Location loc = locations.get(i);
            sb.append("[").append(i + 1).append("] ");
            sb.append(loc.getUri());
            if (loc.getRange() != null) {
                sb.append(" at ").append(loc.getRange().getStart());
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    /**
     * Location list type.
     */
    public static class LocationList extends java.util.ArrayList<Location> {
    }

    /**
     * Location.
     */
    public static class Location {
        private String uri;
        private Range range;

        public String getUri() {
            return uri;
        }

        public void setUri(String uri) {
            this.uri = uri;
        }

        public Range getRange() {
            return range;
        }

        public void setRange(Range range) {
            this.range = range;
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
}
