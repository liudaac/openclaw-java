package openclaw.agent.context;

import openclaw.agent.event.AgentEventEmitter;
import openclaw.agent.event.AgentEventPayload;
import openclaw.agent.event.AgentEventStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;

/**
 * Service for notifying users about context compaction events.
 *
 * <p>This service manages:
 * <ul>
 *   <li>Compaction start notifications</li>
 *   <li>Compaction completion notifications</li>
 *   <li>Event listeners for compaction events</li>
 * </ul>
 * </p>
 *
 * @author OpenClaw Team
 * @version 2026.3.21
 * @since 2026.3.21
 */
@Service
public class ContextCompactionNotifier {

    private static final Logger logger = LoggerFactory.getLogger(ContextCompactionNotifier.class);

    private final AgentEventEmitter eventEmitter;
    private final Set<Consumer<CompactionEvent>> listeners;
    private final Map<String, CompactionState> compactionStates;

    public ContextCompactionNotifier(AgentEventEmitter eventEmitter) {
        this.eventEmitter = eventEmitter;
        this.listeners = new CopyOnWriteArraySet<>();
        this.compactionStates = new ConcurrentHashMap<>();
    }

    /**
     * Notifies that context compaction has started.
     *
     * @param runId the run ID
     * @param sessionKey the session key
     */
    public void notifyCompactionStart(String runId, String sessionKey) {
        logger.debug("Context compaction started for run: {}, session: {}", runId, sessionKey);

        // Store compaction state
        compactionStates.put(runId, new CompactionState(sessionKey, CompactionPhase.STARTED));

        // Emit agent event
        eventEmitter.emit(runId, AgentEventStream.LIFECYCLE, Map.of(
                "type", "compaction",
                "phase", "start",
                "sessionKey", sessionKey != null ? sessionKey : ""
        ), sessionKey);

        // Notify listeners
        CompactionEvent event = new CompactionEvent(
                runId,
                sessionKey,
                CompactionPhase.STARTED,
                null,
                false
        );
        notifyListeners(event);
    }

    /**
     * Notifies that context compaction has completed.
     *
     * @param runId the run ID
     * @param willRetry whether the request will be retried
     * @param completed whether compaction actually completed
     */
    public void notifyCompactionEnd(String runId, boolean willRetry, boolean completed) {
        CompactionState state = compactionStates.get(runId);
        String sessionKey = state != null ? state.sessionKey() : null;

        logger.debug("Context compaction ended for run: {}, willRetry: {}, completed: {}",
                runId, willRetry, completed);

        // Update compaction state
        compactionStates.put(runId, new CompactionState(sessionKey, CompactionPhase.ENDED));

        // Emit agent event
        eventEmitter.emit(runId, AgentEventStream.LIFECYCLE, Map.of(
                "type", "compaction",
                "phase", "end",
                "willRetry", willRetry,
                "completed", completed,
                "sessionKey", sessionKey != null ? sessionKey : ""
        ), sessionKey);

        // Notify listeners
        CompactionEvent event = new CompactionEvent(
                runId,
                sessionKey,
                CompactionPhase.ENDED,
                willRetry,
                completed
        );
        notifyListeners(event);

        // Clean up state if not retrying
        if (!willRetry) {
            compactionStates.remove(runId);
        }
    }

    /**
     * Subscribes to compaction events.
     *
     * @param listener the event listener
     * @return a runnable that unsubscribes when called
     */
    public Runnable subscribe(Consumer<CompactionEvent> listener) {
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    /**
     * Checks if compaction is in flight for a run.
     *
     * @param runId the run ID
     * @return true if compaction is in progress
     */
    public boolean isCompactionInFlight(String runId) {
        CompactionState state = compactionStates.get(runId);
        return state != null && state.phase() == CompactionPhase.STARTED;
    }

    /**
     * Gets the compaction state for a run.
     *
     * @param runId the run ID
     * @return the state or null
     */
    public CompactionState getCompactionState(String runId) {
        return compactionStates.get(runId);
    }

    /**
     * Clears all compaction states (for testing).
     */
    public void resetForTest() {
        compactionStates.clear();
        listeners.clear();
    }

    private void notifyListeners(CompactionEvent event) {
        for (Consumer<CompactionEvent> listener : listeners) {
            try {
                listener.accept(event);
            } catch (Exception e) {
                logger.error("Error in compaction event listener: {}", e.getMessage(), e);
            }
        }
    }

    /**
     * Compaction event.
     */
    public record CompactionEvent(
            String runId,
            String sessionKey,
            CompactionPhase phase,
            Boolean willRetry,
            boolean completed
    ) {
        /**
         * Checks if this is a start event.
         *
         * @return true if start
         */
        public boolean isStart() {
            return phase == CompactionPhase.STARTED;
        }

        /**
         * Checks if this is an end event.
         *
         * @return true if end
         */
        public boolean isEnd() {
            return phase == CompactionPhase.ENDED;
        }
    }

    /**
     * Compaction phase.
     */
    public enum CompactionPhase {
        STARTED,
        ENDED
    }

    /**
     * Compaction state.
     */
    public record CompactionState(
            String sessionKey,
            CompactionPhase phase
    ) {}
}
