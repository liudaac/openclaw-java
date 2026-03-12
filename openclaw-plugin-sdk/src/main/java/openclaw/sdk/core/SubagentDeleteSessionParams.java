package openclaw.sdk.core;

import java.util.Optional;

/**
 * Parameters for deleting a subagent session.
 *
 * @param sessionKey the session key to delete
 * @param deleteTranscript whether to delete the transcript as well
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public record SubagentDeleteSessionParams(
        String sessionKey,
        Optional<Boolean> deleteTranscript
) {

    /**
     * Creates params for deleting a session.
     *
     * @param sessionKey the session key
     * @return the params
     */
    public static SubagentDeleteSessionParams of(String sessionKey) {
        return new SubagentDeleteSessionParams(sessionKey, Optional.empty());
    }

    /**
     * Creates params with transcript deletion option.
     *
     * @param sessionKey the session key
     * @param deleteTranscript whether to delete transcript
     * @return the params
     */
    public static SubagentDeleteSessionParams withTranscriptDeletion(
            String sessionKey, boolean deleteTranscript) {
        return new SubagentDeleteSessionParams(sessionKey, Optional.of(deleteTranscript));
    }
}
