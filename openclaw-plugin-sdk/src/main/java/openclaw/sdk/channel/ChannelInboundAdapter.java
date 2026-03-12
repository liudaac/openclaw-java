package openclaw.sdk.channel;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Channel inbound adapter for receiving messages.
 *
 * <p>This adapter handles incoming messages from the channel (e.g., webhook).</p>
 *
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public interface ChannelInboundAdapter {

    /**
     * Process an incoming message.
     *
     * @param message the incoming message
     * @return the processing result
     */
    CompletableFuture<ProcessResult> onMessage(ChannelMessage message);

    /**
     * Register a message handler.
     *
     * @param handler the handler to register
     */
    void onMessage(Consumer<ChannelMessage> handler);

    /**
     * Remove a message handler.
     *
     * @param handler the handler to remove
     */
    void removeHandler(Consumer<ChannelMessage> handler);

    /**
     * Processing result.
     *
     * @param success whether processing succeeded
     * @param error error message if failed
     */
    record ProcessResult(boolean success, String error) {
        /**
         * Creates a successful result.
         *
         * @return the result
         */
        public static ProcessResult success() {
            return new ProcessResult(true, null);
        }

        /**
         * Creates a failed result.
         *
         * @param error the error message
         * @return the result
         */
        public static ProcessResult failure(String error) {
            return new ProcessResult(false, error);
        }
    }
}
