package openclaw.sdk.channel;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Channel streaming adapter for real-time message streaming.
 *
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public interface ChannelStreamingAdapter {

    /**
     * Starts a streaming session.
     *
     * @param account the account
     * @param to the recipient
     * @param listener the streaming listener
     * @return the streaming session
     */
    CompletableFuture<StreamingSession> startStreaming(
            Object account,
            String to,
            StreamingListener listener
    );

    /**
     * Sends a streaming chunk.
     *
     * @param session the streaming session
     * @param chunk the text chunk
     * @return completion future
     */
    CompletableFuture<Void> sendChunk(StreamingSession session, String chunk);

    /**
     * Finalizes a streaming session.
     *
     * @param session the streaming session
     * @param finalText the final text
     * @return completion future
     */
    CompletableFuture<Void> finalizeStreaming(
            StreamingSession session,
            String finalText
    );

    /**
     * Cancels a streaming session.
     *
     * @param session the streaming session
     * @param reason the cancellation reason
     * @return completion future
     */
    CompletableFuture<Void> cancelStreaming(
            StreamingSession session,
            String reason
    );

    /**
     * Streaming session.
     *
     * @param sessionId the session ID
     * @param account the account
     * @param recipient the recipient
     * @param startTime the start timestamp
     */
    record StreamingSession(
            String sessionId,
            Object account,
            String recipient,
            long startTime
    ) {
    }

    /**
     * Streaming listener.
     */
    interface StreamingListener {

        /**
         * Called when a chunk is received.
         *
         * @param chunk the chunk
         */
        void onChunk(String chunk);

        /**
         * Called when streaming completes.
         *
         * @param finalText the final text
         */
        void onComplete(String finalText);

        /**
         * Called when streaming errors.
         *
         * @param error the error
         */
        void onError(Throwable error);

        /**
         * Called when streaming is cancelled.
         *
         * @param reason the reason
         */
        void onCancelled(String reason);
    }
}
