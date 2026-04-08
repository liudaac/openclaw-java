package openclaw.server.controller;

import openclaw.agent.AcpProtocol;
import openclaw.agent.AcpProtocol.*;
import openclaw.server.http.ClientDisconnectHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

/**
 * Agent REST API Controller
 *
 * <p>Provides HTTP endpoints for agent operations using the ACP (Agent Communication Protocol).</p>
 * <p>Supports client disconnect detection for long-running operations.</p>
 */
@RestController
@RequestMapping("/api/v1/agent")
public class AgentController {

    private static final Logger logger = LoggerFactory.getLogger(AgentController.class);

    private final AcpProtocol acpProtocol;
    private final ClientDisconnectHandler disconnectHandler;

    public AgentController(AcpProtocol acpProtocol, ClientDisconnectHandler disconnectHandler) {
        this.acpProtocol = acpProtocol;
        this.disconnectHandler = disconnectHandler;
    }

    /**
     * Spawn a new agent session
     */
    @PostMapping("/spawn")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<SpawnResponse> spawnAgent(@RequestBody SpawnRequestDto request) {
        String requestId = UUID.randomUUID().toString();
        ClientDisconnectHandler.RequestContext context = disconnectHandler.createContext(requestId);

        String sessionKey = request.sessionKey() != null ?
                request.sessionKey() : UUID.randomUUID().toString();

        logger.info("Spawning agent session: {} (request: {})", sessionKey, requestId);

        SpawnRequest spawnRequest = SpawnRequest.builder()
                .sessionKey(sessionKey)
                .userMessage(request.message())
                .systemPrompt(request.systemPrompt())
                .model(request.model() != null ? request.model() : "gpt-4")
                .tools(request.tools() != null ? request.tools() : Map.of())
                .metadata(request.metadata() != null ? request.metadata() : Map.of())
                .lightContext(request.lightContext() != null ? request.lightContext() : false)
                .build();

        return Mono.fromFuture(acpProtocol.spawnAgent(spawnRequest))
                .doFinally(signal -> disconnectHandler.removeContext(requestId))
                .map(result -> new SpawnResponse(
                        result.success(),
                        result.sessionKey(),
                        result.agentId(),
                        result.error().orElse(null)
                ));
    }

    /**
     * Send message to agent
     */
    @PostMapping("/{sessionKey}/message")
    public Mono<MessageResponse> sendMessage(
            @PathVariable String sessionKey,
            @RequestBody MessageRequestDto request) {

        AgentMessage message = AgentMessage.user(request.message());

        return Mono.fromFuture(acpProtocol.sendMessage(sessionKey, message))
                .thenReturn(new MessageResponse("sent", sessionKey, System.currentTimeMillis()));
    }

    /**
     * Get agent messages
     */
    @GetMapping("/{sessionKey}/messages")
    public Mono<MessagesResponse> getMessages(
            @PathVariable String sessionKey,
            @RequestParam(defaultValue = "10") int limit) {

        return Mono.fromFuture(acpProtocol.getMessages(sessionKey, limit))
                .map(agentMessages -> new MessagesResponse(
                        agentMessages.messages().stream()
                                .map(m -> new MessageDto(m.role(), m.content(), m.timestamp()))
                                .toList(),
                        agentMessages.hasMore()
                ));
    }

    /**
     * Wait for agent completion (blocking)
     * Supports client disconnect detection - will abort if client disconnects.
     */
    @PostMapping("/{sessionKey}/wait")
    public Mono<WaitResponse> waitForCompletion(
            @PathVariable String sessionKey,
            @RequestParam(defaultValue = "60000") long timeoutMs) {

        String requestId = UUID.randomUUID().toString();
        ClientDisconnectHandler.RequestContext context = disconnectHandler.createContext(requestId);

        logger.debug("Waiting for agent {} (request: {})", sessionKey, requestId);

        return Mono.fromFuture(acpProtocol.waitForAgent(sessionKey, timeoutMs))
                .doFinally(signal -> disconnectHandler.removeContext(requestId))
                .map(result -> new WaitResponse(
                        result.status().name(),
                        result.result().orElse(null),
                        result.error().orElse(null)
                ));
    }

    /**
     * Stream agent responses
     * Automatically stops if client disconnects.
     */
    @GetMapping(value = "/{sessionKey}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<StreamResponse> streamResponses(@PathVariable String sessionKey) {
        String requestId = UUID.randomUUID().toString();
        ClientDisconnectHandler.RequestContext context = disconnectHandler.createContext(requestId);

        logger.debug("Starting stream for agent {} (request: {})", sessionKey, requestId);

        return Flux.interval(Duration.ofMillis(100))
                .flatMap(i -> Mono.fromFuture(acpProtocol.getMessages(sessionKey, 1)))
                .flatMap(agentMessages -> Flux.fromIterable(agentMessages.messages()))
                .filter(m -> m.role().equals("assistant"))
                .map(m -> new StreamResponse("content", m.content(), m.timestamp()))
                .take(Duration.ofMinutes(5))
                .takeUntilOther(context.onCancel())
                .doFinally(signal -> {
                    logger.debug("Stream ended for agent {} (request: {}, signal: {})",
                            sessionKey, requestId, signal);
                    disconnectHandler.removeContext(requestId);
                });
    }

    /**
     * Delete agent session
     */
    @DeleteMapping("/{sessionKey}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteSession(@PathVariable String sessionKey) {
        logger.info("Deleting agent session: {}", sessionKey);
        return Mono.fromFuture(acpProtocol.deleteSession(sessionKey));
    }

    // Request/Response DTOs
    public record SpawnRequestDto(
            String sessionKey,
            String message,
            String systemPrompt,
            String model,
            Map<String, Object> tools,
            Map<String, Object> metadata,
            Boolean lightContext
    ) {}

    public record SpawnResponse(
            boolean success,
            String sessionKey,
            String agentId,
            String error
    ) {}

    public record MessageRequestDto(
            String message
    ) {}

    public record MessageResponse(
            String status,
            String sessionKey,
            long timestamp
    ) {}

    public record MessageDto(
            String role,
            String content,
            long timestamp
    ) {}

    public record MessagesResponse(
            java.util.List<MessageDto> messages,
            boolean hasMore
    ) {}

    public record WaitResponse(
            String status,
            String result,
            String error
    ) {}

    public record StreamResponse(
            String type,
            String content,
            long timestamp
    ) {}
}
