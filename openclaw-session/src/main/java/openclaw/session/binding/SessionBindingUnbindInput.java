package openclaw.session.binding;

import java.util.Objects;

/**
 * Input for unbinding sessions.
 *
 * @author OpenClaw Team
 * @version 2026.3.21
 * @since 2026.3.21
 */
public final class SessionBindingUnbindInput {

    private final String bindingId;
    private final String targetSessionKey;
    private final String reason;

    private SessionBindingUnbindInput(Builder builder) {
        this.bindingId = builder.bindingId;
        this.targetSessionKey = builder.targetSessionKey;
        this.reason = Objects.requireNonNull(builder.reason, "reason cannot be null");
    }

    public String getBindingId() {
        return bindingId;
    }

    public String getTargetSessionKey() {
        return targetSessionKey;
    }

    public String getReason() {
        return reason;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return "SessionBindingUnbindInput{" +
                "bindingId='" + bindingId + '\'' +
                ", targetSessionKey='" + targetSessionKey + '\'' +
                ", reason='" + reason + '\'' +
                '}';
    }

    public static class Builder {
        private String bindingId;
        private String targetSessionKey;
        private String reason;

        public Builder bindingId(String bindingId) {
            this.bindingId = bindingId;
            return this;
        }

        public Builder targetSessionKey(String targetSessionKey) {
            this.targetSessionKey = targetSessionKey;
            return this;
        }

        public Builder reason(String reason) {
            this.reason = reason;
            return this;
        }

        public SessionBindingUnbindInput build() {
            return new SessionBindingUnbindInput(this);
        }
    }
}
