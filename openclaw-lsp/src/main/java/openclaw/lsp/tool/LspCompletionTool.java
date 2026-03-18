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
 * LSP Completion Tool.
 * Provides code completion suggestions.
 *
 * @author OpenClaw Team
 * @version 2026.3.18
 * @since 2026.3.0
 */
public class LspCompletionTool implements AgentTool {

    private static final Logger logger = LoggerFactory.getLogger(LspCompletionTool.class);

    private final LspSession session;

    public LspCompletionTool(LspSession session) {
        this.session = session;
    }

    @Override
    public String getName() {
        return "lsp_completion_" + session.getServerName();
    }

    @Override
    public String getDescription() {
        return "Get code completion suggestions via " + session.getServerName() +
                ". Returns completion items for the given position.";
    }

    @Override
    public ToolParameters getParameters() {
        return ToolParameters.builder()
                .property("uri", PropertySchema.string("File URI"))
                .property("line", PropertySchema.integer("Line number (0-based)"))
                .property("character", PropertySchema.integer("Character offset (0-based)"))
                .property("triggerCharacter", PropertySchema.string("Trigger character (optional)"))
                .required("uri", "line", "character")
                .build();
    }

    @Override
    public CompletableFuture<ToolResult> execute(ToolExecuteContext context) {
        Map<String, Object> args = context.arguments();

        String uri = (String) args.get("uri");
        int line = (Integer) args.get("line");
        int character = (Integer) args.get("character");
        String triggerCharacter = (String) args.get("triggerCharacter");

        CompletionParams params = new CompletionParams(
                new TextDocumentIdentifier(uri),
                new Position(line, character)
        );
        if (triggerCharacter != null) {
            params.setTriggerCharacter(triggerCharacter);
        }

        logger.debug("Completion request: {} at {}:{}", uri, line, character);

        return session.getClient()
                .sendRequest("textDocument/completion", params, CompletionList.class)
                .thenApply(list -> {
                    if (list == null) {
                        return ToolResult.success("No completions available");
                    }
                    return ToolResult.success(formatCompletions(list));
                })
                .exceptionally(e -> {
                    logger.error("Completion request failed", e);
                    return ToolResult.failure("Completion request failed: " + e.getMessage());
                });
    }

    private String formatCompletions(CompletionList list) {
        StringBuilder sb = new StringBuilder();
        sb.append("Completions:\n\n");

        List<CompletionItem> items = list.getItems();
        if (items == null || items.isEmpty()) {
            return "No completions available";
        }

        for (int i = 0; i < items.size(); i++) {
            CompletionItem item = items.get(i);
            sb.append("[").append(i + 1).append("] ");
            sb.append(item.getLabel());

            if (item.getDetail() != null) {
                sb.append(" - ").append(item.getDetail());
            }

            if (item.getDocumentation() != null) {
                sb.append("\n    ").append(item.getDocumentation());
            }

            sb.append("\n");
        }

        return sb.toString();
    }

    /**
     * Completion params.
     */
    public static class CompletionParams extends TextDocumentPositionParams {
        private String triggerCharacter;

        public CompletionParams() {
        }

        public CompletionParams(TextDocumentIdentifier textDocument, Position position) {
            super(textDocument, position);
        }

        public String getTriggerCharacter() {
            return triggerCharacter;
        }

        public void setTriggerCharacter(String triggerCharacter) {
            this.triggerCharacter = triggerCharacter;
        }
    }

    /**
     * Completion list.
     */
    public static class CompletionList {
        private List<CompletionItem> items;

        public List<CompletionItem> getItems() {
            return items;
        }

        public void setItems(List<CompletionItem> items) {
            this.items = items;
        }
    }

    /**
     * Completion item.
     */
    public static class CompletionItem {
        private String label;
        private String kind;
        private String detail;
        private String documentation;
        private String insertText;

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public String getKind() {
            return kind;
        }

        public void setKind(String kind) {
            this.kind = kind;
        }

        public String getDetail() {
            return detail;
        }

        public void setDetail(String detail) {
            this.detail = detail;
        }

        public String getDocumentation() {
            return documentation;
        }

        public void setDocumentation(String documentation) {
            this.documentation = documentation;
        }

        public String getInsertText() {
            return insertText;
        }

        public void setInsertText(String insertText) {
            this.insertText = insertText;
        }
    }
}
