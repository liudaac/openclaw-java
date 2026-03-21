package openclaw.agent.autoreply;

import openclaw.agent.config.SessionEntry;

import java.util.Map;
import java.util.function.Consumer;

/**
 * Options for creating a FollowupRunner.
 *
 * @author OpenClaw Team
 * @version 2026.3.21
 * @since 2026.3.21
 */
public record FollowupRunnerOptions(
        GetReplyOptions opts,
        TypingMode typingMode,
        SessionEntry sessionEntry,
        Map<String, SessionEntry> sessionStore,
        String sessionKey,
        String storePath,
        String defaultModel,
        Integer agentCfgContextTokens
) {
    /**
     * Options for getting a reply.
     */
    public record GetReplyOptions(
            boolean isHeartbeat,
            boolean suppressToolErrorWarnings,
            Consumer<ReplyPayload> onBlockReply
    ) {}

    /**
     * Typing mode configuration.
     */
    public enum TypingMode {
        OFF,
        ON,
        AUTO
    }

    /**
     * Gets the onBlockReply handler.
     *
     * @return the handler or null
     */
    public Consumer<ReplyPayload> onBlockReply() {
        return opts != null ? opts.onBlockReply() : null;
    }

    /**
     * Checks if this is a heartbeat run.
     *
     * @return true if heartbeat
     */
    public boolean isHeartbeat() {
        return opts != null && opts.isHeartbeat();
    }

    /**
     * Builder for FollowupRunnerOptions.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private GetReplyOptions opts;
        private TypingMode typingMode = TypingMode.AUTO;
        private SessionEntry sessionEntry;
        private Map<String, SessionEntry> sessionStore;
        private String sessionKey;
        private String storePath;
        private String defaultModel;
        private Integer agentCfgContextTokens;

        public Builder opts(GetReplyOptions opts) {
            this.opts = opts;
            return this;
        }

        public Builder typingMode(TypingMode typingMode) {
            this.typingMode = typingMode;
            return this;
        }

        public Builder sessionEntry(SessionEntry sessionEntry) {
            this.sessionEntry = sessionEntry;
            return this;
        }

        public Builder sessionStore(Map<String, SessionEntry> sessionStore) {
            this.sessionStore = sessionStore;
            return this;
        }

        public Builder sessionKey(String sessionKey) {
            this.sessionKey = sessionKey;
            return this;
        }

        public Builder storePath(String storePath) {
            this.storePath = storePath;
            return this;
        }

        public Builder defaultModel(String defaultModel) {
            this.defaultModel = defaultModel;
            return this;
        }

        public Builder agentCfgContextTokens(Integer agentCfgContextTokens) {
            this.agentCfgContextTokens = agentCfgContextTokens;
            return this;
        }

        public FollowupRunnerOptions build() {
            return new FollowupRunnerOptions(
                    opts, typingMode, sessionEntry, sessionStore,
                    sessionKey, storePath, defaultModel, agentCfgContextTokens
            );
        }
    }
}
