package openclaw.sdk.tool;

import java.util.Map;
import java.util.Optional;

/**
 * Tool execution result.
 *
 * @param success whether the execution succeeded
 * @param content the result content
 * @param error the error if failed
 * @param metadata additional metadata
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public record ToolResult(
        boolean success,
        Optional<String> content,
        Optional<String> error,
        Map<String, Object> metadata
) {

    /**
     * Creates a successful result.
     *
     * @param content the content
     * @return the result
     */
    public static ToolResult success(String content) {
        return new ToolResult(true, Optional.of(content), Optional.empty(), Map.of());
    }

    /**
     * Creates a successful result with metadata.
     *
     * @param content the content
     * @param metadata the metadata
     * @return the result
     */
    public static ToolResult success(String content, Map<String, Object> metadata) {
        return new ToolResult(true, Optional.of(content), Optional.empty(), metadata);
    }

    /**
     * Creates a failed result.
     *
     * @param error the error message
     * @return the result
     */
    public static ToolResult failure(String error) {
        return new ToolResult(false, Optional.empty(), Optional.of(error), Map.of());
    }

    /**
     * Creates a failed result with metadata.
     *
     * @param error the error message
     * @param metadata the metadata
     * @return the result
     */
    public static ToolResult failure(String error, Map<String, Object> metadata) {
        return new ToolResult(false, Optional.empty(), Optional.of(error), metadata);
    }

    /**
     * Creates an empty successful result.
     *
     * @return the result
     */
    public static ToolResult empty() {
        return new ToolResult(true, Optional.empty(), Optional.empty(), Map.of());
    }
}
