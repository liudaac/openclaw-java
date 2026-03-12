package openclaw.sdk.core;

import java.util.concurrent.CompletableFuture;

/**
 * Subagent runtime interface for spawning and managing child sessions.
 *
 * <p>Provides asynchronous operations for subagent lifecycle management.</p>
 *
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public interface SubagentRuntime {

    /**
     * Spawns a new subagent session.
     *
     * @param params the run parameters
     * @return a future containing the run result with session ID
     */
    CompletableFuture<SubagentRunResult> run(SubagentRunParams params);

    /**
     * Waits for a subagent run to complete.
     *
     * @param params the wait parameters
     * @return a future containing the wait result
     */
    CompletableFuture<SubagentWaitResult> waitForRun(SubagentWaitParams params);

    /**
     * Gets messages from a subagent session.
     *
     * @param params the get session messages parameters
     * @return a future containing the session messages
     */
    CompletableFuture<SubagentGetSessionMessagesResult> getSessionMessages(SubagentGetSessionMessagesParams params);

    /**
     * Deletes a subagent session.
     *
     * @param params the delete session parameters
     * @return a future that completes when the session is deleted
     */
    CompletableFuture<Void> deleteSession(SubagentDeleteSessionParams params);
}
