package openclaw.server.controller;

import openclaw.sdk.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Channel REST API Controller
 *
 * <p>Provides HTTP endpoints for channel operations.</p>
 */
@RestController
@RequestMapping("/api/v1/channels")
public class ChannelController {

    private static final Logger logger = LoggerFactory.getLogger(ChannelController.class);

    private final Map<ChannelId, ChannelPlugin<?, ?, ?>> channels;

    public ChannelController(List<ChannelPlugin<?, ?, ?>> channelPlugins) {
        this.channels = channelPlugins.stream()
                .collect(Collectors.toMap(ChannelPlugin::getId, p -> p));
    }

    /**
     * List all available channels
     */
    @GetMapping
    public Mono<List<ChannelInfo>> listChannels() {
        return Mono.just(channels.values().stream()
                .map(plugin -> new ChannelInfo(
                        plugin.getId().name(),
                        plugin.getMeta().name(),
                        plugin.getMeta().description(),
                        plugin.getCapabilities()
                ))
                .toList());
    }

    /**
     * Get channel details
     */
    @GetMapping("/{channelId}")
    public Mono<ChannelInfo> getChannel(@PathVariable String channelId) {
        ChannelPlugin<?, ?, ?> plugin = channels.get(ChannelId.valueOf(channelId.toUpperCase()));
        if (plugin == null) {
            return Mono.error(new ChannelNotFoundException("Channel not found: " + channelId));
        }

        return Mono.just(new ChannelInfo(
                plugin.getId().name(),
                plugin.getMeta().name(),
                plugin.getMeta().description(),
                plugin.getCapabilities()
        ));
    }

    /**
     * Send message to channel
     */
    @PostMapping("/{channelId}/send")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Mono<SendResponse> sendMessage(
            @PathVariable String channelId,
            @RequestBody SendRequest request) {

        ChannelPlugin<?, ?, ?> plugin = channels.get(ChannelId.valueOf(channelId.toUpperCase()));
        if (plugin == null) {
            return Mono.error(new ChannelNotFoundException("Channel not found: " + channelId));
        }

        Optional<ChannelOutboundAdapter> outbound = plugin.getOutboundAdapter();
        if (outbound.isEmpty()) {
            return Mono.error(new ChannelOperationException("Channel does not support outbound messages"));
        }

        logger.info("Sending message via {} to {}", channelId, request.target());

        // Create message
        ChannelMessage message = ChannelMessage.builder()
                .text(request.text())
                .target(request.target())
                .build();

        return Mono.fromFuture(outbound.get().send(message))
                .map(result -> new SendResponse(
                        result.success(),
                        result.messageId().orElse(null),
                        result.error().orElse(null)
                ));
    }

    /**
     * Get channel health status
     */
    @GetMapping("/{channelId}/health")
    public Mono<ChannelHealthResponse> getChannelHealth(@PathVariable String channelId) {
        ChannelPlugin<?, ?, ?> plugin = channels.get(ChannelId.valueOf(channelId.toUpperCase()));
        if (plugin == null) {
            return Mono.error(new ChannelNotFoundException("Channel not found: " + channelId));
        }

        // Simple health check
        return Mono.just(new ChannelHealthResponse(
                channelId,
                "UP",
                System.currentTimeMillis()
        ));
    }

    // Request/Response Records

    public record ChannelInfo(
            String id,
            String name,
            String description,
            ChannelCapabilities capabilities
    ) {}

    public record SendRequest(
            String target,
            String text,
            Map<String, Object> options
    ) {}

    public record SendResponse(
            boolean success,
            String messageId,
            String error
    ) {}

    public record ChannelHealthResponse(
            String channelId,
            String status,
            long timestamp
    ) {}

    // Exceptions

    @ResponseStatus(HttpStatus.NOT_FOUND)
    public static class ChannelNotFoundException extends RuntimeException {
        public ChannelNotFoundException(String message) {
            super(message);
        }
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public static class ChannelOperationException extends RuntimeException {
        public ChannelOperationException(String message) {
            super(message);
        }
    }
}
