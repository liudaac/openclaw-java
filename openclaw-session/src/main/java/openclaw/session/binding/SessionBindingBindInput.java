package openclaw.session.binding;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;

/**
 * Input for binding a session.
 *
 * @author OpenClaw Team
 * @version 2026.3.21
 * @since 2026.3.21
 */
public final class SessionBindingBindInput {

    private final String targetSessionKey;
    private final BindingTargetKind targetKind;
    private final ConversationRef conversation;
    private final SessionBindingPlacement placement;
    private final Map<String, Object> metadata;
    private final Duration ttl;

    private SessionBindingBindInput(Builder builder) {
        this.targetSessionKey = Objects.requireNonNull(builder.targetSessionKey, "targetSessionKey cannot be null");
        this.targetKind = Objects.requireNonNull(builder.targetKind, "targetKind cannot be null");
        this.conversation = Objects.requireNonNull(builder.conversation, "conversation cannot be null");
        this.placement = builder.placement;
        this.metadata = builder.metadata != null ? Map.copyOf(builder.metadata) : Map.of();
        this.ttl = builder.ttl;
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

    public SessionBindingPlacement getPlacement() {
        return placement;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public Duration getTtl() {
        return ttl;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return "SessionBindingBindInput{" +
                "targetSessionKey='" + targetSessionKey + '\'' +
                ", targetKind=" + targetKind +
                ", conversation=" + conversation +
                '}';
    }

    public static class Builder {
        private String targetSessionKey;
        private BindingTargetKind targetKind;
        private ConversationRef conversation;
        private SessionBindingPlacement placement;
        private Map<String, Object> metadata;
        private Duration ttl;

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

        public Builder placement(SessionBindingPlacement placement) {
            this.placement = placement;
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        public Builder ttl(Duration ttl) {
            this.ttl = ttl;
            return this;
        }

        public SessionBindingBindInput build() {
            return new SessionBindingBindInput(this);
        }
    }
}
