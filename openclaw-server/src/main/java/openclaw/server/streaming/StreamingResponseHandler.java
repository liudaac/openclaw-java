package openclaw.server.streaming;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Enhanced Streaming Response Handler
 * 
 * <p>Provides optimized streaming with:</p>
 * <ul>
 *   <li>Cancellation support</li>
 *   <li>Backpressure control</li>
 *   <li>Error handling</li>
 *   <li>Flow control</li>
 * </ul>
 */
@Service
public class StreamingResponseHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(StreamingResponseHandler.class);
    
    // Active streams
    private final Map<String, StreamContext> activeStreams;
    
    // Configuration
    private static final int DEFAULT_BUFFER_SIZE = 100;
    private static final Duration DEFAULT_TIMEOUT = Duration.ofMinutes(5);
    private static final long DEFAULT_BACKPRESSURE_THRESHOLD = 1000; // 1 second worth of tokens
    
    public StreamingResponseHandler() {
        this.activeStreams = new ConcurrentHashMap<>();
    }
    
    /**
     * Create a new streaming response
     * 
     * @param streamId unique stream identifier
     * @return flux of string chunks
     */
    public Flux<String> createStream(String streamId) {
        return createStream(streamId, DEFAULT_BUFFER_SIZE, DEFAULT_TIMEOUT);
    }
    
    /**
     * Create a new streaming response with custom configuration
     * 
     * @param streamId unique stream identifier
     * @param bufferSize buffer size for backpressure
     * @param timeout stream timeout
     * @return flux of string chunks
     */
    public Flux<String> createStream(String streamId, int bufferSize, Duration timeout) {
        logger.debug("Creating stream: {} with buffer size: {}", streamId, bufferSize);
        
        // Create sink with backpressure buffer
        Sinks.Many<String> sink = Sinks.many().multicast()
            .onBackpressureBuffer(bufferSize, false);
        
        // Create stream context
        StreamContext context = new StreamContext(
            streamId,
            sink,
            new AtomicBoolean(false),
            new AtomicLong(0),
            System.currentTimeMillis()
        );
        
        activeStreams.put(streamId, context);
        
        return sink.asFlux()
            .doOnSubscribe(subscription -> {
                logger.debug("Stream {} subscribed", streamId);
            })
            .doOnNext(chunk -> {
                context.incrementChunkCount();
                logger.trace("Stream {} emitted chunk: {}", streamId, chunk.length());
            })
            .doOnError(error -> {
                logger.error("Stream {} error: {}", streamId, error.getMessage());
                cleanupStream(streamId);
            })
            .doOnComplete(() -> {
                logger.debug("Stream {} completed", streamId);
                cleanupStream(streamId);
            })
            .doOnCancel(() -> {
                logger.debug("Stream {} cancelled", streamId);
                cleanupStream(streamId);
            })
            .timeout(timeout, Flux.error(new StreamTimeoutException("Stream timeout after " + timeout)))
            .onBackpressureBuffer(bufferSize, 
                chunk -> logger.warn("Stream {} backpressure buffer full", streamId))
            .subscribeOn(Schedulers.boundedElastic());
    }
    
    /**
     * Emit a chunk to the stream
     * 
     * @param streamId stream identifier
     * @param chunk text chunk
     * @return true if emitted successfully
     */
    public boolean emitChunk(String streamId, String chunk) {
        StreamContext context = activeStreams.get(streamId);
        
        if (context == null) {
            logger.warn("Stream {} not found, cannot emit chunk", streamId);
            return false;
        }
        
        if (context.isCancelled()) {
            logger.debug("Stream {} is cancelled, dropping chunk", streamId);
            return false;
        }
        
        Sinks.EmitResult result = context.sink().tryEmitNext(chunk);
        
        if (result.isSuccess()) {
            return true;
        } else {
            logger.warn("Failed to emit chunk to stream {}: {}", streamId, result);
            return false;
        }
    }
    
    /**
     * Complete the stream
     * 
     * @param streamId stream identifier
     */
    public void completeStream(String streamId) {
        StreamContext context = activeStreams.get(streamId);
        
        if (context != null && !context.isCancelled()) {
            context.sink().tryEmitComplete();
            cleanupStream(streamId);
        }
    }
    
    /**
     * Cancel the stream
     * 
     * @param streamId stream identifier
     */
    public void cancelStream(String streamId) {
        logger.info("Cancelling stream: {}", streamId);
        
        StreamContext context = activeStreams.get(streamId);
        
        if (context != null) {
            context.cancel();
            context.sink().tryEmitComplete();
            cleanupStream(streamId);
        }
    }
    
    /**
     * Check if stream is active
     */
    public boolean isStreamActive(String streamId) {
        StreamContext context = activeStreams.get(streamId);
        return context != null && !context.isCancelled();
    }
    
    /**
     * Get stream statistics
     */
    public StreamStats getStreamStats(String streamId) {
        StreamContext context = activeStreams.get(streamId);
        
        if (context == null) {
            return null;
        }
        
        return new StreamStats(
            streamId,
            context.chunkCount(),
            context.isCancelled(),
            System.currentTimeMillis() - context.startTime()
        );
    }
    
    /**
     * Get all active stream stats
     */
    public Map<String, StreamStats> getAllStreamStats() {
        Map<String, StreamStats> stats = new ConcurrentHashMap<>();
        
        activeStreams.forEach((id, context) -> {
            stats.put(id, new StreamStats(
                id,
                context.chunkCount(),
                context.isCancelled(),
                System.currentTimeMillis() - context.startTime()
            ));
        });
        
        return stats;
    }
    
    /**
     * Apply flow control - pause if consumer is slow
     */
    public boolean shouldPause(String streamId) {
        StreamContext context = activeStreams.get(streamId);
        
        if (context == null) {
            return false;
        }
        
        // Simple flow control: pause if too many chunks emitted recently
        long recentChunks = context.chunkCount() - context.lastCheckpoint();
        return recentChunks > DEFAULT_BACKPRESSURE_THRESHOLD;
    }
    
    /**
     * Checkpoint for flow control
     */
    public void checkpoint(String streamId) {
        StreamContext context = activeStreams.get(streamId);
        
        if (context != null) {
            context.setCheckpoint();
        }
    }
    
    /**
     * Cleanup stream resources
     */
    private void cleanupStream(String streamId) {
        StreamContext context = activeStreams.remove(streamId);
        
        if (context != null) {
            logger.debug("Cleaned up stream: {} ({} chunks, {}ms)",
                streamId, context.chunkCount(), 
                System.currentTimeMillis() - context.startTime());
        }
    }
    
    /**
     * Stream context
     */
    private static class StreamContext {
        private final String streamId;
        private final Sinks.Many<String> sink;
        private final AtomicBoolean cancelled;
        private final AtomicLong chunkCount;
        private final long startTime;
        private volatile long lastCheckpoint;
        
        StreamContext(String streamId, Sinks.Many<String> sink, 
                     AtomicBoolean cancelled, AtomicLong chunkCount, long startTime) {
            this.streamId = streamId;
            this.sink = sink;
            this.cancelled = cancelled;
            this.chunkCount = chunkCount;
            this.startTime = startTime;
            this.lastCheckpoint = 0;
        }
        
        String streamId() { return streamId; }
        Sinks.Many<String> sink() { return sink; }
        AtomicBoolean cancelled() { return cancelled; }
        AtomicLong chunkCount() { return chunkCount; }
        long startTime() { return startTime; }
        long lastCheckpoint() { return lastCheckpoint; }
        
        boolean isCancelled() { return cancelled.get(); }
        void cancel() { cancelled.set(true); }
        void incrementChunkCount() { chunkCount.incrementAndGet(); }
        void setCheckpoint() { lastCheckpoint = chunkCount.get(); }
    }
    
    /**
     * Stream statistics
     */
    public record StreamStats(
        String streamId,
        long chunkCount,
        boolean cancelled,
        long durationMs
    ) {}
    
    /**
     * Stream timeout exception
     */
    public static class StreamTimeoutException extends RuntimeException {
        public StreamTimeoutException(String message) {
            super(message);
        }
    }
}
