package openclaw.sdk.channel;

import java.util.concurrent.CompletableFuture;

/**
 * Channel heartbeat adapter for periodic health checks.
 *
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public interface ChannelHeartbeatAdapter {

    /**
     * Performs a heartbeat check.
     *
     * @param account the account
     * @return the heartbeat result
     */
    CompletableFuture<HeartbeatResult> heartbeat(Object account);

    /**
     * Gets the heartbeat interval in milliseconds.
     *
     * @return the interval
     */
    long getHeartbeatIntervalMs();

    /**
     * Heartbeat result.
     *
     * @param healthy whether the channel is healthy
     * @param message status message
     * @param shouldReconnect whether reconnection is needed
     */
    record HeartbeatResult(
            boolean healthy,
            String message,
            boolean shouldReconnect
    ) {

        /**
         * Creates a healthy heartbeat result.
         *
         * @return the result
         */
        public static HeartbeatResult healthy() {
            return new HeartbeatResult(true, "Channel is healthy", false);
        }

        /**
         * Creates an unhealthy heartbeat result.
         *
         * @param message the error message
         * @return the result
         */
        public static HeartbeatResult unhealthy(String message) {
            return new HeartbeatResult(false, message, false);
        }

        /**
         * Creates a result indicating reconnection is needed.
         *
         * @param message the message
         * @return the result
         */
        public static HeartbeatResult reconnectNeeded(String message) {
            return new HeartbeatResult(false, message, true);
        }
    }
}
