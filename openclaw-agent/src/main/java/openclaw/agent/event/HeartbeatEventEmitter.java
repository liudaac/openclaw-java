package openclaw.agent.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;

/**
 * Service for emitting and listening to heartbeat events.
 *
 * <p>This service manages heartbeat event state including:
 * <ul>
 *   <li>Last heartbeat event</li>
 *   <li>Event listeners</li>
 * </ul>
 * </p>
 *
 * @author OpenClaw Team
 * @version 2026.3.21
 * @since 2026.3.21
 */
@Service
public class HeartbeatEventEmitter {

    private static final Logger logger = LoggerFactory.getLogger(HeartbeatEventEmitter.class);

    private HeartbeatEventPayload lastHeartbeat;
    private final Set<Consumer<HeartbeatEventPayload>> listeners;

    public HeartbeatEventEmitter() {
        this.listeners = new CopyOnWriteArraySet<>();
    }

    /**
     * Emits a heartbeat event.
     *
     * <p>The event will be enriched with the current timestamp.</p>
     *
     * @param status the heartbeat status
     * @param to the recipient
     * @param channel the channel
     */
    public void emit(HeartbeatStatus status, String to, String channel) {
        emit(HeartbeatEventPayload.builder()
                .status(status)
                .to(to)
                .channel(channel)
                .build());
    }

    /**
     * Emits a heartbeat event with full payload.
     *
     * @param payload the event payload (timestamp will be set automatically)
     */
    public void emit(HeartbeatEventPayload payload) {
        HeartbeatEventPayload enriched = HeartbeatEventPayload.builder()
                .timestamp(Instant.now())
                .status(payload.getStatus())
                .to(payload.getTo())
                .accountId(payload.getAccountId())
                .preview(payload.getPreview())
                .durationMs(payload.getDurationMs())
                .hasMedia(payload.getHasMedia())
                .reason(payload.getReason())
                .channel(payload.getChannel())
                .silent(payload.getSilent())
                .indicatorType(payload.getIndicatorType())
                .build();

        // Store as last heartbeat
        this.lastHeartbeat = enriched;

        // Emit to all listeners
        for (Consumer<HeartbeatEventPayload> listener : listeners) {
            try {
                listener.accept(enriched);
            } catch (Exception e) {
                logger.error("Error in heartbeat event listener: {}", e.getMessage(), e);
                // Continue to next listener
            }
        }
    }

    /**
     * Subscribes to heartbeat events.
     *
     * @param listener the event listener
     * @return a runnable that unsubscribes when called
     */
    public Runnable subscribe(Consumer<HeartbeatEventPayload> listener) {
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    /**
     * Gets the last heartbeat event.
     *
     * @return the last heartbeat or null
     */
    public HeartbeatEventPayload getLastHeartbeat() {
        return lastHeartbeat;
    }

    /**
     * Resets state for testing.
     */
    public void resetForTest() {
        lastHeartbeat = null;
        listeners.clear();
    }
}
