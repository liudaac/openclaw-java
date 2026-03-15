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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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

    private final ChannelPlugin<?, ?, ?> channelPlugin;

    public ChannelController(ChannelPlugin<?, ?, ?> channelPlugin) {
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

        return Mono.fromCallable(() -> {
            ChannelMessage message = ChannelMessage.builder()
                    .text(request.text())
                    .from(request.from())
                    .fromName(request.fromName())
                    .chatId(request.chatId())
                    .messageId(request.messageId())
                    .metadata(request.metadata())
                    .build();

            Optional<ChannelOutboundAdapter> outboundAdapter = channelPlugin.getOutboundAdapter();
            if (outboundAdapter.isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("error", "Outbound adapter not available");
                return ResponseEntity.badRequest().body(error);
            }

            ChannelOutboundAdapter.SendOptions options = ChannelOutboundAdapter.SendOptions.builder()
                    .build();

            return outboundAdapter.get().sendText(null, request.chatId(), request.text(), Optional.of(options))
                    .thenApply(result -> {
                        Map<String, Object> response = new HashMap<>();
                        response.put("success", result.success());
                        response.put("messageId", result.messageId().orElse(null));
                        response.put("timestamp", System.currentTimeMillis());
                        return ResponseEntity.ok(response);
                    })
                    .get();
        }).onErrorResume(e -> {
            logger.error("Failed to send message", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return Mono.just(ResponseEntity.badRequest().body(error));
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

        return Mono.fromCallable(() -> {
            Optional<ChannelOutboundAdapter> outboundAdapter = channelPlugin.getOutboundAdapter();
            if (outboundAdapter.isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("error", "Outbound adapter not available");
                return ResponseEntity.badRequest().body(error);
            }

            return outboundAdapter.get().sendTyping(null, chatId)
                    .thenApply(v -> {
                        Map<String, Object> response = new HashMap<>();
                        response.put("success", true);
                        response.put("chatId", chatId);
                        return ResponseEntity.ok(response);
                    })
                    .get();
        }).onErrorResume(e -> {
            logger.error("Failed to send typing indicator", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return Mono.just(ResponseEntity.badRequest().body(error));
        });
    }

    /**
     * Get channel info
     */
    @GetMapping("/{channel}/info")
    public Mono<ResponseEntity<Map<String, Object>>> getChannelInfo(
            @PathVariable String channel) {

        return Mono.fromCallable(() -> {
            Map<String, Object> info = new HashMap<>();
            info.put("channel", channel);
            info.put("id", channelPlugin.getId().toString());
            info.put("name", channelPlugin.getMeta().name());
            info.put("available", true);
            return ResponseEntity.ok(info);
        });
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
