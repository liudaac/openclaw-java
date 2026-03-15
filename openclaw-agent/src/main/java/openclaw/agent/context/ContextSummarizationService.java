package openclaw.agent.context;

import openclaw.agent.token.TokenCounterService;
import openclaw.sdk.llm.LlmService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Context Summarization Service
 * 
 * <p>Automatically compresses long conversation history to reduce token usage.
 * Implements sliding window + LLM summarization strategy.</p>
 */
@Service
public class ContextSummarizationService {
    
    private static final Logger logger = LoggerFactory.getLogger(ContextSummarizationService.class);
    
    private final LlmService llmService;
    private final TokenCounterService tokenCounter;
    
    // Configuration
    private static final int DEFAULT_TOKEN_THRESHOLD = 3000;
    private static final int SUMMARY_TARGET_TOKENS = 500;
    private static final int RECENT_MESSAGES_TO_KEEP = 4;
    private static final int MIN_MESSAGES_FOR_SUMMARY = 6;
    
    public ContextSummarizationService(LlmService llmService, TokenCounterService tokenCounter) {
        this.llmService = llmService;
        this.tokenCounter = tokenCounter;
    }
    
    /**
     * Compress messages if they exceed token threshold
     * 
     * @param messages the conversation messages
     * @param model the model name
     * @param tokenThreshold the token threshold (default: 3000)
     * @return compressed messages
     */
    public CompletableFuture<List<Message>> compressIfNeeded(
            List<Message> messages, 
            String model,
            int tokenThreshold) {
        
        return CompletableFuture.supplyAsync(() -> {
            if (messages == null || messages.size() < MIN_MESSAGES_FOR_SUMMARY) {
                return messages;
            }
            
            // Count total tokens
            int totalTokens = countMessageTokens(messages, model);
            
            if (totalTokens <= tokenThreshold) {
                logger.debug("Messages within token limit: {}/{} tokens", totalTokens, tokenThreshold);
                return messages;
            }
            
            logger.info("Compressing {} messages ({} tokens) for model {}", 
                messages.size(), totalTokens, model);
            
            // Compress messages
            return compressMessages(messages, model, tokenThreshold);
        });
    }
    
    /**
     * Compress messages with default threshold
     */
    public CompletableFuture<List<Message>> compressIfNeeded(List<Message> messages, String model) {
        return compressIfNeeded(messages, model, DEFAULT_TOKEN_THRESHOLD);
    }
    
    /**
     * Compress messages using sliding window + summarization
     */
    private List<Message> compressMessages(List<Message> messages, String model, int tokenThreshold) {
        // Strategy: Keep recent messages + summarize older ones
        
        int totalMessages = messages.size();
        int messagesToKeep = Math.min(RECENT_MESSAGES_TO_KEEP, totalMessages);
        int messagesToSummarize = totalMessages - messagesToKeep;
        
        if (messagesToSummarize < MIN_MESSAGES_FOR_SUMMARY) {
            // Not enough messages to summarize, just truncate
            return messages.subList(totalMessages - messagesToKeep, totalMessages);
        }
        
        // Split messages
        List<Message> olderMessages = messages.subList(0, messagesToSummarize);
        List<Message> recentMessages = messages.subList(messagesToSummarize, totalMessages);
        
        // Generate summary of older messages
        String summary = generateSummary(olderMessages).join();
        
        // Create summary message
        Message summaryMessage = new Message(
            "system",
            "[Earlier conversation summary]: " + summary,
            System.currentTimeMillis()
        );
        
        // Combine: Summary + Recent messages
        List<Message> compressed = new ArrayList<>();
        compressed.add(summaryMessage);
        compressed.addAll(recentMessages);
        
        // Verify compression result
        int newTokenCount = countMessageTokens(compressed, model);
        logger.info("Compressed from {} to {} messages ({} to {} tokens)",
            totalMessages, compressed.size(), 
            countMessageTokens(messages, model), newTokenCount);
        
        return compressed;
    }
    
    /**
     * Generate summary of messages using LLM
     */
    private CompletableFuture<String> generateSummary(List<Message> messages) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Build summary prompt
                StringBuilder prompt = new StringBuilder();
                prompt.append("Summarize the following conversation in a concise paragraph. ");
                prompt.append("Focus on key facts, decisions, and context that would be relevant for continuing the conversation.\n\n");
                prompt.append("Conversation:\n");
                
                for (Message msg : messages) {
                    prompt.append(msg.role()).append(": ").append(msg.content()).append("\n");
                }
                
                prompt.append("\nSummary:");
                
                // Call LLM
                String summary = llmService.chat(prompt.toString()).join();
                
                // Clean up summary
                summary = summary.trim();
                if (summary.length() > 500) {
                    summary = summary.substring(0, 500) + "...";
                }
                
                return summary;
                
            } catch (Exception e) {
                logger.error("Failed to generate summary, using fallback", e);
                // Fallback: simple concatenation of key points
                return generateFallbackSummary(messages);
            }
        });
    }
    
    /**
     * Generate fallback summary when LLM fails
     */
    private String generateFallbackSummary(List<Message> messages) {
        StringBuilder sb = new StringBuilder();
        sb.append("Previous conversation covered: ");
        
        // Extract key points from messages
        List<String> keyPoints = messages.stream()
            .filter(m -> m.role().equals("user"))
            .map(m -> m.content().substring(0, Math.min(50, m.content().length())))
            .limit(3)
            .collect(Collectors.toList());
        
        sb.append(String.join("; ", keyPoints));
        return sb.toString();
    }
    
    /**
     * Count tokens in messages
     */
    private int countMessageTokens(List<Message> messages, String model) {
        int total = 0;
        for (Message msg : messages) {
            total += tokenCounter.countMessageTokens(msg.role(), msg.content(), model);
        }
        return total;
    }
    
    /**
     * Estimate how many messages can fit in token budget
     */
    public int estimateMaxMessages(List<Message> messages, String model, int tokenBudget) {
        if (messages.isEmpty()) {
            return 0;
        }
        
        int avgTokensPerMessage = countMessageTokens(messages, model) / messages.size();
        if (avgTokensPerMessage == 0) {
            avgTokensPerMessage = 50; // Default estimate
        }
        
        return tokenBudget / avgTokensPerMessage;
    }
    
    /**
     * Message record
     */
    public record Message(
        String role,
        String content,
        long timestamp
    ) {}
    
    /**
     * Compression result
     */
    public record CompressionResult(
        List<Message> compressedMessages,
        int originalTokens,
        int compressedTokens,
        double compressionRatio,
        boolean wasCompressed
    ) {}
}
