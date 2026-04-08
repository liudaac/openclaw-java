package openclaw.memory.wiki;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Belief Layer Digest entity.
 *
 * <p>Represents a condensed belief or insight extracted from session data.</p>
 *
 * @author OpenClaw Team
 * @version 2026.4.8
 */
public class BeliefLayerDigest {

    private final String digestId;
    private String sessionKey;
    private String content;
    private MemoryWikiService.DigestType type;
    private Map<String, Object> metadata;
    private final Instant createdAt;
    private Instant updatedAt;
    private double confidence;
    private int referenceCount;

    public BeliefLayerDigest() {
        this.digestId = UUID.randomUUID().toString();
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        this.confidence = 1.0;
        this.referenceCount = 0;
    }

    public BeliefLayerDigest(String sessionKey, String content, MemoryWikiService.DigestType type) {
        this();
        this.sessionKey = sessionKey;
        this.content = content;
        this.type = type;
    }

    // Getters and Setters

    public String getDigestId() {
        return digestId;
    }

    public String getSessionKey() {
        return sessionKey;
    }

    public void setSessionKey(String sessionKey) {
        this.sessionKey = sessionKey;
        this.updatedAt = Instant.now();
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
        this.updatedAt = Instant.now();
    }

    public MemoryWikiService.DigestType getType() {
        return type;
    }

    public void setType(MemoryWikiService.DigestType type) {
        this.type = type;
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

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = Math.max(0.0, Math.min(1.0, confidence));
        this.updatedAt = Instant.now();
    }

    public int getReferenceCount() {
        return referenceCount;
    }

    public void incrementReferenceCount() {
        this.referenceCount++;
        this.updatedAt = Instant.now();
    }

    /**
     * Check if this digest is high confidence.
     *
     * @return true if confidence >= 0.8
     */
    public boolean isHighConfidence() {
        return confidence >= 0.8;
    }

    @Override
    public String toString() {
        return String.format("BeliefLayerDigest{digestId='%s', type=%s, confidence=%.2f}",
                digestId, type, confidence);
    }
}
