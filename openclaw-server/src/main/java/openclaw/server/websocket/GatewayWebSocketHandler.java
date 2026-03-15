package openclaw.server.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import openclaw.agent.AcpProtocol;
import openclaw.gateway.GatewayService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gateway WebSocket Handler
 */
@Component
public class GatewayWebSocketHandler implements WebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(GatewayWebSocketHandler.class);

    private final ObjectMapper objectMapper;
    private final GatewayService gatewayService;
    private final AcpProtocol acpProtocol;
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    public GatewayWebSocketHandler(ObjectMapper objectMapper,
                                   GatewayService gatewayService,
                                   AcpProtocol acpProtocol) {
        this.objectMapper = objectMapper;
        this.gatewayService = gatewayService;
        this.acpProtocol = acpProtocol;
    }

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        String sessionId = session.getId();
        sessions.put(sessionId, session);

        logger.info("WebSocket connection established: {}", sessionId);

        return session.receive()
                .map(msg -> msg.getPayloadAsText())
                .flatMap(payload -> handleMessage(session, payload))
                .onErrorResume(e -> {
                    logger.error("WebSocket error: {}", e.getMessage());
                    return Mono.empty();
                })
                .doFinally(signal -> {
                    sessions.remove(sessionId);
                    logger.info("WebSocket connection closed: {}", sessionId);
                })
                .then();
    }

    private Mono<Void> handleMessage(WebSocketSession session, String payload) {
        try {
            WebSocketMessage message = objectMapper.readValue(payload, WebSocketMessage.class);

            switch (message.type()) {
                case "agent.spawn":
                    return handleAgentSpawn(session, message);
                case "agent.message":
                    return handleAgentMessage(session, message);
                case "gateway.submit":
                    return handleGatewaySubmit(session, message);
                case "ping":
                    return sendMessage(session, new WebSocketMessage("pong", Map.of("timestamp", System.currentTimeMillis())));
                default:
                    return sendError(session, "Unknown message type: " + message.type());
            }
        } catch (Exception e) {
            logger.error("Failed to process message: {}", e.getMessage());
            return sendError(session, "Invalid message format");
        }
    }

    private Mono<Void> handleAgentSpawn(WebSocketSession session, WebSocketMessage message) {
        String userMessage = (String) message.payload().get("message");
        String model = (String) message.payload().getOrDefault("model", "gpt-4");

        AcpProtocol.SpawnRequest request = AcpProtocol.SpawnRequest.builder()
                .sessionKey(session.getId())
                .userMessage(userMessage)
                .model(model)
                .build();

        return Mono.fromFuture(acpProtocol.spawnAgent(request))
                .flatMap(result -> {
                    if (result.success()) {
                        return sendMessage(session, new WebSocketMessage("agent.spawned", Map.of(
                                "sessionKey", result.sessionKey(),
                                "agentId", result.agentId()
                        )));
                    } else {
                        return sendError(session, result.error().orElse("Spawn failed"));
                    }
                });
    }

    private Mono<Void> handleAgentMessage(WebSocketSession session, WebSocketMessage message) {
        String sessionKey = (String) message.payload().get("sessionKey");
        String content = (String) message.payload().get("content");

        AcpProtocol.AgentMessage agentMessage = AcpProtocol.AgentMessage.user(content);

        return Mono.fromFuture(acpProtocol.sendMessage(sessionKey, agentMessage))
                .then(sendMessage(session, new WebSocketMessage("agent.message.sent", Map.of(
                        "sessionKey", sessionKey,
                        "timestamp", System.currentTimeMillis()
                ))));
    }

    private Mono<Void> handleGatewaySubmit(WebSocketSession session, WebSocketMessage message) {
        String type = (String) message.payload().get("workType");
        String payload_data = (String) message.payload().get("payload");

        GatewayService.WorkItem workItem = GatewayService.WorkItem.of(
                GatewayService.WorkType.valueOf(type),
                payload_data.getBytes()
        );

        return Mono.fromFuture(gatewayService.submitWork(workItem))
                .flatMap(workId -> sendMessage(session, new WebSocketMessage("gateway.work.submitted", Map.of(
                        "workId", workId
                ))));
    }

    private Mono<Void> sendMessage(WebSocketSession session, WebSocketMessage message) {
        try {
            String json = objectMapper.writeValueAsString(message);
            return session.send(Mono.just(session.textMessage(json)));
        } catch (Exception e) {
            logger.error("Failed to serialize: {}", e.getMessage());
            return Mono.error(e);
        }
    }

    private Mono<Void> sendError(WebSocketSession session, String error) {
        return sendMessage(session, new WebSocketMessage("error", Map.of("message", error)));
    }

    /**
     * Get the current connection count.
     *
     * @return the number of active WebSocket connections
     */
    public int getConnectionCount() {
        return sessions.size();
    }

    public record WebSocketMessage(String type, Map<String, Object> payload) {}
}
