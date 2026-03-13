package openclaw.session.model;

import java.util.Map;
import java.util.Set;

/**
 * State machine for session status transitions.
 *
 * @author OpenClaw Team
 * @version 2026.3.13
 */
public class SessionStatusMachine {
    
    private static final Map<SessionStatus, Set<SessionStatus>> VALID_TRANSITIONS = Map.of(
        SessionStatus.PENDING, Set.of(
            SessionStatus.ACTIVE,
            SessionStatus.ERROR
        ),
        SessionStatus.ACTIVE, Set.of(
            SessionStatus.PAUSED,
            SessionStatus.COMPLETED,
            SessionStatus.ERROR
        ),
        SessionStatus.PAUSED, Set.of(
            SessionStatus.ACTIVE,
            SessionStatus.COMPLETED,
            SessionStatus.ERROR
        ),
        SessionStatus.COMPLETED, Set.of(
            SessionStatus.ARCHIVED
        ),
        SessionStatus.ERROR, Set.of(
            SessionStatus.ARCHIVED
        ),
        SessionStatus.ARCHIVED, Set.of()  // Terminal state
    );
    
    /**
     * Check if transition is valid.
     */
    public static boolean isValidTransition(SessionStatus from, SessionStatus to) {
        if (from == null || to == null) return false;
        if (from == to) return true;
        Set<SessionStatus> validTargets = VALID_TRANSITIONS.get(from);
        return validTargets != null && validTargets.contains(to);
    }
    
    /**
     * Get valid transitions from a state.
     */
    public static Set<SessionStatus> getValidTransitions(SessionStatus from) {
        return VALID_TRANSITIONS.getOrDefault(from, Set.of());
    }
    
    /**
     * Validate and throw if invalid.
     */
    public static void validateTransition(String sessionId, SessionStatus from, SessionStatus to) {
        if (!isValidTransition(from, to)) {
            throw new IllegalStateException(
                String.format("Invalid transition for session %s: %s -> %s. Valid: %s",
                    sessionId, from, to, getValidTransitions(from))
            );
        }
    }
}
