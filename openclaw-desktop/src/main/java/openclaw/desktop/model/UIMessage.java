package openclaw.desktop.model;

import javafx.beans.property.*;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * UI Message Model for JavaFX binding.
 *
 * <p>Provides observable properties for real-time UI updates.</p>
 */
public class UIMessage {

    private final StringProperty id;
    private final StringProperty role;
    private final StringProperty content;
    private final ObjectProperty<Instant> timestamp;
    private final BooleanProperty streaming;
    private final StringProperty model;
    private final IntegerProperty tokenCount;
    private final ObjectProperty<MessageStatus> status;
    private final StringProperty error;

    public enum MessageStatus {
        PENDING,      // Waiting to be sent
        SENDING,      // Currently sending
        STREAMING,    // Receiving stream
        COMPLETED,    // Finished successfully
        FAILED,       // Failed with error
        ABORTED       // User aborted
    }

    public UIMessage() {
        this(UUID.randomUUID().toString(), "user", "", Instant.now());
    }

    public UIMessage(String id, String role, String content, Instant timestamp) {
        this.id = new SimpleStringProperty(id);
        this.role = new SimpleStringProperty(role);
        this.content = new SimpleStringProperty(content);
        this.timestamp = new SimpleObjectProperty<>(timestamp);
        this.streaming = new SimpleBooleanProperty(false);
        this.model = new SimpleStringProperty("");
        this.tokenCount = new SimpleIntegerProperty(0);
        this.status = new SimpleObjectProperty<>(MessageStatus.PENDING);
        this.error = new SimpleStringProperty("");
    }

    /**
     * Builder for fluent creation.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String id = UUID.randomUUID().toString();
        private String role = "user";
        private String content = "";
        private Instant timestamp = Instant.now();
        private boolean streaming = false;
        private String model = "";
        private int tokenCount = 0;
        private MessageStatus status = MessageStatus.PENDING;
        private String error = "";

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder role(String role) {
            this.role = role;
            return this;
        }

        public Builder content(String content) {
            this.content = content;
            return this;
        }

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder streaming(boolean streaming) {
            this.streaming = streaming;
            return this;
        }

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        public Builder tokenCount(int tokenCount) {
            this.tokenCount = tokenCount;
            return this;
        }

        public Builder status(MessageStatus status) {
            this.status = status;
            return this;
        }

        public Builder error(String error) {
            this.error = error;
            return this;
        }

        public UIMessage build() {
            UIMessage msg = new UIMessage(id, role, content, timestamp);
            msg.setStreaming(streaming);
            msg.setModel(model);
            msg.setTokenCount(tokenCount);
            msg.setStatus(status);
            msg.setError(error);
            return msg;
        }
    }

    // Property getters for JavaFX binding

    public StringProperty idProperty() {
        return id;
    }

    public StringProperty roleProperty() {
        return role;
    }

    public StringProperty contentProperty() {
        return content;
    }

    public ObjectProperty<Instant> timestampProperty() {
        return timestamp;
    }

    public BooleanProperty streamingProperty() {
        return streaming;
    }

    public StringProperty modelProperty() {
        return model;
    }

    public IntegerProperty tokenCountProperty() {
        return tokenCount;
    }

    public ObjectProperty<MessageStatus> statusProperty() {
        return status;
    }

    public StringProperty errorProperty() {
        return error;
    }

    // Regular getters

    public String getId() {
        return id.get();
    }

    public String getRole() {
        return role.get();
    }

    public String getContent() {
        return content.get();
    }

    public Instant getTimestamp() {
        return timestamp.get();
    }

    public boolean isStreaming() {
        return streaming.get();
    }

    public String getModel() {
        return model.get();
    }

    public int getTokenCount() {
        return tokenCount.get();
    }

    public MessageStatus getStatus() {
        return status.get();
    }

    public String getError() {
        return error.get();
    }

    // Setters

    public void setId(String id) {
        this.id.set(id);
    }

    public void setRole(String role) {
        this.role.set(role);
    }

    public void setContent(String content) {
        this.content.set(content);
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp.set(timestamp);
    }

    public void setStreaming(boolean streaming) {
        this.streaming.set(streaming);
    }

    public void setModel(String model) {
        this.model.set(model);
    }

    public void setTokenCount(int tokenCount) {
        this.tokenCount.set(tokenCount);
    }

    public void setStatus(MessageStatus status) {
        this.status.set(status);
    }

    public void setError(String error) {
        this.error.set(error);
    }

    /**
     * Append content for streaming.
     */
    public void appendContent(String chunk) {
        content.set(content.get() + chunk);
    }

    /**
     * Get formatted timestamp.
     */
    public String getFormattedTimestamp() {
        return DateTimeFormatter.ofPattern("HH:mm")
            .withZone(ZoneId.systemDefault())
            .format(timestamp.get());
    }

    /**
     * Check if this is a user message.
     */
    public boolean isUser() {
        return "user".equals(role.get());
    }

    /**
     * Check if this is an assistant message.
     */
    public boolean isAssistant() {
        return "assistant".equals(role.get());
    }

    /**
     * Check if this is a system message.
     */
    public boolean isSystem() {
        return "system".equals(role.get());
    }

    /**
     * Check if this is a tool message.
     */
    public boolean isTool() {
        return "tool".equals(role.get());
    }

    @Override
    public String toString() {
        return String.format("UIMessage[%s, %s, %s...]", 
            role.get(), status.get(), 
            content.get().substring(0, Math.min(50, content.get().length())));
    }
}
