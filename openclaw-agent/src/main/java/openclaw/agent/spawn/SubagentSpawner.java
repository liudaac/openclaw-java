package openclaw.agent.spawn;

import openclaw.agent.AcpProtocol;
import openclaw.agent.AcpProtocol.SpawnRequest;
import openclaw.agent.AcpProtocol.SpawnResult;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Subagent spawner for managing child agents.
 *
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public interface SubagentSpawner {

    /**
     * Spawns a subagent.
     *
     * @param parentSession the parent session key
     * @param task the task description
     * @return the spawn result
     */
    CompletableFuture<SpawnResult> spawn(String parentSession, String task);

    /**
     * Spawns a subagent with options.
     *
     * @param parentSession the parent session key
     * @param task the task
     * @param options the spawn options
     * @return the spawn result
     */
    CompletableFuture<SpawnResult> spawnWithOptions(
            String parentSession,
            String task,
            SpawnOptions options
    );

    /**
     * Lists active subagents.
     *
     * @param parentSession the parent session
     * @return list of subagent info
     */
    CompletableFuture<java.util.List<SubagentInfo>> listSubagents(String parentSession);

    /**
     * Kills a subagent.
     *
     * @param subagentId the subagent ID
     * @return completion future
     */
    CompletableFuture<Void> kill(String subagentId);

    /**
     * Gets subagent status.
     *
     * @param subagentId the subagent ID
     * @return the status
     */
    CompletableFuture<SubagentStatus> getStatus(String subagentId);

    /**
     * Spawn options.
     *
     * @param model the model to use
     * @param systemPrompt the system prompt
     * @param timeoutMs the timeout
     * @param tools the tools available
     */
    record SpawnOptions(
            String model,
            String systemPrompt,
            long timeoutMs,
            Map<String, Object> tools
    ) {
        public static SpawnOptions defaults() {
            return new SpawnOptions("gpt-4", null, 60000, Map.of());
        }
    }

    /**
     * Subagent information.
     *
     * @param id the subagent ID
     * @param sessionKey the session key
     * @param parentSession the parent session
     * @param status the status
     * @param startTime the start time
     */
    record SubagentInfo(
            String id,
            String sessionKey,
            String parentSession,
            SubagentStatus status,
            long startTime
    ) {
    }

    /**
     * Subagent status.
     */
    enum SubagentStatus {
        PENDING,
        RUNNING,
        COMPLETED,
        FAILED,
        KILLED
    }
}
