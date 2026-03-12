package openclaw.agent;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * ACP (Agent Communication Protocol) interface.
 *
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public interface AcpProtocol {

    /**
     * Initializes the ACP protocol.
     *
     * @param config the configuration
     * @return completion future
     */
    CompletableFuture<Void> initialize(AcpConfig config);

    /**
     * Spawns a new agent session.
     *
     * @param request the spawn request
     * @return the spawn result
     */
    CompletableFuture<SpawnResult> spawnAgent(SpawnRequest request);

    /**
     * Waits for an agent to complete.
     *
     * @param sessionKey the session key
     * @param timeoutMs the timeout in milliseconds
     * @return the wait result
     */
    CompletableFuture<WaitResult> waitForAgent(String sessionKey, long timeoutMs);

    /**
     * Gets agent messages.
     *
     * @param sessionKey the session key
     * @param limit the message limit
     * @return the messages
     */
    CompletableFuture<AgentMessages> getMessages(String sessionKey, int limit);

    /**
     * Deletes an agent session.
     *
     * @param sessionKey the session key
     * @return completion future
     */
    CompletableFuture<Void> deleteSession(String sessionKey);

    /**
     * Sends a message to an agent.
     *
     * @param sessionKey the session key
     * @param message the message
     * @return completion future
     */
    CompletableFuture<Void> sendMessage(String sessionKey, AgentMessage message);

    /**
     * ACP configuration.
     *
     * @param maxConcurrentAgents the maximum concurrent agents
     * @param defaultTimeoutMs the default timeout
     * @param enableStreaming whether to enable streaming
     * @param modelProvider the model provider
     */
    record AcpConfig(
            int maxConcurrentAgents,
            long defaultTimeoutMs,
            boolean enableStreaming,
            String modelProvider
    ) {
        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private int maxConcurrentAgents = 10;
            private long defaultTimeoutMs = 60000;
            private boolean enableStreaming = true;
            private String modelProvider = "openai";

            public Builder maxConcurrentAgents(int max) {
                this.maxConcurrentAgents = max;
                return this;
            }
            public Builder defaultTimeoutMs(long timeoutMs) {
                this.defaultTimeoutMs = timeoutMs;
                return this;
            }
            public Builder enableStreaming(boolean enable) {
                this.enableStreaming = enable;
                return this;
            }
            public Builder modelProvider(String provider) {
                this.modelProvider = provider;
                return this;
            }
            public AcpConfig build() {
                return new AcpConfig(maxConcurrentAgents, defaultTimeoutMs, enableStreaming, modelProvider);
            }
        }
    }

    /**
     * Spawn request.
     *
     * @param sessionKey the session key
     * @param systemPrompt the system prompt
     * @param userMessage the user message
     * @param model the model to use
     * @param tools the tools available
     * @param metadata additional metadata
     */
    record SpawnRequest(
            String sessionKey,
            Optional<String> systemPrompt,
            String userMessage,
            String model,
            Map<String, Object> tools,
            Map<String, Object> metadata
    ) {
        public static Builder builder() {
            return new Builder();
        }
        public static class Builder {
            private String sessionKey;
            private String systemPrompt;
            private String userMessage;
            private String model = "gpt-4";
            private Map<String, Object> tools = Map.of();
            private Map<String, Object> metadata = Map.of();
            public Builder sessionKey(String sessionKey) {
                this.sessionKey = sessionKey;
                return this;
            }
            public Builder systemPrompt(String systemPrompt) {
                this.systemPrompt = systemPrompt;
                return this;
            }
            public Builder userMessage(String userMessage) {
                this.userMessage = userMessage;
                return this;
            }
            public Builder model(String model) {
                this.model = model;
                return this;
            }
            public Builder tools(Map<String, Object> tools) {
                this.tools = tools != null ? tools : Map.of();
                return this;
            }
            public Builder metadata(Map<String, Object> metadata) {
                this.metadata = metadata != null ? metadata : Map.of();
                return this;
            }
            public SpawnRequest build() {
                return new SpawnRequest(
                        sessionKey,
                        Optional.ofNullable(systemPrompt),
                        userMessage,
                        model,
                        tools,
                        metadata
                );
            }
        }
    }

    /**
     * Spawn result.
     *
     * @param success whether spawn succeeded
     * @param sessionKey the session key
     * @param agentId the agent ID
     * @param error error message if failed
     */
    record SpawnResult(
            boolean success,
            String sessionKey,
            String agentId,
            Optional<String> error
    ) {
        public static SpawnResult success(String sessionKey, String agentId) {
            return new SpawnResult(true, sessionKey, agentId, Optional.empty());
        }
        public static SpawnResult failure(String error) {
            return new SpawnResult(false, null, null, Optional.of(error));
        }
    }

    /**
     * Wait result.
     *
     * @param status the wait status
     * @param result the result if completed
     * @param error the error if failed
     */
    record WaitResult(
            WaitStatus status,
            Optional<String> result,
            Optional<String> error
    ) {
        public enum WaitStatus {
            OK,
            TIMEOUT,
            ERROR
        }
        public static WaitResult ok(String result) {
            return new WaitResult(WaitStatus.OK, Optional.of(result), Optional.empty());
        }
        public static WaitResult timeout() {
            return new WaitResult(WaitStatus.TIMEOUT, Optional.empty(), Optional.empty());
        }
        public static WaitResult error(String error) {
            return new WaitResult(WaitStatus.ERROR, Optional.empty(), Optional.of(error));
        }
    }

    /**
     * Agent messages.
     *
     * @param messages the messages
     * @param hasMore whether there are more messages
     */
    record AgentMessages(
            java.util.List<AgentMessage> messages,
            boolean hasMore
    ) {
    }

    /**
     * Agent message.
     *
     * @param role the role (user, assistant, system)
     * @param content the content
     * @param timestamp the timestamp
     * @param metadata additional metadata
     */
    record AgentMessage(
            String role,
            String content,
            long timestamp,
            Map<String, Object> metadata
    ) {
        public static AgentMessage user(String content) {
            return new AgentMessage("user", content, System.currentTimeMillis(), Map.of());
        }
        public static AgentMessage assistant(String content) {
            return new AgentMessage("assistant", content, System.currentTimeMillis(), Map.of());
        }
        public static AgentMessage system(String content) {
            return new AgentMessage("system", content, System.currentTimeMillis(), Map.of());
        }
    }
}
