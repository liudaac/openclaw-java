package openclaw.sdk.channel;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Channel outbound adapter for sending messages.
 *
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public interface ChannelOutboundAdapter {

    /**
     * Sends a text message.
     *
     * @param account the account
     * @param to the recipient
     * @param message the message text
     * @param options optional send options
     * @return the send result
     */
    CompletableFuture<SendResult> sendText(
            Object account,
            String to,
            String message,
            Optional<SendOptions> options
    );

    /**
     * Sends a message with media.
     *
     * @param account the account
     * @param to the recipient
     * @param message the message text
     * @param mediaUrl the media URL
     * @param options optional send options
     * @return the send result
     */
    CompletableFuture<SendResult> sendMedia(
            Object account,
            String to,
            Optional<String> message,
            String mediaUrl,
            Optional<SendOptions> options
    );

    /**
     * Sends a typing indicator.
     *
     * @param account the account
     * @param to the recipient
     * @return completion future
     */
    CompletableFuture<Void> sendTyping(Object account, String to);

    /**
     * Send options.
     *
     * @param replyTo message to reply to
     * @param threadId thread ID
     * @param metadata additional metadata
     * @param silent whether to send silently
     */
    record SendOptions(
            Optional<String> replyTo,
            Optional<String> threadId,
            Optional<Map<String, Object>> metadata,
            boolean silent
    ) {

        /**
         * Creates a builder for SendOptions.
         *
         * @return a new builder
         */
        public static Builder builder() {
            return new Builder();
        }

        /**
         * Builder for SendOptions.
         */
        public static class Builder {
            private String replyTo;
            private String threadId;
            private Map<String, Object> metadata;
            private boolean silent = false;

            public Builder replyTo(String replyTo) {
                this.replyTo = replyTo;
                return this;
            }

            public Builder threadId(String threadId) {
                this.threadId = threadId;
                return this;
            }

            public Builder metadata(Map<String, Object> metadata) {
                this.metadata = metadata;
                return this;
            }

            public Builder silent(boolean silent) {
                this.silent = silent;
                return this;
            }

            public SendOptions build() {
                return new SendOptions(
                        Optional.ofNullable(replyTo),
                        Optional.ofNullable(threadId),
                        Optional.ofNullable(metadata),
                        silent
                );
            }
        }
    }
}
