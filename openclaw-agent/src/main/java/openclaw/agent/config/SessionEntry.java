package openclaw.agent.config;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * Session entry for storing session state.
 *
 * @author OpenClaw Team
 * @version 2026.3.21
 */
public record SessionEntry(
        String sessionKey,
        String sessionId,
        String agentId,
        Instant createdAt,
        Instant lastAccessedAt,
        SessionStatus status,
        Map<String, Object> metadata
) {
    public enum SessionStatus {
        ACTIVE,
        PAUSED,
        COMPLETED,
        EXPIRED
    }

    public SessionEntry {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (lastAccessedAt == null) {
            lastAccessedAt = createdAt;
        }
        if (status == null) {
            status = SessionStatus.ACTIVE;
        }
        if (metadata == null) {
            metadata = Map.of();
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public SessionEntry withLastAccessedAt(Instant lastAccessedAt) {
        return new SessionEntry(
                sessionKey, sessionId, agentId, createdAt,
                lastAccessedAt, status, metadata
        );
    }

    public SessionEntry withStatus(SessionStatus status) {
        return new SessionEntry(
                sessionKey, sessionId, agentId, createdAt,
                lastAccessedAt, status, metadata
        );
    }

    public static class Builder {
        private String sessionKey;
        private String sessionId;
        private String agentId;
        private Instant createdAt;
        private Instant lastAccessedAt;
        private SessionStatus status;
        private Map<String, Object> metadata;

        public Builder sessionKey(String sessionKey) {
            this.sessionKey = sessionKey;
            return this;
        }

        public Builder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        public Builder agentId(String agentId) {
            this.agentId = agentId;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder lastAccessedAt(Instant lastAccessedAt) {
            this.lastAccessedAt = lastAccessedAt;
            return this;
        }

        public Builder status(SessionStatus status) {
            this.status = status;
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        public SessionEntry build() {
            return new SessionEntry(
                    sessionKey, sessionId, agentId, createdAt,
                    lastAccessedAt, status, metadata
            );
        }
    }
}
