package openclaw.sdk.core;

import java.util.Optional;

/**
 * Parameters for spawning a subagent session.
 *
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public record SubagentRunParams(
        String sessionKey,
        String message,
        Optional<String> extraSystemPrompt,
        Optional<String> lane,
        boolean deliver,
        Optional<String> idempotencyKey
) {

    /**
     * Creates a builder for SubagentRunParams.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for SubagentRunParams.
     */
    public static class Builder {
        private String sessionKey;
        private String message;
        private String extraSystemPrompt;
        private String lane;
        private boolean deliver = true;
        private String idempotencyKey;

        public Builder sessionKey(String sessionKey) {
            this.sessionKey = sessionKey;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder extraSystemPrompt(String extraSystemPrompt) {
            this.extraSystemPrompt = extraSystemPrompt;
            return this;
        }

        public Builder lane(String lane) {
            this.lane = lane;
            return this;
        }

        public Builder deliver(boolean deliver) {
            this.deliver = deliver;
            return this;
        }

        public Builder idempotencyKey(String idempotencyKey) {
            this.idempotencyKey = idempotencyKey;
            return this;
        }

        public SubagentRunParams build() {
            return new SubagentRunParams(
                    sessionKey,
                    message,
                    Optional.ofNullable(extraSystemPrompt),
                    Optional.ofNullable(lane),
                    deliver,
                    Optional.ofNullable(idempotencyKey)
            );
        }
    }
}
