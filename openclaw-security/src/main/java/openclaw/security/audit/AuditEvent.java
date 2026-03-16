package openclaw.security.audit;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Represents a security audit event.
 *
 * @author OpenClaw Team
 * @version 2026.3.14
 */
public record AuditEvent(
        String id,
        Instant timestamp,
        EventType type,
        EventSeverity severity,
        String actor,
        String action,
        String resource,
        EventStatus status,
        String message,
        Map<String, Object> details,
        String sourceIp,
        String sessionId,
        String toolName,
        Long durationMs
) {

    /**
     * Creates a new audit event builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for AuditEvent.
     */
    public static class Builder {
        private String id = UUID.randomUUID().toString();
        private Instant timestamp = Instant.now();
        private EventType type;
        private EventSeverity severity = EventSeverity.INFO;
        private String actor;
        private String action;
        private String resource;
        private EventStatus status = EventStatus.SUCCESS;
        private String message;
        private Map<String, Object> details = Map.of();
        private String sourceIp;
        private String sessionId;
        private String toolName;
        private Long durationMs;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder type(EventType type) {
            this.type = type;
            return this;
        }

        public Builder severity(EventSeverity severity) {
            this.severity = severity;
            return this;
        }

        public Builder actor(String actor) {
            this.actor = actor;
            return this;
        }

        public Builder action(String action) {
            this.action = action;
            return this;
        }

        public Builder resource(String resource) {
            this.resource = resource;
            return this;
        }

        public Builder status(EventStatus status) {
            this.status = status;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder details(Map<String, Object> details) {
            this.details = details != null ? details : Map.of();
            return this;
        }

        public Builder sourceIp(String sourceIp) {
            this.sourceIp = sourceIp;
            return this;
        }

        public Builder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        public Builder toolName(String toolName) {
            this.toolName = toolName;
            return this;
        }

        public Builder durationMs(Long durationMs) {
            this.durationMs = durationMs;
            return this;
        }

        public AuditEvent build() {
            return new AuditEvent(
                    id, timestamp, type, severity, actor, action, resource,
                    status, message, details, sourceIp, sessionId, toolName, durationMs
            );
        }
    }

    /**
     * Event types.
     */
    public enum EventType {
        TOOL_EXECUTION,      // Tool execution
        COMMAND_EXECUTION,   // Shell command execution
        FILE_ACCESS,         // File read/write
        NETWORK_ACCESS,      // Network request
        AUTHENTICATION,      // Login/logout
        AUTHORIZATION,       // Permission check
        CONFIG_CHANGE,       // Configuration modification
        SECURITY_VIOLATION,  // Security policy violation
        SYSTEM_EVENT         // System-level events
    }

    /**
     * Event severity levels.
     */
    public enum EventSeverity {
        DEBUG,      // Debug information
        INFO,       // Informational
        WARNING,    // Warning
        ERROR,      // Error
        CRITICAL    // Critical security event
    }

    /**
     * Event status.
     */
    public enum EventStatus {
        SUCCESS,    // Operation succeeded
        FAILURE,    // Operation failed
        BLOCKED,    // Operation blocked by policy
        PENDING     // Operation pending approval
    }
}
