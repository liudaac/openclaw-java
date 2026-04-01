package openclaw.acp;

import openclaw.acp.model.ToolCall;

import java.util.Map;
import java.util.Set;

/**
 * ACP approval classifier.
 * Mirrors the functionality of approval-classifier.ts
 */
public class ApprovalClassifier {

    // Tool sets from approval-classifier.ts
    private static final Set<String> SAFE_SEARCH_TOOLS = Set.of(
        "search",
        "web_search",
        "memory_search"
    );

    private static final Set<String> EXEC_CAPABLE_TOOLS = Set.of(
        "exec",
        "spawn",
        "shell",
        "bash",
        "process",
        "nodes",
        "code_execution"
    );

    private static final Set<String> CONTROL_PLANE_TOOLS = Set.of(
        "gateway",
        "cron",
        "sessions_spawn",
        "sessions_send",
        "session_status",
        "subagents"
    );

    private static final Set<String> INTERACTIVE_TOOLS = Set.of(
        "whatsapp_login",
        "telegram_login",
        "interactive_prompt"
    );

    private static final Set<String> READONLY_SCOPED_TOOLS = Set.of(
        "read_file",
        "list_files",
        "get_file_info"
    );

    /**
     * Classify a tool call for approval.
     */
    public AcpApprovalClassification classify(ToolCall toolCall) {
        String toolName = resolveToolName(toolCall);
        AcpApprovalClass approvalClass = classifyTool(toolName);
        boolean autoApprove = approvalClass.isAutoApprove();

        return new AcpApprovalClassification(toolName, approvalClass, autoApprove);
    }

    /**
     * Resolve the tool name from a tool call.
     * Tries multiple sources in order of preference.
     */
    private String resolveToolName(ToolCall toolCall) {
        if (toolCall == null) {
            return "unknown";
        }

        // Try title first (most specific)
        if (toolCall.getTitle() != null && !toolCall.getTitle().isBlank()) {
            String normalized = normalizeToolName(toolCall.getTitle());
            if (!normalized.isEmpty()) {
                return normalized;
            }
        }

        // Try _meta.tool
        if (toolCall.get_meta() != null) {
            Object metaTool = toolCall.get_meta().get("tool");
            if (metaTool instanceof String && !((String) metaTool).isBlank()) {
                return normalizeToolName((String) metaTool);
            }
        }

        // Try tool field directly
        if (toolCall.getTool() != null && !toolCall.getTool().isBlank()) {
            return normalizeToolName(toolCall.getTool());
        }

        // Try to extract from rawInput
        if (toolCall.getRawInput() != null && !toolCall.getRawInput().isBlank()) {
            String extracted = extractToolNameFromRawInput(toolCall.getRawInput());
            if (!extracted.isEmpty()) {
                return extracted;
            }
        }

        // Try type field
        if (toolCall.getType() != null && !toolCall.getType().isBlank()) {
            return normalizeToolName(toolCall.getType());
        }

        return "unknown";
    }

    /**
     * Classify a tool by its name.
     */
    private AcpApprovalClass classifyTool(String toolName) {
        if (toolName == null || toolName.isBlank()) {
            return AcpApprovalClass.UNKNOWN;
        }

        String normalized = toolName.toLowerCase().trim();

        // Check safe search tools (auto-approved)
        if (SAFE_SEARCH_TOOLS.contains(normalized)) {
            return AcpApprovalClass.READONLY_SEARCH;
        }

        // Check exec capable tools
        if (EXEC_CAPABLE_TOOLS.contains(normalized)) {
            return AcpApprovalClass.EXEC_CAPABLE;
        }

        // Check control plane tools
        if (CONTROL_PLANE_TOOLS.contains(normalized)) {
            return AcpApprovalClass.CONTROL_PLANE;
        }

        // Check interactive tools
        if (INTERACTIVE_TOOLS.contains(normalized)) {
            return AcpApprovalClass.INTERACTIVE;
        }

        // Check readonly scoped tools
        if (READONLY_SCOPED_TOOLS.contains(normalized)) {
            return AcpApprovalClass.READONLY_SCOPED;
        }

        // Check for patterns that suggest mutating operations
        if (looksLikeMutatingTool(normalized)) {
            return AcpApprovalClass.MUTATING;
        }

        return AcpApprovalClass.OTHER;
    }

    /**
     * Normalize a tool name for comparison.
     */
    private String normalizeToolName(String name) {
        if (name == null) {
            return "";
        }
        return name.toLowerCase().trim()
            .replaceAll("[^a-z0-9_]", "_")  // Replace special chars with underscore
            .replaceAll("_+", "_");          // Collapse multiple underscores
    }

    /**
     * Extract tool name from raw input string.
     */
    private String extractToolNameFromRawInput(String rawInput) {
        // Try to find tool name patterns like "tool": "name" or tool: name
        if (rawInput.contains("\"tool\"")) {
            int idx = rawInput.indexOf("\"tool\"");
            int colonIdx = rawInput.indexOf(":", idx);
            if (colonIdx > 0) {
                int quoteStart = rawInput.indexOf("\"", colonIdx);
                if (quoteStart > 0) {
                    int quoteEnd = rawInput.indexOf("\"", quoteStart + 1);
                    if (quoteEnd > quoteStart) {
                        return rawInput.substring(quoteStart + 1, quoteEnd).trim().toLowerCase();
                    }
                }
            }
        }
        return "";
    }

    /**
     * Check if a tool name suggests mutating operations.
     */
    private boolean looksLikeMutatingTool(String toolName) {
        Set<String> mutatingPatterns = Set.of(
            "write", "create", "delete", "update", "modify",
            "edit", "remove", "add", "insert", "append"
        );

        for (String pattern : mutatingPatterns) {
            if (toolName.contains(pattern)) {
                return true;
            }
        }
        return false;
    }
}
