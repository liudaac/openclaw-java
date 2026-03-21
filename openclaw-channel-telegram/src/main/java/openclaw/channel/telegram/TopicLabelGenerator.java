package openclaw.channel.telegram;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

/**
 * Generator for Telegram topic labels.
 *
 * <p>Uses LLM to generate concise, descriptive titles for DM topics
 * based on the user's first message.</p>
 *
 * @author OpenClaw Team
 * @version 2026.3.21
 * @since 2026.3.21
 */
public class TopicLabelGenerator {

    private static final Logger logger = LoggerFactory.getLogger(TopicLabelGenerator.class);

    private final LlmClient llmClient;

    public TopicLabelGenerator(LlmClient llmClient) {
        this.llmClient = llmClient;
    }

    /**
     * Generates a topic label from the user's first message.
     *
     * @param userMessage the user's first message
     * @param config the auto-topic-label config
     * @return CompletableFuture with the generated label or empty if failed
     */
    public CompletableFuture<String> generate(String userMessage, AutoTopicLabelConfig config) {
        if (!config.isEnabled()) {
            return CompletableFuture.completedFuture(null);
        }

        if (userMessage == null || userMessage.isBlank()) {
            logger.debug("Cannot generate topic label from empty message");
            return CompletableFuture.completedFuture(null);
        }

        String prompt = buildPrompt(userMessage, config.getPrompt());

        return llmClient.generate(prompt)
                .thenApply(response -> {
                    String label = sanitizeLabel(response);
                    if (label.isEmpty()) {
                        logger.debug("LLM returned empty label");
                        return null;
                    }
                    logger.debug("Generated topic label (len={}): {}", label.length(), label);
                    return label;
                })
                .exceptionally(e -> {
                    logger.warn("Failed to generate topic label: {}", e.getMessage());
                    return null;
                });
    }

    /**
     * Builds the prompt for the LLM.
     */
    private String buildPrompt(String userMessage, String customPrompt) {
        StringBuilder sb = new StringBuilder();

        if (customPrompt != null && !customPrompt.isBlank()) {
            sb.append(customPrompt.trim());
        } else {
            sb.append(DEFAULT_SYSTEM_PROMPT);
        }

        sb.append("\n\nUser message:\n");
        // Truncate long messages
        String truncated = userMessage.length() > 500
                ? userMessage.substring(0, 500) + "..."
                : userMessage;
        sb.append(truncated);

        sb.append("\n\nGenerate a concise title (max 30 chars):");

        return sb.toString();
    }

    /**
     * Sanitizes the generated label.
     */
    private String sanitizeLabel(String label) {
        if (label == null) {
            return "";
        }

        // Remove quotes if present
        String sanitized = label.trim();
        if (sanitized.startsWith("\"") && sanitized.endsWith("\"")) {
            sanitized = sanitized.substring(1, sanitized.length() - 1);
        }
        if (sanitized.startsWith("'") && sanitized.endsWith("'")) {
            sanitized = sanitized.substring(1, sanitized.length() - 1);
        }

        // Remove newlines and extra spaces
        sanitized = sanitized.replaceAll("\\s+", " ").trim();

        // Truncate to max length
        if (sanitized.length() > MAX_LABEL_LENGTH) {
            sanitized = sanitized.substring(0, MAX_LABEL_LENGTH).trim();
        }

        return sanitized;
    }

    private static final int MAX_LABEL_LENGTH = 30;

    private static final String DEFAULT_SYSTEM_PROMPT = """
            You are a helpful assistant that generates concise, descriptive titles for conversations.
            
            Rules:
            - Maximum 30 characters
            - No special characters (emojis, markdown, etc.)
            - Should describe the topic clearly
            - Use the same language as the user's message
            - Be concise but informative
            
            Examples:
            - User: "How do I deploy to Kubernetes?" -> "Kubernetes Deployment"
            - User: "Help me write a Python script" -> "Python Script Help"
            - User: "What's the weather like?" -> "Weather Question"
            """;

    /**
     * LLM client interface.
     */
    public interface LlmClient {
        /**
         * Generates text from a prompt.
         *
         * @param prompt the prompt
         * @return CompletableFuture with the generated text
         */
        CompletableFuture<String> generate(String prompt);
    }
}
