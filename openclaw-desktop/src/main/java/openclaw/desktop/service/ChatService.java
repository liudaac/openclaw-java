package openclaw.desktop.service;

import javafx.application.Platform;
import openclaw.agent.AcpProtocol;
import openclaw.agent.AcpProtocol.*;
import openclaw.desktop.model.UIMessage;
import openclaw.session.service.SessionPersistenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Chat Service for Desktop Application.
 *
 * <p>Provides high-level chat operations with JavaFX-friendly callbacks.
 * Directly integrates with OpenClaw ACP protocol without REST API.</p>
 */
@Service
public class ChatService {

    private static final Logger logger = LoggerFactory.getLogger(ChatService.class);

    @Autowired
    private AcpProtocol acpProtocol;

    @Autowired
    private SessionPersistenceService sessionService;

    /** Active conversation sessions */
    private final Map<String, ConversationSession> activeSessions = new ConcurrentHashMap<>();

    /**
     * Create a new conversation session.
     */
    public CompletableFuture<String> createConversation(String title, String model) {
        String sessionKey = "conv-" + UUID.randomUUID().toString().substring(0, 8);

        return CompletableFuture.supplyAsync(() -> {
            // Create session in persistence layer
            sessionService.createSession(sessionKey, model != null ? model : "gpt-4").join();

            // Track active session
            ConversationSession session = new ConversationSession(sessionKey, title, model);
            activeSessions.put(sessionKey, session);

            logger.info("Created conversation: {} ({})", title, sessionKey);
            return sessionKey;
        });
    }

    /**
     * Send a message and receive streaming response.
     */
    public void sendMessage(String sessionKey, String message,
                           Consumer<UIMessage> onMessageUpdate,
                           Consumer<String> onComplete,
                           Consumer<Throwable> onError) {

        ConversationSession session = activeSessions.get(sessionKey);
        if (session == null) {
            onError.accept(new IllegalStateException("Session not found: " + sessionKey));
            return;
        }

        // Create user message
        UIMessage userMsg = UIMessage.builder()
            .role("user")
            .content(message)
            .status(UIMessage.MessageStatus.COMPLETED)
            .build();

        Platform.runLater(() -> onMessageUpdate.accept(userMsg));

        // Create assistant message (streaming)
        UIMessage assistantMsg = UIMessage.builder()
            .role("assistant")
            .content("")
            .status(UIMessage.MessageStatus.STREAMING)
            .streaming(true)
            .model(session.getModel())
            .build();

        Platform.runLater(() -> onMessageUpdate.accept(assistantMsg));

        // Build spawn request
        SpawnRequest request = SpawnRequest.builder()
            .sessionKey(sessionKey)
            .userMessage(message)
            .model(session.getModel())
            .build();

        // Execute
        acpProtocol.spawnAgent(request)
            .thenAccept(result -> {
                if (result.success()) {
                    // Poll for messages
                    pollMessages(sessionKey, assistantMsg, onMessageUpdate, onComplete, onError);
                } else {
                    assistantMsg.setStatus(UIMessage.MessageStatus.FAILED);
                    assistantMsg.setError(result.error().orElse("Unknown error"));
                    Platform.runLater(() -> onMessageUpdate.accept(assistantMsg));
                    onError.accept(new RuntimeException(result.error().orElse("Spawn failed")));
                }
            })
            .exceptionally(ex -> {
                logger.error("Failed to send message", ex);
                assistantMsg.setStatus(UIMessage.MessageStatus.FAILED);
                assistantMsg.setError(ex.getMessage());
                Platform.runLater(() -> onMessageUpdate.accept(assistantMsg));
                onError.accept(ex);
                return null;
            });
    }

