package openclaw.server.streaming;

import openclaw.plugin.sdk.channel.ChannelStreamingAdapter;
import openclaw.plugin.sdk.channel.ChannelStreamingAdapter.StreamChunk;
import openclaw.plugin.sdk.channel.ChannelStreamingAdapter.StreamingConfig;
import openclaw.plugin.sdk.channel.ChannelStreamingAdapter.StreamingMessageRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Streaming message service for real-time response streaming.
 *
 * @author OpenClaw Team
 * @version 2026.3.13
 */
@Service
public class StreamingMessageService {
    
    private static final Logger logger = LoggerFactory.getLogger(StreamingMessageService.class);
    
    private final Map<String, StreamingContext> activeStreams;
    private final StreamingConfig defaultConfig;
    
    public StreamingMessageService() {
        this.activeStreams = new ConcurrentHashMap<>();
        this.defaultConfig = new StreamingConfig();
    }
    
    /**
     * Create a streaming response.
     */
    public Flux<StreamChunk> createStreamingResponse(
            String chatId,
            Flux<String> contentStream,
            ChannelStreamingAdapter adapter) {
        
        String messageId = UUID.randomUUID().toString();
        logger.info("Creating streaming response: {} for chat: {}", messageId, chatId);
        
        // Create streaming request
        StreamingMessageRequest request = new StreamingMessageRequest(
            chatId,
            contentStream,
            Map.of("messageId", messageId),
            defaultConfig.getDefaultTimeout()
        );
        
        // Create streaming context
        StreamingContext context = new StreamingContext(messageId, adapter, defaultConfig);
        activeStreams.put(messageId, context);
        
        // Process content stream
        return processContentStream(request, context)
            .doOnComplete(() -> {
                activeStreams.remove(messageId);
                logger.debug("Streaming completed: {}", messageId);
            })
            .doOnError(error -> {
                activeStreams.remove(messageId);
                logger.error("Streaming error: {}", messageId, error);
            });
    }
    
    /**
     * Create streaming response with typing indicator.
     */
    public Flux<StreamChunk> createStreamingResponseWithTyping(
            String chatId,
            Flux<String> contentStream,
            ChannelStreamingAdapter adapter) {
        
        String messageId = UUID.randomUUID().toString();
        
        // Send typing indicator
        if (defaultConfig.isEnableTypingIndicator()) {
            adapter.sendTypingIndicator(chatId, defaultConfig.getTypingIndicatorInterval())
                .subscribe();
        }
        
        return createStreamingResponse(chatId, contentStream, adapter);
    }
    
    /**
     * Cancel an active stream.
     */
    public Mono<Void> cancelStream(String messageId, String reason) {
        StreamingContext context = activeStreams.get(messageId);
        if (context == null) {
            return Mono.empty();
        }
        
        logger.info("Cancelling stream: {} - {}", messageId, reason);
        context.cancel();
        activeStreams.remove(messageId);
        
        return context.getAdapter().cancelStreamingMessage(messageId, reason);
    }
    
    /**
     * Check if stream is active.
     */
    public boolean isStreamActive(String messageId) {
        return activeStreams.containsKey(messageId);
    }
    
    /**
     * Get active stream count.
     */
    public int getActiveStreamCount() {
        return activeStreams.size();
    }
    
    /**
     * Get default streaming config.
     */
    public StreamingConfig getDefaultConfig() {
        return defaultConfig;
    }
    
    // Private methods
    
    private Flux<StreamChunk> processContentStream(
            StreamingMessageRequest request,
            StreamingContext context) {
        
        AtomicLong sequence = new AtomicLong(0);
        StringBuilder buffer = new StringBuilder();
        String messageId = context.getMessageId();
        
        return request.getContentStream()
            // Buffer content into chunks
            .bufferTimeout(
                context.getConfig().getChunkSize(),
                context.getConfig().getChunkInterval()
            )
            // Process each buffer
            .flatMap(chunks -> {
                if (context.isCancelled()) {
                    return Flux.empty();
                }
                
                // Combine chunks
                String content = String.join("", chunks);
                buffer.append(content);
                
                // Create stream chunk
                long seq = sequence.incrementAndGet();
                StreamChunk chunk = StreamChunk.intermediate(messageId, buffer.toString(), seq);
                
                // Update message via adapter
                return context.getAdapter()
                    .updateStreamingMessage(messageId, buffer.toString())
                    .thenReturn(chunk);
            })
            // Add final chunk
            .concatWith(Mono.defer(() -> {
                long seq = sequence.incrementAndGet();
                StreamChunk finalChunk = StreamChunk.finalChunk(
                    messageId, buffer.toString(), seq
                );
                
                return context.getAdapter()
                    .finalizeStreamingMessage(messageId, buffer.toString())
                    .thenReturn(finalChunk);
            }))
            // Handle errors
            .onErrorResume(error -> {
                logger.error("Error in streaming: {}", messageId, error);
                return context.getAdapter()
                    .cancelStreamingMessage(messageId, error.getMessage())
                    .then(Mono.empty());
            })
            // Run on bounded elastic scheduler
            .subscribeOn(Schedulers.boundedElastic());
    }
    
    /**
     * Streaming context.
     */
    private static class StreamingContext {
        private final String messageId;
        private final ChannelStreamingAdapter adapter;
        private final StreamingConfig config;
        private volatile boolean cancelled;
        
        StreamingContext(String messageId, ChannelStreamingAdapter adapter, StreamingConfig config) {
            this.messageId = messageId;
            this.adapter = adapter;
            this.config = config;
            this.cancelled = false;
        }
        
        String getMessageId() { return messageId; }
        ChannelStreamingAdapter getAdapter() { return adapter; }
        StreamingConfig getConfig() { return config; }
        boolean isCancelled() { return cancelled; }
        void cancel() { this.cancelled = true; }
    }
}
