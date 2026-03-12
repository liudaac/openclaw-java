package openclaw.sdk.channel;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Channel messaging adapter for message operations.
 *
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public interface ChannelMessagingAdapter {

    /**
     * Edits a message.
     *
     * @param account the account
     * @param messageId the message ID
     * @param newText the new text
     * @return the edit result
     */
    CompletableFuture<EditResult> editMessage(
            Object account,
            String messageId,
            String newText
    );

    /**
     * Deletes a message.
     *
     * @param account the account
     * @param messageId the message ID
     * @return completion future
     */
    CompletableFuture<Void> deleteMessage(Object account, String messageId);

    /**
     * Gets a message.
     *
     * @param account the account
     * @param messageId the message ID
     * @return the message if found
     */
    CompletableFuture<Optional<Message>> getMessage(Object account, String messageId);

    /**
     * Adds a reaction to a message.
     *
     * @param account the account
     * @param messageId the message ID
     * @param reaction the reaction emoji
     * @return completion future
     */
    CompletableFuture<Void> addReaction(
            Object account,
            String messageId,
            String reaction
    );

    /**
     * Removes a reaction from a message.
     *
     * @param account the account
     * @param messageId the message ID
     * @param reaction the reaction emoji
     * @return completion future
     */
    CompletableFuture<Void> removeReaction(
            Object account,
            String messageId,
            String reaction
    );

    /**
     * Message information.
     *
     * @param id the message ID
     * @param text the message text
     * @param senderId the sender ID
     * @param senderName the sender name
     * @param timestamp the timestamp
     * @param edited whether the message was edited
     * @param replyTo the replied message ID if any
     * @param metadata additional metadata
     */
    record Message(
            String id,
            String text,
            String senderId,
            String senderName,
            long timestamp,
            boolean edited,
            Optional<String> replyTo,
            Map<String, Object> metadata
    ) {
    }

    /**
     * Edit result.
     *
     * @param success whether the edit succeeded
     * @param messageId the message ID
     * @param error error message if failed
     */
    record EditResult(
            boolean success,
            String messageId,
            Optional<String> error
    ) {

        /**
         * Creates a successful edit result.
         *
         * @param messageId the message ID
         * @return the result
         */
        public static EditResult success(String messageId) {
            return new EditResult(true, messageId, Optional.empty());
        }

        /**
         * Creates a failed edit result.
         *
         * @param error the error message
         * @return the result
         */
        public static EditResult failure(String error) {
            return new EditResult(false, null, Optional.of(error));
        }
    }
}
