package openclaw.memory;

import java.util.Map;

/**
 * Memory entry representing a stored memory/embedding
 */
public record MemoryEntry(
    String id,
    String text,
    float[] vector,
    Map<String, Object> metadata,
    long timestamp,
    String sessionKey
) {
    /**
     * Builder for MemoryEntry
     */
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String id;
        private String text;
        private float[] vector;
        private Map<String, Object> metadata = Map.of();
        private long timestamp = System.currentTimeMillis();
        private String sessionKey;
        
        public Builder id(String id) {
            this.id = id;
            return this;
        }
        
        public Builder text(String text) {
            this.text = text;
            return this;
        }
        
        public Builder vector(float[] vector) {
            this.vector = vector;
            return this;
        }
        
        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }
        
        public Builder timestamp(long timestamp) {
            this.timestamp = timestamp;
            return this;
        }
        
        public Builder sessionKey(String sessionKey) {
            this.sessionKey = sessionKey;
            return this;
        }
        
        public MemoryEntry build() {
            return new MemoryEntry(id, text, vector, metadata, timestamp, sessionKey);
        }
    }
}
