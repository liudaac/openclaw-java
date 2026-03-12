package openclaw.sdk.core;

import java.util.Map;
import java.util.Optional;

/**
 * Health status of the plugin runtime.
 *
 * @param status the overall health status
 * @param checks individual health checks
 * @param message optional status message
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public record HealthStatus(
        Status status,
        Map<String, Check> checks,
        Optional<String> message
) {

    /**
     * Creates a healthy status.
     *
     * @return the healthy status
     */
    public static HealthStatus healthy() {
        return new HealthStatus(Status.HEALTHY, Map.of(), Optional.empty());
    }

    /**
     * Creates a healthy status with message.
     *
     * @param message the message
     * @return the healthy status
     */
    public static HealthStatus healthy(String message) {
        return new HealthStatus(Status.HEALTHY, Map.of(), Optional.of(message));
    }

    /**
     * Creates an unhealthy status.
     *
     * @param message the error message
     * @return the unhealthy status
     */
    public static HealthStatus unhealthy(String message) {
        return new HealthStatus(Status.UNHEALTHY, Map.of(), Optional.of(message));
    }

    /**
     * Creates a degraded status.
     *
     * @param message the status message
     * @return the degraded status
     */
    public static HealthStatus degraded(String message) {
        return new HealthStatus(Status.DEGRADED, Map.of(), Optional.of(message));
    }

    /**
     * Overall health status.
     */
    public enum Status {
        HEALTHY,
        DEGRADED,
        UNHEALTHY
    }

    /**
     * Individual health check.
     *
     * @param name the check name
     * @param status the check status
     * @param message optional message
     */
    public record Check(String name, Status status, Optional<String> message) {
    }
}
