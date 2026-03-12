package openclaw.sdk.channel;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Request for sending a message.
 *
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public record SendMessageRequest(
        String channelId,
        String accountId,
        String to,
        Optional<String> message,
        Optional<List<Attachment>> attachments,
        Optional<Map<String, Object>> metadata,
        Optional<String> replyTo,
        Optional<String> threadId
) {

    /**
     * Creates a builder for SendMessageRequest.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for SendMessageRequest.
     */
    public static class Builder {
        private String channelId;
        private String accountId;
        private String to;
        private String message;
        private List<Attachment> attachments;
        private Map<String, Object> metadata;
        private String replyTo;
        private String threadId;

        public Builder channelId(String channelId) {
            this.channelId = channelId;
            return this;
        }

        public Builder accountId(String accountId) {
            this.accountId = accountId;
            return this;
        }

        public Builder to(String to) {
            this.to = to;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder attachments(List<Attachment> attachments) {
            this.attachments = attachments;
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        public Builder replyTo(String replyTo) {
            this.replyTo = replyTo;
            return this;
        }

        public Builder threadId(String threadId) {
            this.threadId = threadId;
            return this;
        }

        public SendMessageRequest build() {
            return new SendMessageRequest(
                    channelId,
                    accountId,
                    to,
                    Optional.ofNullable(message),
                    Optional.ofNullable(attachments),
                    Optional.ofNullable(metadata),
                    Optional.ofNullable(replyTo),
                    Optional.ofNullable(threadId)
            );
        }
    }

    /**
     * Message attachment.
     *
     * @param type the attachment type (image, file, etc.)
     * @param url the attachment URL or path
     * @param filename the filename
     * @param mimeType the MIME type
     */
    public record Attachment(
            String type,
            String url,
            String filename,
            Optional<String> mimeType
    ) {
    }
}
