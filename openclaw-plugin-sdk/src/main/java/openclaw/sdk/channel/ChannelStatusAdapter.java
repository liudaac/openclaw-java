package openclaw.sdk.channel;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Channel status adapter for health checks and status reporting.
 *
 * @param <ResolvedAccount> the type of resolved account
 * @param <Probe> the type of probe result
 * @param <Audit> the type of audit info
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public interface ChannelStatusAdapter<ResolvedAccount, Probe, Audit> {

    /**
     * Probes the channel status.
     *
     * @param account the account
     * @return the probe result
     */
    CompletableFuture<Probe> probe(ResolvedAccount account);

    /**
     * Gets the channel status summary.
     *
     * @param account the account
     * @param probe the probe result
     * @return the status summary
     */
    CompletableFuture<StatusSummary> getStatusSummary(ResolvedAccount account, Probe probe);

    /**
     * Performs an audit of the channel.
     *
     * @param account the account
     * @return the audit info
     */
    CompletableFuture<Optional<Audit>> audit(ResolvedAccount account);

    /**
     * Status summary.
     *
     * @param status the status string
     * @param healthy whether the channel is healthy
     * @param message status message
     * @param details additional details
     */
    record StatusSummary(
            String status,
            boolean healthy,
            Optional<String> message,
            Map<String, Object> details
    ) {

        /**
         * Creates a healthy status summary.
         *
         * @param message the message
         * @return the summary
         */
        public static StatusSummary healthy(String message) {
            return new StatusSummary("healthy", true, Optional.of(message), Map.of());
        }

        /**
         * Creates an unhealthy status summary.
         *
         * @param message the error message
         * @return the summary
         */
        public static StatusSummary unhealthy(String message) {
            return new StatusSummary("unhealthy", false, Optional.of(message), Map.of());
        }

        /**
         * Creates a degraded status summary.
         *
         * @param message the message
         * @return the summary
         */
        public static StatusSummary degraded(String message) {
            return new StatusSummary("degraded", false, Optional.of(message), Map.of());
        }
    }
}
