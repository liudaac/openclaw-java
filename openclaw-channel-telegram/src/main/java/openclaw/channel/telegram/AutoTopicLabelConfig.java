package openclaw.channel.telegram;

import java.util.Objects;

/**
 * Configuration for auto-topic-label feature.
 *
 * <p>Controls whether and how to automatically rename DM topics
 * on first message.</p>
 *
 * @author OpenClaw Team
 * @version 2026.3.21
 * @since 2026.3.21
 */
public final class AutoTopicLabelConfig {

    private final boolean enabled;
    private final String prompt;

    private AutoTopicLabelConfig(boolean enabled, String prompt) {
        this.enabled = enabled;
        this.prompt = prompt;
    }

    /**
     * Checks if auto-topic-label is enabled.
     *
     * @return true if enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Gets the prompt for generating topic labels.
     *
     * @return the prompt or null
     */
    public String getPrompt() {
        return prompt;
    }

    /**
     * Creates a disabled config.
     *
     * @return disabled config
     */
    public static AutoTopicLabelConfig disabled() {
        return new AutoTopicLabelConfig(false, null);
    }

    /**
     * Creates an enabled config with default prompt.
     *
     * @return enabled config
     */
    public static AutoTopicLabelConfig enabled() {
        return new AutoTopicLabelConfig(true, DEFAULT_PROMPT);
    }

    /**
     * Creates an enabled config with custom prompt.
     *
     * @param prompt the custom prompt
     * @return enabled config
     */
    public static AutoTopicLabelConfig withPrompt(String prompt) {
        return new AutoTopicLabelConfig(true, Objects.requireNonNull(prompt, "prompt cannot be null"));
    }

    /**
     * Resolves config from direct and account settings.
     *
     * @param direct the direct config (higher priority)
     * @param account the account config (lower priority)
     * @return resolved config
     */
    public static AutoTopicLabelConfig resolve(Boolean direct, Boolean account) {
        // Direct config takes priority
        if (direct != null) {
            return direct ? enabled() : disabled();
        }
        // Fall back to account config
        if (account != null) {
            return account ? enabled() : disabled();
        }
        // Default: disabled
        return disabled();
    }

    /**
     * Resolves config from direct and account settings with custom prompt.
     *
     * @param direct the direct config
     * @param account the account config
     * @param customPrompt the custom prompt
     * @return resolved config
     */
    public static AutoTopicLabelConfig resolve(Boolean direct, Boolean account, String customPrompt) {
        AutoTopicLabelConfig base = resolve(direct, account);
        if (!base.enabled || customPrompt == null || customPrompt.isBlank()) {
            return base;
        }
        return withPrompt(customPrompt);
    }

    private static final String DEFAULT_PROMPT = """
            Generate a concise, descriptive title (max 30 characters) for this conversation
            based on the user's first message. The title should be:
            - Short and clear
            - Descriptive of the topic
            - Without special characters
            - In the same language as the user's message
            """;

    @Override
    public String toString() {
        return "AutoTopicLabelConfig{" +
                "enabled=" + enabled +
                ", prompt=" + (prompt != null ? "custom" : "default") +
                '}';
    }
}
