package openclaw.agent.memory;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Agent memory interface for persistent storage.
 *
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public interface AgentMemory {

    /**
     * Stores a memory entry.
     *
     * @param agentId the agent ID
     * @param key the key
     * @param value the value
     * @return completion future
     */
    CompletableFuture<Void> store(String agentId, String key, Object value);

    /**
     * Retrieves a memory entry.
     *
     * @param agentId the agent ID
     * @param key the key
     * @return the value if found
     */
    CompletableFuture<Optional<Object>> retrieve(String agentId, String key);

    /**
     * Deletes a memory entry.
     *
     * @param agentId the agent ID
     * @param key the key
     * @return completion future
     */
    CompletableFuture<Void> delete(String agentId, String key);

    /**
     * Lists all keys for an agent.
     *
     * @param agentId the agent ID
     * @return list of keys
     */
    CompletableFuture<List<String>> listKeys(String agentId);

    /**
     * Searches memories.
     *
     * @param agentId the agent ID
     * @param query the query
     * @return matching entries
     */
    CompletableFuture<List<MemoryEntry>> search(String agentId, String query);

    /**
     * Clears all memories for an agent.
     *
     * @param agentId the agent ID
     * @return completion future
     */
    CompletableFuture<Void> clear(String agentId);

    /**
     * Memory entry.
     *
     * @param key the key
     * @param value the value
     * @param timestamp the timestamp
     * @param metadata additional metadata
     */
    record MemoryEntry(
            String key,
            Object value,
            long timestamp,
            Map<String, Object> metadata
    ) {
    }
}
