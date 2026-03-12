package openclaw.sdk.core;

import java.util.Optional;

/**
 * Parameters for waiting on a subagent run.
 *
 * @param runId the run ID to wait for
 * @param timeoutMs optional timeout in milliseconds
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public record SubagentWaitParams(
        String runId,
        Optional<Long> timeoutMs
) {

    /**
     * Creates wait params with just a run ID.
     *
     * @param runId the run ID
     * @return the wait params
     */
    public static SubagentWaitParams of(String runId) {
        return new SubagentWaitParams(runId, Optional.empty());
    }

    /**
     * Creates wait params with run ID and timeout.
     *
     * @param runId the run ID
     * @param timeoutMs the timeout in milliseconds
     * @return the wait params
     */
    public static SubagentWaitParams withTimeout(String runId, long timeoutMs) {
        return new SubagentWaitParams(runId, Optional.of(timeoutMs));
    }
}
