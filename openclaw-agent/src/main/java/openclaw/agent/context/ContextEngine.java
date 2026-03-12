package openclaw.agent.context;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Context engine for managing agent context.
 *
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public interface ContextEngine {

    /**
     * Initializes the context engine.
     *
     * @param config the configuration
     * @return completion future
     */
    CompletableFuture<Void> initialize(ContextConfig config);

    /**
     * Bootstraps context for a session.
     *
     * @param sessionKey the session key
     * @param context the initial context
     * @return the bootstrapped context
     */
    CompletableFuture<ContextSnapshot> bootstrap(String sessionKey, Map<String, Object> context);

    /**
     * Ingests data into context.
     *
     * @param sessionKey the session key
     * @param data the data to ingest
     * @return the updated context
     */
    CompletableFuture<ContextSnapshot> ingest(String sessionKey, IngestData data);

    /**
     * Assembles context for agent execution.
     *
     * @param sessionKey the session key
     * @return the assembled context
     */
    CompletableFuture<ContextSnapshot> assemble(String sessionKey);

    /**
     * Compacts context to reduce size.
     *
     * @param sessionKey the session key
     * @return the compacted context
     */
    CompletableFuture<ContextSnapshot> compact(String sessionKey);

    /**
     * Gets context for a session.
     *
     * @param sessionKey the session key
     * @return the context if found
     */
    CompletableFuture<Optional<ContextSnapshot>> getContext(String sessionKey);

    /**
     * Clears context for a session.
     *
     * @param sessionKey the session key
     * @return completion future
     */
    CompletableFuture<Void> clearContext(String sessionKey);

    /**
     * Context configuration.
     *
     * @param maxContextSize the maximum context size
     * @param maxTokens the maximum tokens
     * @param enableCompression whether to enable compression
     */
    record ContextConfig(
            int maxContextSize,
            int maxTokens,
            boolean enableCompression
    ) {
        public static ContextConfig defaults() {
            return new ContextConfig(100000, 8000, true);
        }
    }

    /**
     * Context snapshot.
     *
     * @param sessionKey the session key
     * @param messages the messages
     * @param metadata the metadata
     * @param tokenCount the token count
     * @param timestamp the timestamp
     */
    record ContextSnapshot(
            String sessionKey,
            List<ContextMessage> messages,
            Map<String, Object> metadata,
            int tokenCount,
            long timestamp
    ) {
    }

    /**
     * Context message.
     *
     * @param role the role
     * @param content the content
     * @param tokenCount the token count
     */
    record ContextMessage(
            String role,
            String content,
            int tokenCount
    ) {
    }

    /**
     * Ingest data.
     *
     * @param type the data type
     * @param content the content
     * @param metadata additional metadata
     */
    record IngestData(
            DataType type,
            String content,
            Map<String, Object> metadata
    ) {
        public enum DataType {
            FILE,
            MESSAGE,
            TOOL_RESULT,
            SYSTEM_EVENT
        }
    }

    /**
     * Context hook for plugins.
     */
    interface ContextHook {
        /**
         * Called during bootstrap.
         *
         * @param context the context
         * @return the modified context
         */
        default Map<String, Object> onBootstrap(Map<String, Object> context) {
            return context;
        }

        /**
         * Called during ingest.
         *
         * @param data the ingest data
         * @return the modified data
         */
        default IngestData onIngest(IngestData data) {
            return data;
        }

        /**
         * Called during assemble.
         *
         * @param snapshot the context snapshot
         * @return the modified snapshot
         */
        default ContextSnapshot onAssemble(ContextSnapshot snapshot) {
            return snapshot;
        }

        /**
         * Called during compact.
         *
         * @param snapshot the context snapshot
         * @return the modified snapshot
         */
        default ContextSnapshot onCompact(ContextSnapshot snapshot) {
            return snapshot;
        }
    }
}
