package openclaw.cron.model;

import java.util.Map;
import java.util.Set;

/**
 * State machine for job status transitions.
 * 
 * <p>Validates and manages status transitions according to predefined rules.</p>
 *
 * @author OpenClaw Team
 * @version 2026.3.13
 */
public class JobStatusMachine {
    
    // Define valid transitions
    private static final Map<JobStatus, Set<JobStatus>> VALID_TRANSITIONS = Map.of(
        JobStatus.PENDING, Set.of(
            JobStatus.RUNNING,
            JobStatus.PAUSED,
            JobStatus.CANCELLED
        ),
        JobStatus.RUNNING, Set.of(
            JobStatus.COMPLETED,
            JobStatus.FAILED,
            JobStatus.PAUSED
        ),
        JobStatus.PAUSED, Set.of(
            JobStatus.RUNNING,
            JobStatus.CANCELLED
        ),
        JobStatus.FAILED, Set.of(
            JobStatus.RUNNING,
            JobStatus.CANCELLED
        ),
        JobStatus.COMPLETED, Set.of(),  // Terminal state
        JobStatus.CANCELLED, Set.of()   // Terminal state
    );
    
    /**
     * Check if a status transition is valid.
     *
     * @param from current status
     * @param to target status
     * @return true if transition is valid
     */
    public static boolean isValidTransition(JobStatus from, JobStatus to) {
        if (from == null || to == null) {
            return false;
        }
        if (from == to) {
            return true;  // Same state is always valid
        }
        Set<JobStatus> validTargets = VALID_TRANSITIONS.get(from);
        return validTargets != null && validTargets.contains(to);
    }
    
    /**
     * Get valid target statuses from a given status.
     *
     * @param from current status
     * @return set of valid target statuses
     */
    public static Set<JobStatus> getValidTransitions(JobStatus from) {
        return VALID_TRANSITIONS.getOrDefault(from, Set.of());
    }
    
    /**
     * Check if the status is a terminal state.
     *
     * @param status the status to check
     * @return true if terminal
     */
    public static boolean isTerminal(JobStatus status) {
        return status == JobStatus.COMPLETED || status == JobStatus.CANCELLED;
    }
    
    /**
     * Validate a transition and throw exception if invalid.
     *
     * @param jobId job ID for error message
     * @param from current status
     * @param to target status
     * @throws IllegalStateException if transition is invalid
     */
    public static void validateTransition(String jobId, JobStatus from, JobStatus to) {
        if (!isValidTransition(from, to)) {
            throw new IllegalStateException(
                String.format(
                    "Invalid status transition for job %s: %s -> %s. Valid transitions from %s: %s",
                    jobId,
                    from,
                    to,
                    from,
                    getValidTransitions(from)
                )
            );
        }
    }
}
