package openclaw.agent;

import java.util.ArrayList;
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
                    .filter(s -> s.status() == AgentSession.AgentStatus.RUNNING)
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
                    AgentSession.AgentStatus.PENDING,
                    System.currentTimeMillis(),
                    request,
                    new ArrayList<>(),
                    null,
                    null
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
                if (session.status() == AgentSession.AgentStatus.COMPLETED) {
                    return WaitResult.ok(session.result());
                }
                if (session.status() == AgentSession.AgentStatus.FAILED) {
                    return WaitResult.error(session.error());
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

            java.util.List<AgentMessage> messages = session.messages().stream()
                    .limit(limit)
                    .toList();
            boolean hasMore = session.messages().size() > limit;

            return new AgentMessages(messages, hasMore);
        });
    }

    @Override
    public CompletableFuture<Void> deleteSession(String sessionKey) {
        return CompletableFuture.runAsync(() -> {
            AgentSession session = sessions.remove(sessionKey);
            if (session != null && session.status() == AgentSession.AgentStatus.RUNNING) {
                sessions.put(sessionKey, new AgentSession(
                        session.sessionKey(),
                        session.agentId(),
                        AgentSession.AgentStatus.CANCELLED,
                        session.startTime(),
                        session.request(),
                        session.messages(),
                        session.result(),
                        session.error()
                ));
            }
        });
    }

    @Override
    public CompletableFuture<Void> sendMessage(String sessionKey, AgentMessage message) {
        return CompletableFuture.runAsync(() -> {
            AgentSession session = sessions.get(sessionKey);
            if (session != null) {
                java.util.List<AgentMessage> newMessages = new ArrayList<>(session.messages());
                newMessages.add(message);
                sessions.put(sessionKey, new AgentSession(
                        session.sessionKey(),
                        session.agentId(),
                        session.status(),
                        session.startTime(),
                        session.request(),
                        newMessages,
                        session.result(),
                        session.error()
                ));
            }
        });
    }

    private void runAgent(AgentSession session) {
        try {
            java.util.List<AgentMessage> messages = new ArrayList<>(session.messages());
            AgentSession.AgentStatus status = AgentSession.AgentStatus.RUNNING;

            // Add system message
            session.request().systemPrompt().ifPresent(prompt ->
                    messages.add(AgentMessage.system(prompt))
            );

            // Add user message
            messages.add(AgentMessage.user(session.request().userMessage()));

            // Simulate agent processing (in production, call LLM API)
            Thread.sleep(1000);

            // Add assistant response
            String response = "Agent response for: " + session.request().userMessage();
            messages.add(AgentMessage.assistant(response));

            sessions.put(session.sessionKey(), new AgentSession(
                    session.sessionKey(),
                    session.agentId(),
                    AgentSession.AgentStatus.COMPLETED,
                    session.startTime(),
                    session.request(),
                    messages,
                    response,
                    null
            ));

        } catch (Exception e) {
            sessions.put(session.sessionKey(), new AgentSession(
                    session.sessionKey(),
                    session.agentId(),
                    AgentSession.AgentStatus.FAILED,
                    session.startTime(),
                    session.request(),
                    session.messages(),
                    null,
                    e.getMessage()
            ));
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
}
