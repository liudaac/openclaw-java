package openclaw.sdk.channel;

import java.util.Optional;

/**
 * Result of sending a message.
 *
 * @param success whether the send was successful
 * @param messageId the message ID if successful
 * @param error the error message if failed
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public record SendResult(
        boolean success,
        Optional<String> messageId,
        Optional<String> error
) {

    /**
     * Creates a successful send result.
     *
     * @param messageId the message ID
     * @return the result
     */
    public static SendResult success(String messageId) {
        return new SendResult(true, Optional.of(messageId), Optional.empty());
    }

    /**
     * Creates a failed send result.
     *
     * @param error the error message
     * @return the result
     */
    public static SendResult failure(String error) {
        return new SendResult(false, Optional.empty(), Optional.of(error));
    }
}
