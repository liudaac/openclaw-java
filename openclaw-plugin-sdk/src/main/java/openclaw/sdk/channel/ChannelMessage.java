package openclaw.sdk.channel;

import java.util.Map;

/**
 * Channel message for inbound/outbound communication.
 *
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public record ChannelMessage(
        String text,
        String from,
        String fromName,
        String chatId,
        String messageId,
        long timestamp,
        Map<String, Object> metadata
) {
    /**
     * Builder for ChannelMessage.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String text;
        private String from;
        private String fromName;
        private String chatId;
        private String messageId;
        private long timestamp = System.currentTimeMillis();
        private Map<String, Object> metadata = Map.of();

        public Builder text(String text) {
            this.text = text;
            return this;
        }

        public Builder from(String from) {
            this.from = from;
            return this;
        }

        public Builder fromName(String fromName) {
            this.fromName = fromName;
            return this;
        }

        public Builder chatId(String chatId) {
            this.chatId = chatId;
            return this;
        }

        public Builder messageId(String messageId) {
            this.messageId = messageId;
            return this;
        }

        public Builder timestamp(long timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        public ChannelMessage build() {
            return new ChannelMessage(text, from, fromName, chatId, messageId, timestamp, metadata);
        }
    }
}
