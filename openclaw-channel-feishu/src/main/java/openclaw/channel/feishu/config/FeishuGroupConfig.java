package openclaw.channel.feishu.config;

import openclaw.channel.feishu.policy.FeishuGroupPolicy;

import java.util.List;
import java.util.Optional;

/**
 * Feishu group configuration.
 *
 * <p>Defines configuration for a specific Feishu group/chat.</p>
 *
 * <p>Ported from original TypeScript: extensions/feishu/src/types.ts</p>
 *
 * @author OpenClaw Team
 * @version 2026.3.25
 */
public class FeishuGroupConfig {

    private final String groupId;
    private final Boolean requireMention;
    private final FeishuGroupPolicy groupPolicy;
    private final List<String> allowFrom;
    private final String topicSessionMode;

    private FeishuGroupConfig(Builder builder) {
        this.groupId = builder.groupId;
        this.requireMention = builder.requireMention;
        this.groupPolicy = builder.groupPolicy;
        this.allowFrom = builder.allowFrom;
        this.topicSessionMode = builder.topicSessionMode;
    }

    /**
     * Creates a builder for FeishuGroupConfig.
     */
    public static Builder builder() {
        return new Builder();
    }

    public String getGroupId() {
        return groupId;
    }

    public Optional<Boolean> getRequireMention() {
        return Optional.ofNullable(requireMention);
    }

    public boolean isRequireMention() {
        return requireMention != null ? requireMention : true;
    }

    public FeishuGroupPolicy getGroupPolicy() {
        return groupPolicy != null ? groupPolicy : FeishuGroupPolicy.OPEN;
    }

    public List<String> getAllowFrom() {
        return allowFrom != null ? allowFrom : List.of();
    }

    public String getTopicSessionMode() {
        return topicSessionMode != null ? topicSessionMode : "disabled";
    }

    /**
     * Builder for FeishuGroupConfig.
     */
    public static class Builder {
        private String groupId;
        private Boolean requireMention;
        private FeishuGroupPolicy groupPolicy;
        private List<String> allowFrom;
        private String topicSessionMode;

        public Builder groupId(String groupId) {
            this.groupId = groupId;
            return this;
        }

        public Builder requireMention(Boolean requireMention) {
            this.requireMention = requireMention;
            return this;
        }

        public Builder groupPolicy(FeishuGroupPolicy groupPolicy) {
            this.groupPolicy = groupPolicy;
            return this;
        }

        public Builder allowFrom(List<String> allowFrom) {
            this.allowFrom = allowFrom != null ? List.copyOf(allowFrom) : null;
            return this;
        }

        public Builder topicSessionMode(String topicSessionMode) {
            this.topicSessionMode = topicSessionMode;
            return this;
        }

        public FeishuGroupConfig build() {
            if (groupId == null || groupId.isBlank()) {
                throw new IllegalArgumentException("groupId is required");
            }
            return new FeishuGroupConfig(this);
        }
    }

    @Override
    public String toString() {
        return "FeishuGroupConfig{" +
                "groupId='" + groupId + '\'' +
                ", requireMention=" + requireMention +
                ", groupPolicy=" + groupPolicy +
                ", allowFrom=" + allowFrom +
                ", topicSessionMode='" + topicSessionMode + '\'' +
                '}';
    }
}
