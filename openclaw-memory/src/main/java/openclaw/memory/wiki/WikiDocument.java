package openclaw.memory.wiki;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Wiki Document entity.
 *
 * <p>Represents a document in the memory wiki.</p>
 *
 * @author OpenClaw Team
 * @version 2026.4.8
 */
public class WikiDocument {

    private final String documentId;
    private String sessionKey;
    private String title;
    private String content;
    private String format; // markdown, html, text
    private Map<String, Object> metadata;
    private final Instant createdAt;
    private Instant updatedAt;
    private double relevance;

    public WikiDocument() {
        this.documentId = UUID.randomUUID().toString();
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        this.format = "markdown";
        this.relevance = 1.0;
    }

    public WikiDocument(String sessionKey, String title, String content) {
        this();
        this.sessionKey = sessionKey;
        this.title = title;
        this.content = content;
    }

    // Getters and Setters

    public String getDocumentId() {
        return documentId;
    }

    public String getSessionKey() {
        return sessionKey;
    }

    public void setSessionKey(String sessionKey) {
        this.sessionKey = sessionKey;
        this.updatedAt = Instant.now();
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
        this.updatedAt = Instant.now();
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
        this.updatedAt = Instant.now();
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
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

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public double getRelevance() {
        return relevance;
    }

    public void setRelevance(double relevance) {
        this.relevance = Math.max(0.0, Math.min(1.0, relevance));
    }

    @Override
    public String toString() {
        return String.format("WikiDocument{documentId='%s', title='%s', format='%s'}",
                documentId, title, format);
    }
}
