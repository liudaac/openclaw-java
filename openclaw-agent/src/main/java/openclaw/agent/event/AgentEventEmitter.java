package openclaw.agent.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Service for emitting and listening to agent events.
 *
 * <p>This service manages the global agent event state including:
 * <ul>
 *   <li>Sequence numbers per run</li>
 *   <li>Event listeners</li>
 *   <li>Run contexts</li>
 * </ul>
 * </p>
 *
 * @author OpenClaw Team
 * @version 2026.3.21
 * @since 2026.3.21
 */
@Service
public class AgentEventEmitter {

    private static final Logger logger = LoggerFactory.getLogger(AgentEventEmitter.class);

    // Global state - maps runId to sequence counter
    private final Map<String, AtomicInteger> seqByRun;

    // Event listeners
    private final Set<Consumer<AgentEventPayload>> listeners;

    // Run contexts
    private final Map<String, AgentRunContext> runContextById;

    public AgentEventEmitter() {
        this.seqByRun = new ConcurrentHashMap<>();
        this.listeners = new CopyOnWriteArraySet<>();
        this.runContextById = new ConcurrentHashMap<>();
    }

    /**
     * Registers a run context for the given run ID.
     *
     * @param runId the run ID
     * @param context the run context
     */
    public void registerRunContext(String runId, AgentRunContext context) {
        if (runId == null || runId.isEmpty()) {
            return;
        }

        runContextById.compute(runId, (key, existing) -> {
            if (existing == null) {
                return context;
            }
            // Merge with existing context
            AgentRunContext.Builder builder = AgentRunContext.builder()
                    .heartbeat(existing.isHeartbeat())
                    .controlUiVisible(existing.isControlUiVisible());

            if (context.getSessionKey() != null) {
                builder.sessionKey(context.getSessionKey());
            } else {
                builder.sessionKey(existing.getSessionKey());
            }

            if (context.getVerboseLevel() != null) {
                builder.verboseLevel(context.getVerboseLevel());
            } else {
                builder.verboseLevel(existing.getVerboseLevel());
            }

            return builder.build();
        });
    }

    /**
     * Gets the run context for the given run ID.
     *
     * @param runId the run ID
     * @return the run context or null
     */
    public AgentRunContext getRunContext(String runId) {
        return runContextById.get(runId);
    }

    /**
     * Clears the run context for the given run ID.
     *
     * @param runId the run ID
     */
    public void clearRunContext(String runId) {
        runContextById.remove(runId);
    }

    /**
     * Emits an agent event.
     *
     * <p>The event will be enriched with:
     * <ul>
     *   <li>Sequence number (auto-incremented per run)</li>
     *   <li>Timestamp</li>
     *   <li>Session key (from run context)</li>
     * </ul>
     * </p>
     *
     * @param runId the run ID
     * @param stream the event stream
     * @param data the event data
     */
    public void emit(String runId, AgentEventStream stream, Map<String, Object> data) {
        emit(runId, stream, data, null);
    }

    /**
     * Emits an agent event with explicit session key.
     *
     * @param runId the run ID
     * @param stream the event stream
     * @param data the event data
     * @param explicitSessionKey explicit session key (overrides context)
     */
    public void emit(String runId, AgentEventStream stream, Map<String, Object> data,
                     String explicitSessionKey) {
        if (runId == null || runId.isEmpty()) {
            logger.warn("Cannot emit event with null or empty runId");
            return;
        }

        // Get next sequence number
        AtomicInteger seqCounter = seqByRun.computeIfAbsent(runId, k -> new AtomicInteger(0));
        int nextSeq = seqCounter.incrementAndGet();

        // Get run context
        AgentRunContext context = runContextById.get(runId);
        boolean isControlUiVisible = context != null && context.isControlUiVisible();

        // Determine session key
        String sessionKey = null;
        if (isControlUiVisible) {
            if (explicitSessionKey != null && !explicitSessionKey.isBlank()) {
                sessionKey = explicitSessionKey;
            } else if (context != null && context.getSessionKey() != null) {
                sessionKey = context.getSessionKey();
            }
        }

        // Build event payload
        AgentEventPayload payload = AgentEventPayload.builder()
                .runId(runId)
                .seq(nextSeq)
                .stream(stream)
                .timestamp(Instant.now())
                .data(data != null ? data : Map.of())
                .sessionKey(sessionKey)
                .build();

        // Emit to all listeners
        for (Consumer<AgentEventPayload> listener : listeners) {
            try {
                listener.accept(payload);
            } catch (Exception e) {
                logger.error("Error in event listener: {}", e.getMessage(), e);
                // Continue to next listener
            }
        }
    }

    /**
     * Subscribes to agent events.
     *
     * @param listener the event listener
     * @return a runnable that unsubscribes when called
     */
    public Runnable subscribe(Consumer<AgentEventPayload> listener) {
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    /**
     * Resets all state for testing.
     */
    public void resetForTest() {
        seqByRun.clear();
        listeners.clear();
        runContextById.clear();
    }

    /**
     * Clears sequence numbers for a specific run.
     *
     * @param runId the run ID
     */
    public void clearSequenceForRun(String runId) {
        seqByRun.remove(runId);
    }
}
