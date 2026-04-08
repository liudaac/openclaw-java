package openclaw.memory.wiki;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Public Artifact entity.
 *
 * <p>Represents a shareable artifact stored in the memory wiki.</p>
 *
 * @author OpenClaw Team
 * @version 2026.4.8
 */
public class PublicArtifact {

    private final String artifactId;
    private String sessionKey;
    private String name;
    private String content;
    private MemoryWikiService.ArtifactType type;
    private Map<String, Object> metadata;
    private final Instant createdAt;
    private Instant updatedAt;
    private int accessCount;
    private boolean isPublic;

    public PublicArtifact() {
        this.artifactId = UUID.randomUUID().toString();
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        this.accessCount = 0;
        this.isPublic = true;
    }

    public PublicArtifact(String sessionKey, String name, String content, MemoryWikiService.ArtifactType type) {
        this();
        this.sessionKey = sessionKey;
        this.name = name;
        this.content = content;
        this.type = type;
    }

    // Getters and Setters

    public String getArtifactId() {
        return artifactId;
    }

    public String getSessionKey() {
        return sessionKey;
    }

    public void setSessionKey(String sessionKey) {
        this.sessionKey = sessionKey;
        this.updatedAt = Instant.now();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        this.updatedAt = Instant.now();
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
        this.updatedAt = Instant.now();
    }

    public MemoryWikiService.ArtifactType getType() {
        return type;
    }

    public void setType(MemoryWikiService.ArtifactType type) {
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

    public int getAccessCount() {
        return accessCount;
    }

    public void incrementAccessCount() {
        this.accessCount++;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public void setPublic(boolean isPublic) {
        this.isPublic = isPublic;
        this.updatedAt = Instant.now();
    }

    /**
     * Get artifact size in bytes.
     *
     * @return size in bytes
     */
    public int getSize() {
        return content != null ? content.getBytes().length : 0;
    }

    @Override
    public String toString() {
        return String.format("PublicArtifact{artifactId='%s', name='%s', type=%s}",
                artifactId, name, type);
    }
}
