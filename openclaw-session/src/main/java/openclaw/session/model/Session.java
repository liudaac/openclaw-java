package openclaw.session.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Represents an agent session with messages and metadata.
 *
 * @author OpenClaw Team
 * @version 2026.3.13
 */
public class Session {
    
    private String id;
    private String sessionKey;
    private String model;
    private SessionStatus status;
    private List<Message> messages;
    private Map<String, Object> metadata;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant lastActivityAt;
    private int totalInputTokens;
    private int totalOutputTokens;
    private String errorMessage;
    
    public Session() {
        this.id = UUID.randomUUID().toString();
        this.messages = new ArrayList<>();
        this.status = SessionStatus.PENDING;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        this.lastActivityAt = Instant.now();
        this.totalInputTokens = 0;
        this.totalOutputTokens = 0;
    }
    
    public Session(String sessionKey, String model) {
        this();
        this.sessionKey = sessionKey;
        this.model = model;
    }
    
    // Getters and Setters
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getSessionKey() {
        return sessionKey;
    }
    
    public void setSessionKey(String sessionKey) {
        this.sessionKey = sessionKey;
    }
    
    public String getModel() {
        return model;
    }
    
    public void setModel(String model) {
        this.model = model;
    }
    
    public SessionStatus getStatus() {
        return status;
    }
    
    public void setStatus(SessionStatus status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }
    
    public List<Message> getMessages() {
        return messages;
    }
    
    public void setMessages(List<Message> messages) {
        this.messages = messages;
        this.updatedAt = Instant.now();
    }
    
    public void addMessage(Message message) {
        this.messages.add(message);
        this.lastActivityAt = Instant.now();
        this.updatedAt = Instant.now();
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
        this.updatedAt = Instant.now();
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
    
    public Instant getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public Instant getLastActivityAt() {
        return lastActivityAt;
    }
    
    public void setLastActivityAt(Instant lastActivityAt) {
        this.lastActivityAt = lastActivityAt;
    }
    
    public int getTotalInputTokens() {
        return totalInputTokens;
    }
    
    public void setTotalInputTokens(int totalInputTokens) {
        this.totalInputTokens = totalInputTokens;
    }
    
    public void addInputTokens(int tokens) {
        this.totalInputTokens += tokens;
    }
    
    public int getTotalOutputTokens() {
        return totalOutputTokens;
    }
    
    public void setTotalOutputTokens(int totalOutputTokens) {
        this.totalOutputTokens = totalOutputTokens;
    }
    
    public void addOutputTokens(int tokens) {
        this.totalOutputTokens += tokens;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
        this.updatedAt = Instant.now();
    }
    
    /**
     * Check if session can transition to given status.
     */
    public boolean canTransitionTo(SessionStatus newStatus) {
        return SessionStatusMachine.isValidTransition(this.status, newStatus);
    }
    
    /**
     * Get total token count.
     */
    public int getTotalTokens() {
        return totalInputTokens + totalOutputTokens;
    }
    
    @Override
    public String toString() {
        return String.format("Session{id='%s', key='%s', status=%s, messages=%d}",
            id, sessionKey, status, messages.size());
    }
}
