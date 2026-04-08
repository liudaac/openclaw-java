package openclaw.agent.heartbeat;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Heartbeat configuration properties.
 *
 * <p>Configures agent heartbeat behavior including interval, prompt, and response handling.</p>
 *
 * @author OpenClaw Team
 * @version 2026.3.17
 */
@Component
@ConfigurationProperties(prefix = "openclaw.agents.defaults.heartbeat")
public class HeartbeatConfig {

    /**
     * Default heartbeat prompt.
     * Guides the agent on how to handle heartbeat polls.
     */
    public static final String DEFAULT_HEARTBEAT_PROMPT =
        "Read HEARTBEAT.md if it exists (workspace context). Follow it strictly. " +
        "Do not infer or repeat old tasks from prior chats. " +
        "If nothing needs attention, reply HEARTBEAT_OK.";

    /**
     * Default heartbeat interval (30 minutes).
     */
    public static final Duration DEFAULT_HEARTBEAT_EVERY = Duration.ofMinutes(30);

    /**
     * Default max characters for HEARTBEAT_OK ack before treating as content.
     */
    public static final int DEFAULT_HEARTBEAT_ACK_MAX_CHARS = 300;

    /**
     * HEARTBEAT_OK token for agent responses.
     */
    public static final String HEARTBEAT_TOKEN = "HEARTBEAT_OK";

    /**
     * NO_REPLY token for silent responses.
     */
    public static final String SILENT_REPLY_TOKEN = "NO_REPLY";

    private boolean enabled = true;
    private Duration every = DEFAULT_HEARTBEAT_EVERY;
    private int ackMaxChars = DEFAULT_HEARTBEAT_ACK_MAX_CHARS;
    private String prompt = DEFAULT_HEARTBEAT_PROMPT;
    private String target = "last"; // "last", "none", or specific channel
    private boolean includeSystemPromptSection = true; // Include Heartbeats system prompt section

    public HeartbeatConfig() {
    }

    /**
     * Check if heartbeat is enabled.
     *
     * @return true if enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Set whether heartbeat is enabled.
     *
     * @param enabled true to enable
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Get heartbeat interval.
     *
     * @return the interval
     */
    public Duration getEvery() {
        return every;
    }

    /**
     * Set heartbeat interval.
     *
     * @param every the interval
     */
    public void setEvery(Duration every) {
        this.every = every;
    }

    /**
     * Get max characters for HEARTBEAT_OK ack.
     *
     * @return the max chars
     */
    public int getAckMaxChars() {
        return ackMaxChars;
    }

    /**
     * Set max characters for HEARTBEAT_OK ack.
     *
     * @param ackMaxChars the max chars
     */
    public void setAckMaxChars(int ackMaxChars) {
        this.ackMaxChars = Math.max(0, ackMaxChars);
    }

    /**
     * Get heartbeat prompt.
     *
     * @return the prompt
     */
    public String getPrompt() {
        return prompt != null && !prompt.trim().isEmpty() ? prompt : DEFAULT_HEARTBEAT_PROMPT;
    }

    /**
     * Set heartbeat prompt.
     *
     * @param prompt the prompt
     */
    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    /**
     * Get heartbeat target.
     *
     * @return the target ("last", "none", or channel)
     */
    public String getTarget() {
        return target;
    }

    /**
     * Set heartbeat target.
     *
     * @param target the target
     */
    public void setTarget(String target) {
        this.target = target;
    }

    /**
     * Check if system prompt section is included.
     *
     * @return true if included
     */
    public boolean isIncludeSystemPromptSection() {
        return includeSystemPromptSection;
    }

    /**
     * Set whether to include system prompt section.
     *
     * @param includeSystemPromptSection true to include
     */
    public void setIncludeSystemPromptSection(boolean includeSystemPromptSection) {
        this.includeSystemPromptSection = includeSystemPromptSection;
    }

    /**
     * Get the effective heartbeat prompt.
     *
     * @return the resolved prompt
     */
    public String resolvePrompt() {
        String trimmed = prompt != null ? prompt.trim() : "";
        return trimmed.isEmpty() ? DEFAULT_HEARTBEAT_PROMPT : trimmed;
    }

    @Override
    public String toString() {
        return String.format(
            "HeartbeatConfig{enabled=%s, every=%s, ackMaxChars=%d, target='%s', includeSystemPromptSection=%s}",
            enabled, every, ackMaxChars, target, includeSystemPromptSection
        );
    }
}
