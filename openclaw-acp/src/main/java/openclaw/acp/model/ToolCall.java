package openclaw.acp.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Represents a tool call from the agent.
 * Mirrors the structure of tool calls in the ACP system.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ToolCall {
    private String id;
    private String type;
    private String title;
    private String tool;
    private Map<String, Object> arguments;
    private Map<String, Object> _meta;
    private String rawInput;
}
