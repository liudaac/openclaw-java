package openclaw.desktop.model;

import java.util.Map;

/**
 * Tool Execution Result Model.
 */
public record ToolExecutionResult(
    boolean success,
    String output,
    String error,
    Map<String, Object> metadata
) {
    public boolean isSuccess() {
        return success;
    }

    public boolean hasOutput() {
        return output != null && !output.isEmpty();
    }

    public boolean hasError() {
        return error != null && !error.isEmpty();
    }
}
