package openclaw.server.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import openclaw.session.model.Session;
import openclaw.session.store.SessionStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sessions WebSocket Handler for Control UI.
 *
 * <p>Handles session management requests from the Control UI including:
 * - sessions.list: List all sessions
 * - sessions.delete: Delete single or multiple sessions
 * - sessions.subscribe: Subscribe to session updates</p>
 *
 * @author OpenClaw Team
 * @version 2026.3.23
 */
@Component
public class SessionsWebSocketHandler implements WebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(SessionsWebSocketHandler.class);

    private final ObjectMapper objectMapper;
    private final SessionStore sessionStore;
    private final Map<String, WebSocketSession> subscriptions = new ConcurrentHashMap<>();

    public SessionsWebSocketHandler(ObjectMapper objectMapper, SessionStore sessionStore) {
        this.objectMapper = objectMapper;
        this.sessionStore = sessionStore;
    }

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        String sessionId = session.getId();

        logger.info("Sessions WebSocket connection established: {}", sessionId);

        return session.receive()
                .map(msg -> msg.getPayloadAsText())
                .flatMap(payload -> handleMessage(session, payload))
                .onErrorResume(e -> {
                    logger.error("Sessions WebSocket error: {}", e.getMessage());
                    return Mono.empty();
                })
                .doFinally(signal -> {
                    subscriptions.remove(sessionId);
                    logger.info("Sessions WebSocket connection closed: {}", sessionId);
                })
                .then();
    }

    private Mono<Void> handleMessage(WebSocketSession session, String payload) {
        try {
            SessionsMessage message = objectMapper.readValue(payload, SessionsMessage.class);

            return switch (message.method()) {
                case "sessions.list" -> handleListSessions(session, message);
                case "sessions.delete" -> handleDeleteSessions(session, message);
                case "sessions.subscribe" -> handleSubscribe(session, message);
                case "sessions.unsubscribe" -> handleUnsubscribe(session, message);
                case "ping" -> sendPong(session);
                default -> sendError(session, "Unknown method: " + message.method());
            };
        } catch (Exception e) {
            logger.error("Failed to process sessions message: {}", e.getMessage());
            return sendError(session, "Invalid message format");
        }
    }

    private Mono<Void> handleListSessions(WebSocketSession session, SessionsMessage message) {
        Map<String, Object> params = message.params() != null ? message.params() : Map.of();

        // Parse parameters
        int limit = parseIntParam(params.get("limit"), 100);
        int activeMinutes = parseIntParam(params.get("activeMinutes"), 0);
        boolean includeGlobal = parseBoolParam(params.get("includeGlobal"), true);
        boolean includeUnknown = parseBoolParam(params.get("includeUnknown"), false);

        return Mono.fromFuture(sessionStore.findAllSessions())
                .map(sessions -> {
                    // Filter by active minutes if specified
                    if (activeMinutes > 0) {
                        Instant cutoff = Instant.now().minusSeconds(activeMinutes * 60L);
                        sessions = sessions.stream()
                                .filter(s -> s.getLastActivityAt() == null || 
                                            s.getLastActivityAt().isAfter(cutoff))
                                .toList();
                    }

                    // Apply limit
                    if (sessions.size() > limit) {
                        sessions = sessions.subList(0, limit);
                    }

                    return sessions;
                })
                .flatMap(sessions -> {
                    List<Map<String, Object>> sessionList = sessions.stream()
                            .map(this::mapSessionToResponse)
                            .toList();

                    Map<String, Object> result = new HashMap<>();
                    result.put("sessions", sessionList);
                    result.put("total", sessionList.size());

                    return sendMessage(session, new SessionsMessage("sessions.list.result", result));
                })
                .onErrorResume(e -> {
                    logger.error("Failed to list sessions", e);
                    return sendError(session, "Failed to list sessions: " + e.getMessage());
                });
    }

    private Mono<Void> handleDeleteSessions(WebSocketSession session, SessionsMessage message) {
        Map<String, Object> params = message.params() != null ? message.params() : Map.of();

        Object keysParam = params.get("keys");
        if (keysParam == null) {
            return sendError(session, "Missing 'keys' parameter");
        }

        List<String> sessionKeys;
        if (keysParam instanceof List) {
            sessionKeys = ((List<?>) keysParam).stream()
                    .map(Object::toString)
                    .toList();
        } else {
            // Single key
            sessionKeys = List.of(keysParam.toString());
        }

        if (sessionKeys.isEmpty()) {
            return sendError(session, "No session keys provided");
        }

        return Mono.fromFuture(sessionStore.deleteSessions(sessionKeys))
                .flatMap(deletedCount -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("deleted", deletedCount);
                    result.put("requested", sessionKeys.size());

                    // Notify subscribers about the deletion
                    notifySessionDeletion(sessionKeys);

                    return sendMessage(session, new SessionsMessage("sessions.delete.result", result));
                })
                .onErrorResume(e -> {
                    logger.error("Failed to delete sessions: {}", sessionKeys, e);
                    return sendError(session, "Failed to delete sessions: " + e.getMessage());
                });
    }

    private Mono<Void> handleSubscribe(WebSocketSession session, SessionsMessage message) {
        subscriptions.put(session.getId(), session);
        logger.debug("Session {} subscribed to session updates", session.getId());

        Map<String, Object> result = Map.of("subscribed", true);
        return sendMessage(session, new SessionsMessage("sessions.subscribe.result", result));
    }

    private Mono<Void> handleUnsubscribe(WebSocketSession session, SessionsMessage message) {
        subscriptions.remove(session.getId());
        logger.debug("Session {} unsubscribed from session updates", session.getId());

        Map<String, Object> result = Map.of("subscribed", false);
        return sendMessage(session, new SessionsMessage("sessions.unsubscribe.result", result));
    }

    private void notifySessionDeletion(List<String> sessionKeys) {
        Map<String, Object> notification = Map.of(
                "type", "sessions.deleted",
                "keys", sessionKeys
        );

        subscriptions.values().forEach(sub -> {
            try {
                String payload = objectMapper.writeValueAsString(notification);
                sub.send(Mono.just(sub.textMessage(payload))).subscribe();
            } catch (Exception e) {
                logger.error("Failed to send deletion notification", e);
            }
        });
    }

    private Mono<Void> sendMessage(WebSocketSession session, SessionsMessage message) {
        try {
            String payload = objectMapper.writeValueAsString(message);
            return session.send(Mono.just(session.textMessage(payload)));
        } catch (Exception e) {
            logger.error("Failed to serialize message", e);
            return Mono.error(e);
        }
    }

    private Mono<Void> sendError(WebSocketSession session, String error) {
        Map<String, Object> errorPayload = Map.of(
                "error", error,
                "timestamp", System.currentTimeMillis()
        );
        return sendMessage(session, new SessionsMessage("error", errorPayload));
    }

    private Mono<Void> sendPong(WebSocketSession session) {
        Map<String, Object> pongPayload = Map.of("timestamp", System.currentTimeMillis());
        return sendMessage(session, new SessionsMessage("pong", pongPayload));
    }

    private Map<String, Object> mapSessionToResponse(Session session) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", session.getId());
        map.put("sessionKey", session.getSessionKey());
        map.put("label", session.getLabel());
        map.put("status", session.getStatus().name());
        map.put("model", session.getModel());
        map.put("createdAt", session.getCreatedAt());
        map.put("updatedAt", session.getUpdatedAt());
        map.put("lastActivityAt", session.getLastActivityAt());
        map.put("messageCount", session.getMessageCount());
        return map;
    }

    private int parseIntParam(Object value, int defaultValue) {
        if (value == null) return defaultValue;
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private boolean parseBoolParam(Object value, boolean defaultValue) {
        if (value == null) return defaultValue;
        if (value instanceof Boolean) return (Boolean) value;
        return Boolean.parseBoolean(value.toString());
    }

    /**
     * WebSocket message for sessions protocol.
     */
    public record SessionsMessage(
            String method,
            Map<String, Object> params
    ) {}
}