    /**
     * Poll messages for streaming effect.
     */
    private void pollMessages(String sessionKey, UIMessage assistantMsg,
                             Consumer<UIMessage> onMessageUpdate,
                             Consumer<String> onComplete,
                             Consumer<Throwable> onError) {

        new Thread(() -> {
            try {
                StringBuilder fullContent = new StringBuilder();
                boolean completed = false;
                int lastMessageCount = 0;

                while (!completed && assistantMsg.getStatus() == UIMessage.MessageStatus.STREAMING) {
                    AgentMessages messages = acpProtocol.getMessages(sessionKey, 100).join();

                    // Find assistant messages
                    for (AgentMessage msg : messages.messages()) {
                        if ("assistant".equals(msg.role())) {
                            String content = msg.content();
                            if (content.length() > fullContent.length()) {
                                // New content received
                                String newChunk = content.substring(fullContent.length());
                                fullContent.append(newChunk);

                                Platform.runLater(() -> {
                                    assistantMsg.setContent(fullContent.toString());
                                    onMessageUpdate.accept(assistantMsg);
                                });
                            }
                        }
                    }

                    // Check if agent completed
                    WaitResult result = acpProtocol.waitForAgent(sessionKey, 100).join();
                    if (result.status() == WaitResult.WaitStatus.OK) {
                        completed = true;
                    }

                    Thread.sleep(50); // Small delay for animation
                }

                // Mark as completed
                Platform.runLater(() -> {
                    assistantMsg.setStreaming(false);
                    assistantMsg.setStatus(UIMessage.MessageStatus.COMPLETED);
                    onMessageUpdate.accept(assistantMsg);
                    onComplete.accept(assistantMsg.getContent());
                });

            } catch (Exception e) {
                logger.error("Error polling messages", e);
                Platform.runLater(() -> {
                    assistantMsg.setStatus(UIMessage.MessageStatus.FAILED);
                    assistantMsg.setError(e.getMessage());
                    onMessageUpdate.accept(assistantMsg);
                });
                onError.accept(e);
            }
        }).start();
    }

    /**
     * Abort ongoing message generation.
     */
    public CompletableFuture<Void> abortGeneration(String sessionKey) {
        return CompletableFuture.runAsync(() -> {
            logger.info("Aborting generation for session: {}", sessionKey);
            // Implementation depends on ACP protocol abort capability
        });
    }

    /**
     * Get conversation history.
     */
    public CompletableFuture<java.util.List<UIMessage>> getHistory(String sessionKey) {
        return CompletableFuture.supplyAsync(() -> {
            AgentMessages messages = acpProtocol.getMessages(sessionKey, 1000).join();

            return messages.messages().stream()
                .map(msg -> UIMessage.builder()
                    .role(msg.role())
                    .content(msg.content())
                    .timestamp(Instant.ofEpochMilli(msg.timestamp()))
                    .status(UIMessage.MessageStatus.COMPLETED)
                    .build())
                .toList();
        });
    }

    /**
     * Delete a conversation.
     */
    public CompletableFuture<Void> deleteConversation(String sessionKey) {
        return CompletableFuture.runAsync(() -> {
            acpProtocol.deleteSession(sessionKey).join();
            sessionService.deleteSession(sessionKey).join();
            activeSessions.remove(sessionKey);
            logger.info("Deleted conversation: {}", sessionKey);
        });
    }

    /**
     * Rename a conversation.
     */
    public void renameConversation(String sessionKey, String newTitle) {
        ConversationSession session = activeSessions.get(sessionKey);
        if (session != null) {
            session.setTitle(newTitle);
        }
    }

    /**
     * Get active conversations.
     */
    public Map<String, ConversationSession> getActiveSessions() {
        return new ConcurrentHashMap<>(activeSessions);
    }

    /**
     * Search conversations.
     */
    public CompletableFuture<java.util.List<ConversationSession>> searchConversations(String keyword) {
        return CompletableFuture.supplyAsync(() -> {
            return activeSessions.values().stream()
                .filter(s -> s.getTitle().toLowerCase().contains(keyword.toLowerCase()))
                .toList();
        });
    }

    /**
     * Export conversation to Markdown.
     */
    public CompletableFuture<String> exportToMarkdown(String sessionKey) {
        return getHistory(sessionKey).thenApply(messages -> {
            StringBuilder md = new StringBuilder();
            md.append("# Conversation\n\n");

            for (UIMessage msg : messages) {
                String role = msg.isUser() ? "**User**" : "**Assistant**";
                md.append(role).append(": ").append(msg.getContent()).append("\n\n");
            }

            return md.toString();
        });
    }

    /**
     * Inner class for conversation session tracking.
     */
    public static class ConversationSession {
        private final String sessionKey;
        private String title;
        private final String model;
        private final Instant createdAt;
        private Instant lastActivity;
        private int messageCount;

        public ConversationSession(String sessionKey, String title, String model) {
            this.sessionKey = sessionKey;
            this.title = title;
            this.model = model != null ? model : "gpt-4";
            this.createdAt = Instant.now();
            this.lastActivity = Instant.now();
            this.messageCount = 0;
        }

        // Getters and setters
        public String getSessionKey() { return sessionKey; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getModel() { return model; }
        public Instant getCreatedAt() { return createdAt; }
        public Instant getLastActivity() { return lastActivity; }
        public void updateActivity() { this.lastActivity = Instant.now(); }
        public int getMessageCount() { return messageCount; }
        public void incrementMessageCount() { this.messageCount++; }
    }
}
