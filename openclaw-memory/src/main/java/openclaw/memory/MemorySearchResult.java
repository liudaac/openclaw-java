package openclaw.memory;

/**
 * Memory search result with relevance score
 */
public record MemorySearchResult(
    String id,
    double score,
    MemoryEntry entry
) {
    public MemorySearchResult {
        if (score < 0 || score > 1) {
            throw new IllegalArgumentException("Score must be between 0 and 1");
        }
    }
}
