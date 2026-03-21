package openclaw.agent.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lightweight in-memory queue for human-readable system events.
 *
 * <p>Events are session-scoped and ephemeral (not persisted).
 * They can be prefixed to the next prompt for user notification.</p>
 *
 * @author OpenClaw Team
 * @version 2026.3.21
 * @since 2026.3.21
 */
@Service
public class SystemEventQueue {

    private static final Logger logger = LoggerFactory.getLogger(SystemEventQueue.class);
    private static final int MAX_EVENTS = 20;

    private final Map<String, SessionQueue> queues;

    public SystemEventQueue() {
        this.queues = new ConcurrentHashMap<>();
    }

    /**
     * Enqueues a system event.
     *
     * @param text the event text
     * @param options the event options
     * @return true if enqueued, false if skipped
     */
    public boolean enqueue(String text, SystemEventOptions options) {
        String sessionKey = requireSessionKey(options.sessionKey());
        String cleaned = text != null ? text.trim() : "";

        if (cleaned.isEmpty()) {
            return false;
        }

        SessionQueue entry = queues.computeIfAbsent(sessionKey, k -> new SessionQueue());
        String normalizedContextKey = normalizeContextKey(options.contextKey());

        // Skip consecutive duplicates
        if (cleaned.equals(entry.lastText)) {
            return false;
        }

        entry.lastText = cleaned;
        entry.lastContextKey = normalizedContextKey;
        entry.queue.add(new SystemEvent(cleaned, Instant.now(), normalizedContextKey));

        // Limit queue size
        while (entry.queue.size() > MAX_EVENTS) {
            entry.queue.poll();
        }

        logger.debug("Enqueued system event for session {}: {}", sessionKey, cleaned);
        return true;
    }

    /**
     * Drains all events from a session queue.
     *
     * @param sessionKey the session key
     * @return list of events
     */
    public List<SystemEvent> drain(String sessionKey) {
        String key = requireSessionKey(sessionKey);
        SessionQueue entry = queues.remove(key);

        if (entry == null || entry.queue.isEmpty()) {
            return List.of();
        }

        List<SystemEvent> result = new ArrayList<>(entry.queue);
        entry.queue.clear();
        entry.lastText = null;
        entry.lastContextKey = null;

        return result;
    }

    /**
     * Drains events as text strings.
     *
     * @param sessionKey the session key
     * @return list of event texts
     */
    public List<String> drainTexts(String sessionKey) {
        return drain(sessionKey).stream()
                .map(SystemEvent::text)
                .toList();
    }

    /**
     * Peeks at events without removing.
     *
     * @param sessionKey the session key
     * @return list of events
     */
    public List<SystemEvent> peek(String sessionKey) {
        String key = requireSessionKey(sessionKey);
        SessionQueue entry = queues.get(key);

        if (entry == null || entry.queue.isEmpty()) {
            return List.of();
        }

        return entry.queue.stream()
                .map(e -> new SystemEvent(e.text(), e.timestamp(), e.contextKey()))
                .toList();
    }

    /**
     * Peeks at event texts without removing.
     *
     * @param sessionKey the session key
     * @return list of event texts
     */
    public List<String> peekTexts(String sessionKey) {
        return peek(sessionKey).stream()
                .map(SystemEvent::text)
                .toList();
    }

    /**
     * Checks if a session has events.
     *
     * @param sessionKey the session key
     * @return true if has events
     */
    public boolean hasEvents(String sessionKey) {
        String key = requireSessionKey(sessionKey);
        SessionQueue entry = queues.get(key);
        return entry != null && !entry.queue.isEmpty();
    }

    /**
     * Checks if context has changed.
     *
     * @param sessionKey the session key
     * @param contextKey the context key
     * @return true if context changed
     */
    public boolean isContextChanged(String sessionKey, String contextKey) {
        String key = requireSessionKey(sessionKey);
        SessionQueue entry = queues.get(key);
        String normalized = normalizeContextKey(contextKey);
        return !Objects.equals(normalized, entry != null ? entry.lastContextKey : null);
    }

    /**
     * Clears all events for a session.
     *
     * @param sessionKey the session key
     */
    public void clear(String sessionKey) {
        String key = requireSessionKey(sessionKey);
        queues.remove(key);
    }

    /**
     * Resets all queues (for testing).
     */
    public void resetForTest() {
        queues.clear();
    }

    private String requireSessionKey(String key) {
        String trimmed = key != null ? key.trim() : "";
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("System events require a sessionKey");
        }
        return trimmed;
    }

    private String normalizeContextKey(String key) {
        if (key == null) {
            return null;
        }
        String trimmed = key.trim();
        return trimmed.isEmpty() ? null : trimmed.toLowerCase();
    }

    /**
     * System event.
     */
    public record SystemEvent(
            String text,
            Instant timestamp,
            String contextKey
    ) {}

    /**
     * System event options.
     */
    public record SystemEventOptions(
            String sessionKey,
            String contextKey
    ) {
        public SystemEventOptions(String sessionKey) {
            this(sessionKey, null);
        }
    }

    /**
     * Session queue entry.
     */
    private static class SessionQueue {
        final Queue<SystemEvent> queue = new ArrayDeque<>();
        String lastText;
        String lastContextKey;
    }
}
