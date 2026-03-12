package openclaw.server.service;

import openclaw.agent.AcpProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ACP Protocol Implementation
 */
@Service
public class AcpProtocolImpl implements AcpProtocol {

    private static final Logger logger = LoggerFactory.getLogger(AcpProtocolImpl.class);

    private final ChatClient chatClient;
    private final Map<String, AgentSession> sessions = new ConcurrentHashMap<>();

    public AcpProtocolImpl(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public CompletableFuture<Void> initialize(AcpConfig config) {
        logger.info("Initializing ACP Protocol with max {} agents", config.maxConcurrentAgents());
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<SpawnResult> spawnAgent(SpawnRequest request) {
        String sessionKey = request.sessionKey() != null ?
                request.sessionKey() : UUID.randomUUID().toString();

        logger.info("Spawning agent session: {}", sessionKey);

        AgentSession session = new AgentSession(sessionKey, request.model());
        sessions.put(sessionKey, session);

        // Add system message if present
        if (request.systemPrompt() != null && request.systemPrompt().isPresent()) {
            session.addMessage(AgentMessage.system(request.systemPrompt().get()));
        }

        // Add user message
        session.addMessage(AgentMessage.user(request.userMessage()));

        // Process with LLM
        return CompletableFuture.supplyAsync(() -> {
            try {
                String response = processWithLlm(session);
                session.addMessage(AgentMessage.assistant(response));
                return SpawnResult.success(sessionKey, sessionKey);
            } catch (Exception e) {
                logger.error("Agent spawn failed: {}", e.getMessage());
                return SpawnResult.failure(e.getMessage());
            }
        });
    }

    @Override
    public CompletableFuture<WaitResult> waitForAgent(String sessionKey, long timeoutMs) {
        AgentSession session = sessions.get(sessionKey);
        if (session == null) {
            return CompletableFuture.completedFuture(WaitResult.error("Session not found"));
        }

        // Simple implementation - return last assistant message
        return CompletableFuture.supplyAsync(() -> {
            List<AgentMessage> messages = session.getMessages();
            for (int i = messages.size() - 1; i >= 0; i--) {
                if (messages.get(i).role().equals("assistant")) {
                    return WaitResult.ok(messages.get(i).content());
                }
            }
            return WaitResult.ok("No response");
        });
    }

    @Override
    public CompletableFuture<AgentMessages> getMessages(String sessionKey, int limit) {
        AgentSession session = sessions.get(sessionKey);
        if (session == null) {
            return CompletableFuture.completedFuture(new AgentMessages(List.of(), false));
        }

        List<AgentMessage> messages = session.getMessages();
        int endIndex = Math.min(limit, messages.size());
        return CompletableFuture.completedFuture(
                new AgentMessages(messages.subList(0, endIndex), messages.size() > limit)
        );
    }

    @Override
    public CompletableFuture<Void> deleteSession(String sessionKey) {
        sessions.remove(sessionKey);
        logger.info("Deleted agent session: {}", sessionKey);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> sendMessage(String sessionKey, AgentMessage message) {
        AgentSession session = sessions.get(sessionKey);
        if (session == null) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Session not found"));
        }

        session.addMessage(message);

        // Process with LLM if user message
        if (message.role().equals("user")) {
            return CompletableFuture.runAsync(() -> {
                try {
                    String response = processWithLlm(session);
                    session.addMessage(AgentMessage.assistant(response));
                } catch (Exception e) {
                    logger.error("Failed to process message: {}", e.getMessage());
                }
            });
        }

        return CompletableFuture.completedFuture(null);
    }

    private String processWithLlm(AgentSession session) {
        StringBuilder prompt = new StringBuilder();
        for (AgentMessage msg : session.getMessages()) {
            prompt.append(msg.role()).append(": ").append(msg.content()).append("\n");
        }
        prompt.append("assistant: ");

        ChatResponse response = chatClient.call(new Prompt(prompt.toString()));
        return response.getResult().getOutput().getContent();
    }

    private static class AgentSession {
        private final String id;
        private final String model;
        private final List<AgentMessage> messages = new ArrayList<>();

        AgentSession(String id, String model) {
            this.id = id;
            this.model = model;
        }

        void addMessage(AgentMessage message) {
            messages.add(message);
        }

        List<AgentMessage> getMessages() {
            return new ArrayList<>(messages);
        }
    }
}
