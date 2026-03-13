package openclaw.session.model;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Represents a message in a session.
 *
 * @author OpenClaw Team
 * @version 2026.3.13
 */
public class Message {
    
    private String id;
    private String sessionId;
    private String role;  // system, user, assistant, tool
    private String content;
    private String toolName;
    private String toolCallId;
    private Map<String, Object> toolResult;
    private Map<String, Object> metadata;
    private Instant createdAt;
    private int tokenCount;
    
    public Message() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = Instant.now();
    }
    
    public Message(String role, String content) {
        this();
        this.role = role;
        this.content = content;
    }
    
    // Getters and Setters
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    public String getRole() {
        return role;
    }
    
    public void setRole(String role) {
        this.role = role;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public String getToolName() {
        return toolName;
    }
    
    public void setToolName(String toolName) {
        this.toolName = toolName;
    }
    
    public String getToolCallId() {
        return toolCallId;
    }
    
    public void setToolCallId(String toolCallId) {
        this.toolCallId = toolCallId;
    }
    
    public Map<String, Object> getToolResult() {
        return toolResult;
    }
    
    public void setToolResult(Map<String, Object> toolResult) {
        this.toolResult = toolResult;
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
    
    public int getTokenCount() {
        return tokenCount;
    }
    
    public void setTokenCount(int tokenCount) {
        this.tokenCount = tokenCount;
    }
    
    /**
     * Check if this is a tool message.
     */
    public boolean isToolMessage() {
        return "tool".equals(role);
    }
    
    /**
     * Check if this is a user message.
     */
    public boolean isUserMessage() {
        return "user".equals(role);
    }
    
    /**
     * Check if this is an assistant message.
     */
    public boolean isAssistantMessage() {
        return "assistant".equals(role);
    }
    
    @Override
    public String toString() {
        return String.format("Message{role='%s', content='%s...'}",
            role, content != null ? content.substring(0, Math.min(50, content.length())) : "");
    }
}
