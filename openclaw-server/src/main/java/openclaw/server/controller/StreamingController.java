package openclaw.server.controller;

import openclaw.plugin.sdk.channel.ChannelStreamingAdapter;
import openclaw.plugin.sdk.channel.ChannelStreamingAdapter.StreamChunk;
import openclaw.server.streaming.MessageChunker;
import openclaw.server.streaming.StreamingMessageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

/**
 * REST API for streaming messages.
 *
 * @author OpenClaw Team
 * @version 2026.3.13
 */
@RestController
@RequestMapping("/api/v1/streaming")
public class StreamingController {
    
    private static final Logger logger = LoggerFactory.getLogger(StreamingController.class);
    
    private final StreamingMessageService streamingService;
    private final MessageChunker messageChunker;
    
    public StreamingController(StreamingMessageService streamingService) {
        this.streamingService = streamingService;
        this.messageChunker = new MessageChunker();
    }
    
    /**
     * Stream a message (Server-Sent Events).
     * 
     * POST /api/v1/streaming/send
     */
    @PostMapping(value = "/send", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<StreamChunk> streamMessage(
            @RequestBody StreamRequest request,
            @RequestHeader("X-Channel-Adapter") String adapterType) {
        
        logger.info("Streaming message to chat: {}", request.chatId());
        
        // Chunk the message
        Flux<String> contentStream = messageChunker.chunkMessageByWords(request.content());
        
        // Get adapter (simplified - should lookup from registry)
        ChannelStreamingAdapter adapter = getAdapter(adapterType);
        
        // Create streaming response
        return streamingService.createStreamingResponseWithTyping(
            request.chatId(),
            contentStream,
            adapter
        );
    }
    
    /**
     * Cancel an active stream.
     * 
     * POST /api/v1/streaming/cancel/{messageId}
     */
    @PostMapping("/cancel/{messageId}")
    public Mono<Map<String, Object>> cancelStream(
            @PathVariable String messageId,
            @RequestParam(defaultValue = "User cancelled") String reason) {
        
        logger.info("Cancelling stream: {}", messageId);
        
        return streamingService.cancelStream(messageId, reason)
            .thenReturn(Map.of(
                "messageId", messageId,
                "status", "cancelled",
                "reason", reason
            ));
    }
    
    /**
     * Get streaming status.
     * 
     * GET /api/v1/streaming/status
     */
    @GetMapping("/status")
    public Mono<Map<String, Object>> getStatus() {
        return Mono.just(Map.of(
            "activeStreams", streamingService.getActiveStreamCount(),
            "chunkSize", messageChunker.getChunkSize(),
            "chunkIntervalMs", messageChunker.getChunkInterval().toMillis()
        ));
    }
    
    /**
     * Test streaming endpoint.
     * 
     * GET /api/v1/streaming/test?message=Hello&delay=100
     */
    @GetMapping(value = "/test", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> testStreaming(
            @RequestParam(defaultValue = "Hello, this is a test message!") String message,
            @RequestParam(defaultValue = "100") long delayMs) {
        
        logger.debug("Test streaming with delay: {}ms", delayMs);
        
        return messageChunker.simulateTyping(message, Duration.ofMillis(delayMs));
    }
    
    // Helper method
    private ChannelStreamingAdapter getAdapter(String type) {
        // Simplified - should lookup from a registry
        // For now, return a mock adapter
        return new ChannelStreamingAdapter() {
            @Override
            public boolean supportsStreaming() { return true; }
            
            @Override
            public Flux<StreamChunk> sendStreamingMessage(StreamingMessageRequest request) {
                return Flux.empty();
            }
            
            @Override
            public Mono<Void> sendTypingIndicator(String chatId, Duration duration) {
                return Mono.empty();
            }
            
            @Override
            public Mono<Void> updateStreamingMessage(String messageId, String content) {
                return Mono.empty();
            }
            
            @Override
            public Mono<Void> finalizeStreamingMessage(String messageId, String finalContent) {
                return Mono.empty();
            }
            
            @Override
            public Mono<Void> cancelStreamingMessage(String messageId, String reason) {
                return Mono.empty();
            }
            
            @Override
            public StreamingConfig getStreamingConfig() {
                return new StreamingConfig();
            }
        };
    }
    
    // Request/Response records
    public record StreamRequest(
        String chatId,
        String content,
        String replyToMessageId,
        Map<String, Object> metadata
    ) {}
}
