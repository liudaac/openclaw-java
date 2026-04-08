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
     * @param lightContext whether to use light context mode (omit full conversation history)
     */
    record SpawnOptions(
            String model,
            String systemPrompt,
            long timeoutMs,
            Map<String, Object> tools,
            boolean lightContext
    ) {
        public static SpawnOptions defaults() {
            return new SpawnOptions("gpt-4", null, 60000, Map.of(), false);
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private String model = "gpt-4";
            private String systemPrompt;
            private long timeoutMs = 60000;
            private Map<String, Object> tools = Map.of();
            private boolean lightContext = false;

            public Builder model(String model) {
                this.model = model;
                return this;
            }

            public Builder systemPrompt(String systemPrompt) {
                this.systemPrompt = systemPrompt;
                return this;
            }

            public Builder timeoutMs(long timeoutMs) {
                this.timeoutMs = timeoutMs;
                return this;
            }

            public Builder tools(Map<String, Object> tools) {
                this.tools = tools != null ? tools : Map.of();
                return this;
            }

            public Builder lightContext(boolean lightContext) {
                this.lightContext = lightContext;
                return this;
            }

            public SpawnOptions build() {
                return new SpawnOptions(model, systemPrompt, timeoutMs, tools, lightContext);
            }
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
