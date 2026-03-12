package openclaw.agent;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Default ACP protocol implementation.
 *
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public class DefaultAcpProtocol implements AcpProtocol {

    private final Map<String, AgentSession> sessions = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private AcpConfig config;
    private boolean initialized = false;

    @Override
    public CompletableFuture<Void> initialize(AcpConfig config) {
        return CompletableFuture.runAsync(() -> {
            this.config = config;
            this.initialized = true;
        });
    }

    @Override
    public CompletableFuture<SpawnResult> spawnAgent(SpawnRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            ensureInitialized();

            // Check concurrent limit
            long runningCount = sessions.values().stream()
                    .filter(s -> s.status == AgentStatus.RUNNING)
                    .count();
            if (runningCount >= config.maxConcurrentAgents()) {
                return SpawnResult.failure("Max concurrent agents reached");
            }

            String sessionKey = request.sessionKey() != null ?
                    request.sessionKey() : generateSessionKey();
            String agentId = generateAgentId();

            AgentSession session = new AgentSession(
                    sessionKey,
                    agentId,
                    AgentStatus.PENDING,
                    System.currentTimeMillis(),
                    request
            );
            sessions.put(sessionKey, session);

            // Start agent execution
            executor.submit(() -> runAgent(session));

            return SpawnResult.success(sessionKey, agentId);
        });
    }

    @Override
    public CompletableFuture<WaitResult> waitForAgent(String sessionKey, long timeoutMs) {
        return CompletableFuture.supplyAsync(() -> {
            AgentSession session = sessions.get(sessionKey);
            if (session == null) {
                return WaitResult.error("Session not found: " + sessionKey);
            }

            long deadline = System.currentTimeMillis() + timeoutMs;
            while (System.currentTimeMillis() < deadline) {
                if (session.status == AgentStatus.COMPLETED) {
                    return WaitResult.ok(session.result);
                }
                if (session.status == AgentStatus.FAILED) {
                    return WaitResult.error(session.error);
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    return WaitResult.error("Wait interrupted");
                }
            }
            return WaitResult.timeout();
        });
    }

    @Override
    public CompletableFuture<AgentMessages> getMessages(String sessionKey, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            AgentSession session = sessions.get(sessionKey);
            if (session == null) {
                return new AgentMessages(java.util.List.of(), false);
            }

            java.util.List<AgentMessage> messages = session.messages.stream()
                    .limit(limit)
                    .toList();
            boolean hasMore = session.messages.size() > limit;

            return new AgentMessages(messages, hasMore);
        });
    }

    @Override
    public CompletableFuture<Void> deleteSession(String sessionKey) {
        return CompletableFuture.runAsync(() -> {
            AgentSession session = sessions.remove(sessionKey);
            if (session != null && session.status == AgentStatus.RUNNING) {
                session.status = AgentStatus.CANCELLED;
            }
        });
    }

    @Override
    public CompletableFuture<Void> sendMessage(String sessionKey, AgentMessage message) {
        return CompletableFuture.runAsync(() -> {
            AgentSession session = sessions.get(sessionKey);
            if (session != null) {
                session.messages.add(message);
            }
        });
    }

    private void runAgent(AgentSession session) {
        try {
            session.status = AgentStatus.RUNNING;

            // Add system message
            session.request.systemPrompt().ifPresent(prompt ->
                    session.messages.add(AgentMessage.system(prompt))
            );

            // Add user message
            session.messages.add(AgentMessage.user(session.request.userMessage()));

            // Simulate agent processing (in production, call LLM API)
            Thread.sleep(1000);

            // Add assistant response
            String response = "Agent response for: " + session.request.userMessage();
            session.messages.add(AgentMessage.assistant(response));
            session.result = response;
            session.status = AgentStatus.COMPLETED;

        } catch (Exception e) {
            session.status = AgentStatus.FAILED;
            session.error = e.getMessage();
        }
    }

    private void ensureInitialized() {
        if (!initialized) {
            throw new IllegalStateException("ACP not initialized");
        }
    }

    private String generateSessionKey() {
        return "session-" + java.util.UUID.randomUUID().toString().substring(0, 8);
    }

    private String generateAgentId() {
        return "agent-" + java.util.UUID.randomUUID().toString().substring(0, 8);
    }

    private enum AgentStatus {
        PENDING, RUNNING, COMPLETED, FAILED, CANCELLED
    }

    private static class AgentSession {
        final String sessionKey;
        final String agentId;
        volatile AgentStatus status;
        final long startTime;
        final SpawnRequest request;
        final java.util.List<AgentMessage> messages = new java.util.ArrayList<>();
        volatile String result;
        volatile String error;

        AgentSession(String sessionKey, String agentId, AgentStatus status,
                     long startTime, SpawnRequest request) {
            this.sessionKey = sessionKey;
            this.agentId = agentId;
            this.status = status;
            this.startTime = startTime;
            this.request = request;
        }
    }
}
