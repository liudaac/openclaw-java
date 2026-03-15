package openclaw.sdk.agent;

import java.util.Map;

/**
 * Message sent to/from an agent.
 *
 * @param role the message role (user, assistant, system)
 * @param content the message content
 * @param metadata additional metadata
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public record AgentMessage(
        String role,
        String content,
        Map<String, Object> metadata
) {
    /**
     * Creates a user message.
     *
     * @param content the content
     * @return the message
     */
    public static AgentMessage user(String content) {
        return new AgentMessage("user", content, Map.of());
    }

    /**
     * Creates an assistant message.
     *
     * @param content the content
     * @return the message
     */
    public static AgentMessage assistant(String content) {
        return new AgentMessage("assistant", content, Map.of());
    }

    /**
     * Creates a system message.
     *
     * @param content the content
     * @return the message
     */
    public static AgentMessage system(String content) {
        return new AgentMessage("system", content, Map.of());
    }

    /**
     * Creates a message with metadata.
     *
     * @param role the role
     * @param content the content
     * @param metadata the metadata
     * @return the message
     */
    public static AgentMessage of(String role, String content, Map<String, Object> metadata) {
        return new AgentMessage(role, content, metadata);
    }
}
