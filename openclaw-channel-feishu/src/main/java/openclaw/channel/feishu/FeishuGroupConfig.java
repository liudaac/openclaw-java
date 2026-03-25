package openclaw.channel.feishu;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Feishu group configuration.
 * <p>
 * Per-group configuration for Feishu channel.
 * Based on: extensions/feishu/src/config-schema.ts (FeishuGroupSchema)
 *
 * @author OpenClaw Team
 * @version 2026.3.25
 */
public class FeishuGroupConfig {

    private Boolean requireMention;
    private ToolPolicy tools;
    private List<String> skills;
    private Boolean enabled;
    private List<String> allowFrom;
    private String systemPrompt;
    private String groupSessionScope;
    private String topicSessionMode;
    private String replyInThread;

    public FeishuGroupConfig() {
    }

    public Optional<Boolean> getRequireMention() {
        return Optional.ofNullable(requireMention);
    }

    public void setRequireMention(Boolean requireMention) {
        this.requireMention = requireMention;
    }

    public Optional<ToolPolicy> getTools() {
        return Optional.ofNullable(tools);
    }

    public void setTools(ToolPolicy tools) {
        this.tools = tools;
    }

    public Optional<List<String>> getSkills() {
        return Optional.ofNullable(skills);
    }

    public void setSkills(List<String> skills) {
        this.skills = skills;
    }

    public Optional<Boolean> getEnabled() {
        return Optional.ofNullable(enabled);
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Optional<List<String>> getAllowFrom() {
        return Optional.ofNullable(allowFrom);
    }

    public void setAllowFrom(List<String> allowFrom) {
        this.allowFrom = allowFrom;
    }

    public Optional<String> getSystemPrompt() {
        return Optional.ofNullable(systemPrompt);
    }

    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    public Optional<String> getGroupSessionScope() {
        return Optional.ofNullable(groupSessionScope);
    }

    public void setGroupSessionScope(String groupSessionScope) {
        this.groupSessionScope = groupSessionScope;
    }

    public Optional<String> getTopicSessionMode() {
        return Optional.ofNullable(topicSessionMode);
    }

    public void setTopicSessionMode(String topicSessionMode) {
        this.topicSessionMode = topicSessionMode;
    }

    public Optional<String> getReplyInThread() {
        return Optional.ofNullable(replyInThread);
    }

    public void setReplyInThread(String replyInThread) {
        this.replyInThread = replyInThread;
    }

    /**
     * Tool policy configuration.
     */
    public static class ToolPolicy {
        private List<String> allow;
        private List<String> deny;

        public Optional<List<String>> getAllow() {
            return Optional.ofNullable(allow);
        }

        public void setAllow(List<String> allow) {
            this.allow = allow;
        }

        public Optional<List<String>> getDeny() {
            return Optional.ofNullable(deny);
        }

        public void setDeny(List<String> deny) {
            this.deny = deny;
        }
    }
}
