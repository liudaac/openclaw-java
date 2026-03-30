package openclaw.channel.feishu.policy;

import openclaw.channel.feishu.FeishuGroupPolicy;

import java.util.List;

/**
 * Feishu policy configuration.
 * <p>
 * Defines the policy settings for Feishu channel.
 * Based on: extensions/feishu/src/policy.ts
 *
 * @author OpenClaw Team
 * @version 2026.3.25
 */
public class FeishuPolicy {

    private FeishuGroupPolicy groupPolicy;
    private boolean requireMention;
    private List<String> allowFrom;
    private List<String> groupAllowFrom;
    private String dmPolicy;
    private int historyLimit;
    private int mediaMaxMb;

    private FeishuPolicy() {
        // Default values
        this.groupPolicy = FeishuGroupPolicy.OPEN;
        this.requireMention = true;
        this.allowFrom = List.of();
        this.groupAllowFrom = List.of();
        this.dmPolicy = "pairing";
        this.historyLimit = 50;
        this.mediaMaxMb = 30;
    }

    /**
     * Creates a builder for FeishuPolicy.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Gets default policy.
     *
     * @return default policy
     */
    public static FeishuPolicy defaults() {
        return new FeishuPolicy();
    }

    public FeishuGroupPolicy getGroupPolicy() {
        return groupPolicy;
    }

    public boolean isRequireMention() {
        return requireMention;
    }

    public List<String> getAllowFrom() {
        return allowFrom;
    }

    public List<String> getGroupAllowFrom() {
        return groupAllowFrom;
    }

    public String getDmPolicy() {
        return dmPolicy;
    }

    public int getHistoryLimit() {
        return historyLimit;
    }

    public int getMediaMaxMb() {
        return mediaMaxMb;
    }

    /**
     * Builder for FeishuPolicy.
     */
    public static class Builder {
        private FeishuPolicy policy = new FeishuPolicy();

        public Builder groupPolicy(FeishuGroupPolicy groupPolicy) {
            policy.groupPolicy = groupPolicy;
            return this;
        }

        public Builder requireMention(boolean requireMention) {
            policy.requireMention = requireMention;
            return this;
        }

        public Builder allowFrom(List<String> allowFrom) {
            policy.allowFrom = allowFrom != null ? allowFrom : List.of();
            return this;
        }

        public Builder groupAllowFrom(List<String> groupAllowFrom) {
            policy.groupAllowFrom = groupAllowFrom != null ? groupAllowFrom : List.of();
            return this;
        }

        public Builder dmPolicy(String dmPolicy) {
            policy.dmPolicy = dmPolicy;
            return this;
        }

        public Builder historyLimit(int historyLimit) {
            policy.historyLimit = historyLimit;
            return this;
        }

        public Builder mediaMaxMb(int mediaMaxMb) {
            policy.mediaMaxMb = mediaMaxMb;
            return this;
        }

        public FeishuPolicy build() {
            return policy;
        }
    }
}
