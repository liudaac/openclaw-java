package openclaw.server.controller;

import openclaw.sdk.channel.ChannelMessage;
import openclaw.sdk.channel.ChannelOutboundAdapter;
import openclaw.sdk.channel.ChannelPlugin;
import openclaw.sdk.channel.SendResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Channel REST API Controller
 *
 * <p>Provides HTTP endpoints for channel operations.</p>
 *
 * @author OpenClaw Team
 * @version 2026.3.13
 */
@RestController
@RequestMapping("/api/v1/channels")
public class ChannelController {

    private static final Logger logger = LoggerFactory.getLogger(ChannelController.class);

    private final ChannelPlugin channelPlugin;

    public ChannelController(ChannelPlugin channelPlugin) {
        this.channelPlugin = channelPlugin;
    }

    /**
     * Send a text message
     */
    @PostMapping("/{channel}/send")
    public Mono<ResponseEntity<Map<String, Object>>> sendMessage(
            @PathVariable String channel,
            @RequestBody SendMessageRequest request) {

        logger.info("Sending message to channel: {}", channel);

        ChannelMessage message = ChannelMessage.builder()
                .text(request.text())
                .from(request.from())
                .fromName(request.fromName())
                .chatId(request.chatId())
                .messageId(request.messageId())
                .metadata(request.metadata())
                .build();

        return Mono.fromFuture(channelPlugin.sendMessage(message))
                .map(result -> ResponseEntity.ok(Map.of(
                        "success", result.success(),
                        "messageId", result.messageId(),
                        "timestamp", result.timestamp()
                )))
                .onErrorResume(e -> {
                    logger.error("Failed to send message", e);
                    return Mono.just(ResponseEntity.badRequest()
                            .body(Map.of(
                                    "success", false,
                                    "error", e.getMessage()
                            )));
                });
    }

    /**
     * Send a typing indicator
     */
    @PostMapping("/{channel}/typing")
    public Mono<ResponseEntity<Map<String, Object>>> sendTyping(
            @PathVariable String channel,
            @RequestParam String chatId) {

        logger.debug("Sending typing indicator to: {}", chatId);

        return Mono.fromFuture(channelPlugin.sendTypingIndicator(chatId))
                .thenReturn(ResponseEntity.ok(Map.of(
                        "success", true,
                        "chatId", chatId
                )))
                .onErrorResume(e -> {
                    logger.error("Failed to send typing indicator", e);
                    return Mono.just(ResponseEntity.badRequest()
                            .body(Map.of(
                                    "success", false,
                                    "error", e.getMessage()
                            )));
                });
    }

    /**
     * Get channel info
     */
    @GetMapping("/{channel}/info")
    public Mono<ResponseEntity<Map<String, Object>>> getChannelInfo(
            @PathVariable String channel) {

        return Mono.fromCallable(() -> ResponseEntity.ok(Map.of(
                "channel", channel,
                "name", channelPlugin.getChannelName(),
                "available", channelPlugin.isAvailable(),
                "capabilities", channelPlugin.getCapabilities()
        )));
    }

    // Request/Response Records

    public record SendMessageRequest(
            String text,
            String from,
            String fromName,
            String chatId,
            String messageId,
            Map<String, Object> metadata
    ) {}
}
