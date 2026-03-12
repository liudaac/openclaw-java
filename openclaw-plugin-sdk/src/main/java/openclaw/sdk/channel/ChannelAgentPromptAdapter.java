package openclaw.sdk.channel;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Channel agent prompt adapter for customizing agent prompts.
 *
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public interface ChannelAgentPromptAdapter {

    /**
     * Gets system context for the agent.
     *
     * @param account the account
     * @param context the context
     * @return system context sections
     */
    CompletableFuture<SystemContext> getSystemContext(
            Object account,
            PromptContext context
    );

    /**
     * Gets channel-specific guidance.
     *
     * @param account the account
     * @return guidance text
     */
    CompletableFuture<Optional<String>> getChannelGuidance(Object account);

    /**
     * Gets formatting instructions.
     *
     * @param account the account
     * @return formatting instructions
     */
    CompletableFuture<Optional<String>> getFormattingInstructions(Object account);

    /**
     * Prompt context.
     *
     * @param sessionKey the session key
     * @param agentId the agent ID
     * @param channelId the channel ID
     * @param isGroup whether this is a group context
     * @param metadata additional metadata
     */
    record PromptContext(
            String sessionKey,
            String agentId,
            String channelId,
            boolean isGroup,
            Map<String, Object> metadata
    ) {
    }

    /**
     * System context.
     *
     * @param prepend sections to prepend
     * @param append sections to append
     */
    record SystemContext(
            Optional<String> prepend,
            Optional<String> append
    ) {

        /**
         * Creates empty system context.
         *
         * @return empty context
         */
        public static SystemContext empty() {
            return new SystemContext(Optional.empty(), Optional.empty());
        }

        /**
         * Creates context with prepend only.
         *
         * @param prepend the prepend text
         * @return the context
         */
        public static SystemContext prepend(String prepend) {
            return new SystemContext(Optional.of(prepend), Optional.empty());
        }

        /**
         * Creates context with append only.
         *
         * @param append the append text
         * @return the context
         */
        public static SystemContext append(String append) {
            return new SystemContext(Optional.empty(), Optional.of(append));
        }

        /**
         * Creates context with both prepend and append.
         *
         * @param prepend the prepend text
         * @param append the append text
         * @return the context
         */
        public static SystemContext both(String prepend, String append) {
            return new SystemContext(Optional.of(prepend), Optional.of(append));
        }
    }
}
