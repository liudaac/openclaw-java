package openclaw.sdk.channel;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Channel message action adapter for interactive message actions.
 *
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public interface ChannelMessageActionAdapter {

    /**
     * Gets available actions for a message.
     *
     * @param account the account
     * @param messageId the message ID
     * @return list of actions
     */
    CompletableFuture<java.util.List<MessageAction>> getActions(
            Object account,
            String messageId
    );

    /**
     * Handles a message action.
     *
     * @param account the account
     * @param messageId the message ID
     * @param actionId the action ID
     * @param payload the action payload
     * @return action result
     */
    CompletableFuture<ActionResult> handleAction(
            Object account,
            String messageId,
            String actionId,
            Map<String, Object> payload
    );

    /**
     * Message action.
     *
     * @param id the action ID
     * @param type the action type
     * @param label the action label
     * @param style the action style
     * @param url the URL if applicable
     */
    record MessageAction(
            String id,
            ActionType type,
            String label,
            ActionStyle style,
            Optional<String> url
    ) {
    }

    /**
     * Action type.
     */
    enum ActionType {
        BUTTON,
        LINK,
        SELECT,
        CHECKBOX,
        RADIO
    }

    /**
     * Action style.
     */
    enum ActionStyle {
        PRIMARY,
        SECONDARY,
        DANGER,
        SUCCESS
    }

    /**
     * Action result.
     *
     * @param success whether the action succeeded
     * @param message response message
     * @param updateMessage whether to update the original message
     * @param metadata additional metadata
     */
    record ActionResult(
            boolean succeeded,
            Optional<String> message,
            boolean updateMessage,
            Map<String, Object> metadata
    ) {

        /**
         * Creates a successful action result.
         *
         * @return the result
         */
        public static ActionResult success() {
            return new ActionResult(true, Optional.empty(), false, Map.of());
        }

        /**
         * Creates a successful action result with message.
         *
         * @param message the message
         * @return the result
         */
        public static ActionResult success(String message) {
            return new ActionResult(true, Optional.of(message), false, Map.of());
        }

        /**
         * Creates a failed action result.
         *
         * @param error the error message
         * @return the result
         */
        public static ActionResult failure(String error) {
            return new ActionResult(false, Optional.of(error), false, Map.of());
        }
    }
}
