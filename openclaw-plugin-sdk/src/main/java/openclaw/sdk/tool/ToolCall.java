package openclaw.sdk.tool;

import java.util.Map;

/**
 * Tool call representation.
 *
 * @param toolName the tool name
 * @param args the tool arguments
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public record ToolCall(
        String toolName,
        Map<String, Object> args
) {
    /**
     * Creates a tool call.
     *
     * @param toolName the tool name
     * @param args the arguments
     * @return the tool call
     */
    public static ToolCall of(String toolName, Map<String, Object> args) {
        return new ToolCall(toolName, args);
    }
}
