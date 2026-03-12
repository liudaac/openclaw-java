package openclaw.memory.search;

import java.util.Map;
import java.util.Optional;

/**
 * Memory search result.
 *
 * @param content the content
 * @param score the relevance score
 * @param source the source
 * @param metadata additional metadata
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public record MemorySearchResult(
        String content,
        double score,
        String source,
        Map<String, Object> metadata
) {

    /**
     * Creates a builder for MemorySearchResult.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for MemorySearchResult.
     */
    public static class Builder {
        private String content;
        private double score;
        private String source;
        private Map<String, Object> metadata = Map.of();

        public Builder content(String content) {
            this.content = content;
            return this;
        }

        public Builder score(double score) {
            this.score = score;
            return this;
        }

        public Builder source(String source) {
            this.source = source;
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata != null ? metadata : Map.of();
            return this;
        }

        public MemorySearchResult build() {
            return new MemorySearchResult(content, score, source, metadata);
        }
    }
}
