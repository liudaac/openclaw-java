package openclaw.session.store;

import java.time.Instant;

/**
 * Session statistics.
 *
 * @param totalMessages total number of messages
 * @param totalTokens total token count
 * @param firstMessageAt timestamp of first message
 * @param lastMessageAt timestamp of last message
 */
public record SessionStats(
        int totalMessages,
        int totalTokens,
        Instant firstMessageAt,
        Instant lastMessageAt
) {
}
