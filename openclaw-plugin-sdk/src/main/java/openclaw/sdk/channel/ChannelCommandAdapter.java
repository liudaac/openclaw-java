package openclaw.sdk.channel;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Channel command adapter for handling slash commands.
 *
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public interface ChannelCommandAdapter {

    /**
     * Gets available commands.
     *
     * @return list of command definitions
     */
    java.util.List<CommandDefinition> getCommands();

    /**
     * Handles a command.
     *
     * @param command the command name
     * @param context the command context
     * @return the command result
     */
    CompletableFuture<CommandResult> handleCommand(
            String command,
            CommandContext context
    );

    /**
     * Command definition.
     *
     * @param name the command name
     * @param description the command description
     * @param argsDescription the arguments description
     * @param requireAuth whether authentication is required
     * @param nativeNames native command name mappings
     */
    record CommandDefinition(
            String name,
            String description,
            String argsDescription,
            boolean requireAuth,
            Map<String, String> nativeNames
    ) {
    }

    /**
     * Command context.
     *
     * @param senderId the sender ID
     * @param channel the channel
     * @param channelId the channel ID
     * @param isAuthorizedSender whether the sender is authorized
     * @param args the command arguments
     * @param commandBody the full command body
     * @param from the from identifier
     * @param to the to identifier
     * @param accountId the account ID
     * @param messageThreadId the thread ID if in a thread
     */
    record CommandContext(
            String senderId,
            String channel,
            String channelId,
            boolean isAuthorizedSender,
            String args,
            String commandBody,
            String from,
            String to,
            String accountId,
            int messageThreadId
    ) {
    }

    /**
     * Command result.
     *
     * @param text the response text
     * @param ephemeral whether the response is ephemeral
     * @param metadata additional metadata
     */
    record CommandResult(
            String text,
            boolean ephemeral,
            Map<String, Object> metadata
    ) {

        /**
         * Creates a simple text result.
         *
         * @param text the text
         * @return the result
         */
        public static CommandResult text(String text) {
            return new CommandResult(text, false, Map.of());
        }

        /**
         * Creates an ephemeral result.
         *
         * @param text the text
         * @return the result
         */
        public static CommandResult ephemeral(String text) {
            return new CommandResult(text, true, Map.of());
        }
    }
}
