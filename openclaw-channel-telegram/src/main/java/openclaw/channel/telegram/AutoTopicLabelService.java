package openclaw.channel.telegram;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

/**
 * Service for auto-renaming Telegram DM topics on first message.
 *
 * @author OpenClaw Team
 * @version 2026.3.21
 * @since 2026.3.21
 */
public class AutoTopicLabelService {

    private static final Logger logger = LoggerFactory.getLogger(AutoTopicLabelService.class);

    private final TopicLabelGenerator labelGenerator;
    private final TelegramApiClient apiClient;

    public AutoTopicLabelService(TopicLabelGenerator labelGenerator, TelegramApiClient apiClient) {
        this.labelGenerator = labelGenerator;
        this.apiClient = apiClient;
    }

    /**
     * Checks if auto-topic-label should be applied.
     *
     * @param isGroup whether this is a group chat
     * @param isDmTopic whether this is a DM topic
     * @param config the auto-topic-label config
     * @return true if should apply
     */
    public boolean shouldApply(boolean isGroup, boolean isDmTopic, AutoTopicLabelConfig config) {
        if (!config.isEnabled()) {
            return false;
        }
        // Only apply to DM topics (not groups)
        return !isGroup && isDmTopic;
    }

    /**
     * Checks if this is the first turn in the session.
     *
     * @param sessionStore the session store
     * @param sessionKey the session key
     * @return true if first turn
     */
    public boolean isFirstTurnInSession(SessionStore sessionStore, String sessionKey) {
        if (sessionStore == null || sessionKey == null || sessionKey.isBlank()) {
            logger.debug("Session store or key is absent, skipping first-turn detection");
            return false;
        }

        try {
            SessionEntry entry = sessionStore.getEntry(sessionKey);
            // First turn if no entry exists or no system messages sent
            return entry == null || !entry.hasSystemSent();
        } catch (Exception e) {
            logger.debug("Session store error: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Applies auto-topic-label to a DM topic.
     *
     * @param chatId the chat ID
     * @param topicThreadId the topic thread ID
     * @param userMessage the user's first message
     * @param config the auto-topic-label config
     * @return CompletableFuture that completes when done
     */
    public CompletableFuture<Void> apply(
            long chatId,
            int topicThreadId,
            String userMessage,
            AutoTopicLabelConfig config) {

        if (!config.isEnabled()) {
            return CompletableFuture.completedFuture(null);
        }

        logger.debug("Applying auto-topic-label for chat {}/{}", chatId, topicThreadId);

        return labelGenerator.generate(userMessage, config)
                .thenCompose(label -> {
                    if (label == null || label.isBlank()) {
                        logger.debug("No label generated, skipping rename");
                        return CompletableFuture.completedFuture(null);
                    }

                    return apiClient.editForumTopic(chatId, topicThreadId, label)
                            .thenRun(() -> {
                                logger.debug("Renamed topic {}/{} to: {}", 
                                        chatId, topicThreadId, label);
                            })
                            .exceptionally(e -> {
                                logger.warn("Failed to rename topic {}/{}: {}", 
                                        chatId, topicThreadId, e.getMessage());
                                return null;
                            });
                });
    }

    /**
     * Session store interface.
     */
    public interface SessionStore {
        /**
         * Gets a session entry.
         *
         * @param sessionKey the session key
         * @return the entry or null
         */
        SessionEntry getEntry(String sessionKey);
    }

    /**
     * Session entry.
     */
    public interface SessionEntry {
        /**
         * Checks if system messages have been sent.
         *
         * @return true if sent
         */
        boolean hasSystemSent();
    }

    /**
     * Telegram API client interface.
     */
    public interface TelegramApiClient {
        /**
         * Edits a forum topic.
         *
         * @param chatId the chat ID
         * @param topicThreadId the topic thread ID
         * @param name the new name
         * @return CompletableFuture that completes when done
         */
        CompletableFuture<Void> editForumTopic(long chatId, int topicThreadId, String name);
    }
}
