package openclaw.server.streaming;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility for chunking messages into smaller pieces for streaming.
 *
 * @author OpenClaw Team
 * @version 2026.3.13
 */
public class MessageChunker {
    
    private static final Logger logger = LoggerFactory.getLogger(MessageChunker.class);
    
    private final int chunkSize;
    private final Duration chunkInterval;
    
    public MessageChunker() {
        this(100, Duration.ofMillis(50));
    }
    
    public MessageChunker(int chunkSize, Duration chunkInterval) {
        this.chunkSize = chunkSize;
        this.chunkInterval = chunkInterval;
    }
    
    /**
     * Chunk a complete message into a stream.
     */
    public Flux<String> chunkMessage(String message) {
        return Flux.defer(() -> {
            List<String> chunks = splitIntoChunks(message, chunkSize);
            logger.debug("Chunked message into {} chunks", chunks.size());
            
            return Flux.fromIterable(chunks)
                .delayElements(chunkInterval);
        });
    }
    
    /**
     * Chunk a message with word boundaries.
     */
    public Flux<String> chunkMessageByWords(String message) {
        return Flux.defer(() -> {
            List<String> chunks = splitByWords(message, chunkSize);
            logger.debug("Chunked message into {} word chunks", chunks.size());
            
            return Flux.fromIterable(chunks)
                .delayElements(chunkInterval);
        });
    }
    
    /**
     * Chunk a message with sentence boundaries.
     */
    public Flux<String> chunkMessageBySentences(String message) {
        return Flux.defer(() -> {
            List<String> chunks = splitBySentences(message);
            logger.debug("Chunked message into {} sentence chunks", chunks.size());
            
            return Flux.fromIterable(chunks)
                .delayElements(chunkInterval);
        });
    }
    
    /**
     * Create a streaming Flux from a complete string with simulated typing.
     */
    public Flux<String> simulateTyping(String message, Duration typingSpeed) {
        return Flux.defer(() -> {
            List<String> chunks = splitIntoChunks(message, 1); // Character by character
            
            return Flux.fromIterable(chunks)
                .delayElements(typingSpeed);
        });
    }
    
    // Private helper methods
    
    private List<String> splitIntoChunks(String message, int size) {
        List<String> chunks = new ArrayList<>();
        for (int i = 0; i < message.length(); i += size) {
            chunks.add(message.substring(i, Math.min(i + size, message.length())));
        }
        return chunks;
    }
    
    private List<String> splitByWords(String message, int maxChunkSize) {
        List<String> chunks = new ArrayList<>();
        String[] words = message.split(" ");
        StringBuilder currentChunk = new StringBuilder();
        
        for (String word : words) {
            if (currentChunk.length() + word.length() + 1 > maxChunkSize) {
                chunks.add(currentChunk.toString().trim());
                currentChunk = new StringBuilder();
            }
            currentChunk.append(word).append(" ");
        }
        
        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString().trim());
        }
        
        return chunks;
    }
    
    private List<String> splitBySentences(String message) {
        // Simple sentence splitting by punctuation
        String[] sentences = message.split("(?<=[.!?])\\s+");
        List<String> chunks = new ArrayList<>();
        StringBuilder currentChunk = new StringBuilder();
        
        for (String sentence : sentences) {
            if (currentChunk.length() + sentence.length() > chunkSize * 2) {
                chunks.add(currentChunk.toString().trim());
                currentChunk = new StringBuilder();
            }
            currentChunk.append(sentence).append(" ");
        }
        
        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString().trim());
        }
        
        return chunks;
    }
    
    // Getters
    public int getChunkSize() { return chunkSize; }
    public Duration getChunkInterval() { return chunkInterval; }
}
