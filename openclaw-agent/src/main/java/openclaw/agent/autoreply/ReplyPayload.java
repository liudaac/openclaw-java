package openclaw.agent.autoreply;

import java.util.Map;

/**
 * Payload for a reply message.
 *
 * @author OpenClaw Team
 * @version 2026.3.21
 * @since 2026.3.21
 */
public record ReplyPayload(
        String text,
        boolean replyToCurrent,
        String replyToMessageId,
        boolean isCompactionNotice,
        Map<String, Object> media,
        Map<String, Object> metadata
) {
    /**
     * Creates a simple text reply.
     *
     * @param text the text content
     * @return the reply payload
     */
    public static ReplyPayload text(String text) {
        return new ReplyPayload(text, false, null, false, null, null);
    }

    /**
     * Creates a reply that references the current message.
     *
     * @param text the text content
     * @return the reply payload
     */
    public static ReplyPayload replyToCurrent(String text) {
        return new ReplyPayload(text, true, null, false, null, null);
    }

    /**
     * Creates a compaction notice.
     *
     * @param text the text content
     * @return the reply payload
     */
    public static ReplyPayload compactionNotice(String text) {
        return new ReplyPayload(text, true, null, true, null, null);
    }

    /**
     * Checks if this payload has content to send.
     *
     * @return true if has content
     */
    public boolean hasContent() {
        return (text != null && !text.isBlank()) || (media != null && !media.isEmpty());
    }
}
