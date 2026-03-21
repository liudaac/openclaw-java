package openclaw.agent.event;

import java.util.Objects;

/**
 * Context for an agent run.
 *
 * <p>Contains metadata about the agent run including session key,
 * verbose level, heartbeat status, and control UI visibility.</p>
 *
 * @author OpenClaw Team
 * @version 2026.3.21
 * @since 2026.3.21
 */
public final class AgentRunContext {

    private final String sessionKey;
    private final String verboseLevel;
    private final boolean heartbeat;
    private final boolean controlUiVisible;

    private AgentRunContext(Builder builder) {
        this.sessionKey = builder.sessionKey;
        this.verboseLevel = builder.verboseLevel;
        this.heartbeat = builder.heartbeat;
        this.controlUiVisible = builder.controlUiVisible;
    }

    /**
     * Gets the session key.
     *
     * @return the session key
     */
    public String getSessionKey() {
        return sessionKey;
    }

    /**
     * Gets the verbose level.
     *
     * @return the verbose level
     */
    public String getVerboseLevel() {
        return verboseLevel;
    }

    /**
     * Checks if this is a heartbeat run.
     *
     * @return true if heartbeat
     */
    public boolean isHeartbeat() {
        return heartbeat;
    }

    /**
     * Checks if control UI should be visible.
     *
     * @return true if visible
     */
    public boolean isControlUiVisible() {
        return controlUiVisible;
    }

    /**
     * Creates a new builder.
     *
     * @return the builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a copy of this context with the given session key.
     *
     * @param sessionKey the new session key
     * @return a new context
     */
    public AgentRunContext withSessionKey(String sessionKey) {
        return builder()
                .sessionKey(sessionKey)
                .verboseLevel(this.verboseLevel)
                .heartbeat(this.heartbeat)
                .controlUiVisible(this.controlUiVisible)
                .build();
    }

    /**
     * Creates a copy of this context with the given verbose level.
     *
     * @param verboseLevel the new verbose level
     * @return a new context
     */
    public AgentRunContext withVerboseLevel(String verboseLevel) {
        return builder()
                .sessionKey(this.sessionKey)
                .verboseLevel(verboseLevel)
                .heartbeat(this.heartbeat)
                .controlUiVisible(this.controlUiVisible)
                .build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AgentRunContext that = (AgentRunContext) o;
        return heartbeat == that.heartbeat &&
                controlUiVisible == that.controlUiVisible &&
                Objects.equals(sessionKey, that.sessionKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sessionKey, heartbeat);
    }

    @Override
    public String toString() {
        return "AgentRunContext{" +
                "sessionKey='" + sessionKey + '\'' +
                ", heartbeat=" + heartbeat +
                ", controlUiVisible=" + controlUiVisible +
                '}';
    }

    /**
     * Builder for AgentRunContext.
     */
    public static class Builder {
        private String sessionKey;
        private String verboseLevel;
        private boolean heartbeat;
        private boolean controlUiVisible = true;

        public Builder sessionKey(String sessionKey) {
            this.sessionKey = sessionKey;
            return this;
        }

        public Builder verboseLevel(String verboseLevel) {
            this.verboseLevel = verboseLevel;
            return this;
        }

        public Builder heartbeat(boolean heartbeat) {
            this.heartbeat = heartbeat;
            return this;
        }

        public Builder controlUiVisible(boolean controlUiVisible) {
            this.controlUiVisible = controlUiVisible;
            return this;
        }

        public AgentRunContext build() {
            return new AgentRunContext(this);
        }
    }
}
