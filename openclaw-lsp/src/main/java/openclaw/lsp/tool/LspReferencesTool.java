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
 * LSP References Tool.
 * Finds all references to a symbol.
 *
 * @author OpenClaw Team
 * @version 2026.3.18
 * @since 2026.3.0
 */
public class LspReferencesTool implements AgentTool {

    private static final Logger logger = LoggerFactory.getLogger(LspReferencesTool.class);

    private final LspSession session;

    public LspReferencesTool(LspSession session) {
        this.session = session;
    }

    @Override
    public String getName() {
        return "lsp_references_" + session.getServerName();
    }

    @Override
    public String getDescription() {
        return "Find all references via " + session.getServerName() +
                ". Returns all locations where the symbol at the given position is referenced.";
    }

    @Override
    public ToolParameters getParameters() {
        return ToolParameters.builder()
                .properties(Map.of(
                        "uri", PropertySchema.string("File URI"),
                        "line", PropertySchema.integer("Line number (0-based)"),
                        "character", PropertySchema.integer("Character offset (0-based)"),
                        "includeDeclaration", PropertySchema.of("boolean", "Include declaration in results")
                ))
                .required(List.of("uri", "line", "character"))
                .build();
    }

    @Override
    public CompletableFuture<ToolResult> execute(ToolExecuteContext context) {
        Map<String, Object> args = context.arguments();

        String uri = (String) args.get("uri");
        int line = (Integer) args.get("line");
        int character = (Integer) args.get("character");
        Boolean includeDeclaration = (Boolean) args.getOrDefault("includeDeclaration", true);

        ReferenceParams params = new ReferenceParams(
                new TextDocumentIdentifier(uri),
                new Position(line, character)
        );
        params.setIncludeDeclaration(includeDeclaration);

        logger.debug("References request: {} at {}:{}", uri, line, character);

        return session.getClient()
                .sendRequest("textDocument/references", params, LocationList.class)
                .thenApply(locations -> {
                    if (locations == null || locations.isEmpty()) {
                        return ToolResult.success("No references found");
                    }
                    return ToolResult.success(formatLocations(locations));
                })
                .exceptionally(e -> {
                    logger.error("References request failed", e);
                    return ToolResult.failure("References request failed: " + e.getMessage());
                });
    }

    private String formatLocations(List<Location> locations) {
        StringBuilder sb = new StringBuilder();
        sb.append("References (").append(locations.size()).append("):\n\n");

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
     * Reference params.
     */
    public static class ReferenceParams extends TextDocumentPositionParams {
        private Boolean includeDeclaration;

        public ReferenceParams() {
        }

        public ReferenceParams(TextDocumentIdentifier textDocument, Position position) {
            super(textDocument, position);
        }

        public Boolean getIncludeDeclaration() {
            return includeDeclaration;
        }

        public void setIncludeDeclaration(Boolean includeDeclaration) {
            this.includeDeclaration = includeDeclaration;
        }
    }

    /**
     * Location list.
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
