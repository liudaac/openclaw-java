package openclaw.session.binding;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Record of a session binding.
 *
 * @author OpenClaw Team
 * @version 2026.3.21
 * @since 2026.3.21
 */
public final class SessionBindingRecord {

    private final String bindingId;
    private final String targetSessionKey;
    private final BindingTargetKind targetKind;
    private final ConversationRef conversation;
    private final BindingStatus status;
    private final Instant boundAt;
    private final Instant expiresAt;
    private final Map<String, Object> metadata;

    private SessionBindingRecord(Builder builder) {
        this.bindingId = Objects.requireNonNull(builder.bindingId, "bindingId cannot be null");
        this.targetSessionKey = Objects.requireNonNull(builder.targetSessionKey, "targetSessionKey cannot be null");
        this.targetKind = Objects.requireNonNull(builder.targetKind, "targetKind cannot be null");
        this.conversation = Objects.requireNonNull(builder.conversation, "conversation cannot be null");
        this.status = Objects.requireNonNull(builder.status, "status cannot be null");
        this.boundAt = Objects.requireNonNull(builder.boundAt, "boundAt cannot be null");
        this.expiresAt = builder.expiresAt;
        this.metadata = builder.metadata != null ? Map.copyOf(builder.metadata) : Map.of();
    }

    public String getBindingId() {
        return bindingId;
    }

    public String getTargetSessionKey() {
        return targetSessionKey;
    }

    public BindingTargetKind getTargetKind() {
        return targetKind;
    }

    public ConversationRef getConversation() {
        return conversation;
    }

    public BindingStatus getStatus() {
        return status;
    }

    public Instant getBoundAt() {
        return boundAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    /**
     * Creates a copy with updated status.
     *
     * @param newStatus the new status
     * @return a new record with updated status
     */
    public SessionBindingRecord withStatus(BindingStatus newStatus) {
        return builder()
                .bindingId(bindingId)
                .targetSessionKey(targetSessionKey)
                .targetKind(targetKind)
                .conversation(conversation)
                .status(newStatus)
                .boundAt(boundAt)
                .expiresAt(expiresAt)
                .metadata(metadata)
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SessionBindingRecord that = (SessionBindingRecord) o;
        return Objects.equals(bindingId, that.bindingId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bindingId);
    }

    @Override
    public String toString() {
        return "SessionBindingRecord{" +
                "bindingId='" + bindingId + '\'' +
                ", targetSessionKey='" + targetSessionKey + '\'' +
                ", status=" + status +
                '}';
    }

    public static class Builder {
        private String bindingId;
        private String targetSessionKey;
        private BindingTargetKind targetKind;
        private ConversationRef conversation;
        private BindingStatus status;
        private Instant boundAt;
        private Instant expiresAt;
        private Map<String, Object> metadata;

        public Builder bindingId(String bindingId) {
            this.bindingId = bindingId;
            return this;
        }

        public Builder targetSessionKey(String targetSessionKey) {
            this.targetSessionKey = targetSessionKey;
            return this;
        }

        public Builder targetKind(BindingTargetKind targetKind) {
            this.targetKind = targetKind;
            return this;
        }

        public Builder conversation(ConversationRef conversation) {
            this.conversation = conversation;
            return this;
        }

        public Builder status(BindingStatus status) {
            this.status = status;
            return this;
        }

        public Builder boundAt(Instant boundAt) {
            this.boundAt = boundAt;
            return this;
        }

        public Builder expiresAt(Instant expiresAt) {
            this.expiresAt = expiresAt;
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        public SessionBindingRecord build() {
            return new SessionBindingRecord(this);
        }
    }
}
