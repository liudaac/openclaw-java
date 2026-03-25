package openclaw.channel.feishu;

/**
 * Feishu group policy enumeration.
 * <p>
 * Defines how the bot handles messages in group chats.
 * Based on: extensions/feishu/src/policy.ts
 *
 * @author OpenClaw Team
 * @version 2026.3.25
 */
public enum FeishuGroupPolicy {
    /**
     * Open policy - all messages in the group are processed.
     * When groupPolicy is "open", requireMention defaults to false
     * so that non-text messages (e.g. images) that cannot carry
     * @-mentions are still delivered to the agent.
     */
    OPEN("open"),

    /**
     * Allowlist policy - only messages from allowed users are processed.
     */
    ALLOWLIST("allowlist"),

    /**
     * Disabled policy - group messages are not processed.
     */
    DISABLED("disabled"),

    /**
     * Allowall policy - same as OPEN, for backward compatibility.
     * @deprecated Use OPEN instead
     */
    ALLOWALL("allowall");

    private final String value;

    FeishuGroupPolicy(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    /**
     * Parse policy from string value.
     *
     * @param value the string value
     * @return the policy enum, defaults to ALLOWLIST if not recognized
     */
    public static FeishuGroupPolicy fromString(String value) {
        if (value == null) {
            return ALLOWLIST;
        }
        for (FeishuGroupPolicy policy : values()) {
            if (policy.value.equalsIgnoreCase(value)) {
                return policy;
            }
        }
        return ALLOWLIST;
    }

    /**
     * Check if this policy is effectively open (includes ALLOWALL).
     *
     * @return true if open or allowall
     */
    public boolean isOpen() {
        return this == OPEN || this == ALLOWALL;
    }
}
