package openclaw.sdk.channel;

import java.util.Map;

/**
 * Outbound message for channel communication.
 *
 * @param content the message content
 * @param metadata additional metadata
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public record OutboundMessage(
        String content,
        Map<String, Object> metadata
) {
    /**
     * Creates a simple outbound message.
     *
     * @param content the content
     * @return the message
     */
    public static OutboundMessage of(String content) {
        return new OutboundMessage(content, Map.of());
    }

    /**
     * Creates an outbound message with metadata.
     *
     * @param content the content
     * @param metadata the metadata
     * @return the message
     */
    public static OutboundMessage of(String content, Map<String, Object> metadata) {
        return new OutboundMessage(content, metadata);
    }
}
