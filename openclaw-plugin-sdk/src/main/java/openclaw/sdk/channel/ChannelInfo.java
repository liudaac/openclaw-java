package openclaw.sdk.channel;

import java.util.Map;
import java.util.Optional;

/**
 * Information about a channel.
 *
 * @param id the channel ID
 * @param name the channel name
 * @param type the channel type
 * @param status the channel status
 * @param metadata additional metadata
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public record ChannelInfo(
        String id,
        String name,
        ChannelType type,
        ChannelStatus status,
        Map<String, Object> metadata
) {

    /**
     * Channel type enumeration.
     */
    public enum ChannelType {
        DIRECT,
        GROUP,
        CHANNEL,
        THREAD,
        UNKNOWN
    }

    /**
     * Channel status.
     */
    public enum ChannelStatus {
        ACTIVE,
        INACTIVE,
        ERROR,
        PENDING
    }

    /**
     * Creates a builder for ChannelInfo.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for ChannelInfo.
     */
    public static class Builder {
        private String id;
        private String name;
        private ChannelType type = ChannelType.UNKNOWN;
        private ChannelStatus status = ChannelStatus.ACTIVE;
        private Map<String, Object> metadata = Map.of();

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder type(ChannelType type) {
            this.type = type;
            return this;
        }

        public Builder status(ChannelStatus status) {
            this.status = status;
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata != null ? metadata : Map.of();
            return this;
        }

        public ChannelInfo build() {
            return new ChannelInfo(id, name, type, status, metadata);
        }
    }
}
