package openclaw.agent.event;

import java.time.Instant;
import java.util.Objects;

/**
 * Payload for heartbeat events.
 *
 * <p>Contains information about heartbeat message delivery status.</p>
 *
 * @author OpenClaw Team
 * @version 2026.3.21
 * @since 2026.3.21
 */
public final class HeartbeatEventPayload {

    private final Instant timestamp;
    private final HeartbeatStatus status;
    private final String to;
    private final String accountId;
    private final String preview;
    private final Long durationMs;
    private final Boolean hasMedia;
    private final String reason;
    private final String channel;
    private final Boolean silent;
    private final HeartbeatIndicatorType indicatorType;

    private HeartbeatEventPayload(Builder builder) {
        this.timestamp = Objects.requireNonNull(builder.timestamp, "timestamp cannot be null");
        this.status = Objects.requireNonNull(builder.status, "status cannot be null");
        this.to = builder.to;
        this.accountId = builder.accountId;
        this.preview = builder.preview;
        this.durationMs = builder.durationMs;
        this.hasMedia = builder.hasMedia;
        this.reason = builder.reason;
        this.channel = builder.channel;
        this.silent = builder.silent;
        this.indicatorType = builder.indicatorType != null 
                ? builder.indicatorType 
                : HeartbeatIndicatorType.resolveFromStatus(status);
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public HeartbeatStatus getStatus() {
        return status;
    }

    public String getTo() {
        return to;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getPreview() {
        return preview;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public Boolean getHasMedia() {
        return hasMedia;
    }

    public String getReason() {
        return reason;
    }

    public String getChannel() {
        return channel;
    }

    public Boolean getSilent() {
        return silent;
    }

    public HeartbeatIndicatorType getIndicatorType() {
        return indicatorType;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return "HeartbeatEventPayload{" +
                "status=" + status +
                ", channel='" + channel + '\'' +
                ", indicatorType=" + indicatorType +
                '}';
    }

    public static class Builder {
        private Instant timestamp = Instant.now();
        private HeartbeatStatus status;
        private String to;
        private String accountId;
        private String preview;
        private Long durationMs;
        private Boolean hasMedia;
        private String reason;
        private String channel;
        private Boolean silent;
        private HeartbeatIndicatorType indicatorType;

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder status(HeartbeatStatus status) {
            this.status = status;
            return this;
        }

        public Builder to(String to) {
            this.to = to;
            return this;
        }

        public Builder accountId(String accountId) {
            this.accountId = accountId;
            return this;
        }

        public Builder preview(String preview) {
            this.preview = preview;
            return this;
        }

        public Builder durationMs(Long durationMs) {
            this.durationMs = durationMs;
            return this;
        }

        public Builder hasMedia(Boolean hasMedia) {
            this.hasMedia = hasMedia;
            return this;
        }

        public Builder reason(String reason) {
            this.reason = reason;
            return this;
        }

        public Builder channel(String channel) {
            this.channel = channel;
            return this;
        }

        public Builder silent(Boolean silent) {
            this.silent = silent;
            return this;
        }

        public Builder indicatorType(HeartbeatIndicatorType indicatorType) {
            this.indicatorType = indicatorType;
            return this;
        }

        public HeartbeatEventPayload build() {
            return new HeartbeatEventPayload(this);
        }
    }
}
