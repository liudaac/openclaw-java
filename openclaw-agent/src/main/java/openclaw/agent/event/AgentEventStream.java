package openclaw.agent.event;

/**
 * Agent event stream types.
 *
 * <p>Defines the different streams of agent events that can be emitted
 * during an agent run lifecycle.</p>
 *
 * @author OpenClaw Team
 * @version 2026.3.21
 * @since 2026.3.21
 */
public enum AgentEventStream {
    /**
     * Lifecycle events (start, end, etc.)
     */
    LIFECYCLE("lifecycle"),

    /**
     * Tool execution events
     */
    TOOL("tool"),

    /**
     * Assistant message events
     */
    ASSISTANT("assistant"),

    /**
     * Error events
     */
    ERROR("error");

    private final String value;

    AgentEventStream(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    /**
     * Parse from string value.
     *
     * @param value the string value
     * @return the enum value or null if not found
     */
    public static AgentEventStream fromString(String value) {
        if (value == null) {
            return null;
        }
        for (AgentEventStream stream : values()) {
            if (stream.value.equalsIgnoreCase(value)) {
                return stream;
            }
        }
        // Allow custom stream types
        return null;
    }
}
