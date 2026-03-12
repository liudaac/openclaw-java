package openclaw.sdk.core;

import java.util.Optional;

/**
 * Result of waiting for a subagent run.
 *
 * @param status the wait status (ok, error, or timeout)
 * @param error optional error message if status is error
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public record SubagentWaitResult(
        WaitStatus status,
        Optional<String> error
) {

    /**
     * Creates a successful wait result.
     *
     * @return the success result
     */
    public static SubagentWaitResult ok() {
        return new SubagentWaitResult(WaitStatus.OK, Optional.empty());
    }

    /**
     * Creates an error wait result.
     *
     * @param error the error message
     * @return the error result
     */
    public static SubagentWaitResult error(String error) {
        return new SubagentWaitResult(WaitStatus.ERROR, Optional.of(error));
    }

    /**
     * Creates a timeout wait result.
     *
     * @return the timeout result
     */
    public static SubagentWaitResult timeout() {
        return new SubagentWaitResult(WaitStatus.TIMEOUT, Optional.empty());
    }

    /**
     * Wait status enumeration.
     */
    public enum WaitStatus {
        OK,
        ERROR,
        TIMEOUT
    }
}
