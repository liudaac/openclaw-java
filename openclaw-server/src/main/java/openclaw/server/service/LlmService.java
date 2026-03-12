package openclaw.server.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * LLM Service for OpenClaw
 *
 * <p>Provides LLM operations using Spring AI.</p>
 */
@Service
public class LlmService {

    private static final Logger logger = LoggerFactory.getLogger(LlmService.class);

    // Pattern to strip model control tokens from user-facing text
    // Matches: ChatML format, GPT format, and other common control tokens
    private static final Pattern CONTROL_TOKEN_PATTERN = Pattern.compile(
        "<\\|im_(start|end)\\|>|" +           // ChatML format: <|im_start|>, <|im_end|>
        "<\\|endoftext\\|>|" +                // GPT format: <|endoftext|>
        "<\\|startoftext\\|>|" +              // GPT format: <|startoftext|>
        "<\\|assistant\\|>|" +                 // Assistant marker
        "<\\|user\\|>|" +                      // User marker
        "<\\|system\\|>|" +                    // System marker
        "<\\|tool\\|>|" +                      // Tool marker
        "<\\|end\\|>"                          // Generic end marker
    );

    private final ChatClient chatClient;

    public LlmService(OpenAiChatClient chatClient) {
        this.chatClient = chatClient;
    }

    /**
     * Generate a chat completion
     */
    public CompletableFuture<String> chat(String message) {
        return CompletableFuture.supplyAsync(() -> {
            logger.debug("Sending chat request: {}", message.substring(0, Math.min(100, message.length())));
            ChatResponse response = chatClient.call(new Prompt(message));
            String content = response.getResult().getOutput().getContent();
            // Strip leaked model control tokens from user-facing text
            return filterControlTokens(content);
        }, Schedulers.boundedElastic().toExecutor());
    }

    /**
     * Generate a chat completion with system prompt
     */
    public CompletableFuture<String> chat(String systemPrompt, String userMessage) {
        return CompletableFuture.supplyAsync(() -> {
            String fullPrompt = String.format("%s\n\nUser: %s\nAssistant:", systemPrompt, userMessage);
            ChatResponse response = chatClient.call(new Prompt(fullPrompt));
            String content = response.getResult().getOutput().getContent();
            // Strip leaked model control tokens from user-facing text
            return filterControlTokens(content);
        }, Schedulers.boundedElastic().toExecutor());
    }

    /**
     * Stream chat completion
     */
    public Flux<String> streamChat(String message) {
        return Flux.defer(() -> {
            logger.debug("Streaming chat request");
            return chatClient.stream(new Prompt(message))
                    .map(response -> {
                        String content = response.getResult().getOutput().getContent();
                        // Strip leaked model control tokens from user-facing text
                        return filterControlTokens(content);
                    });
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Check if LLM service is available
     */
    public CompletableFuture<Boolean> isAvailable() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                chatClient.call(new Prompt("Hello"));
                return true;
            } catch (Exception e) {
                logger.warn("LLM service not available: {}", e.getMessage());
                return false;
            }
        }, Schedulers.boundedElastic().toExecutor());
    }

    /**
     * Get model info
     */
    public Map<String, Object> getModelInfo() {
        return Map.of(
                "provider", "openai",
                "model", "gpt-4",
                "version", "2026.3.9"
        );
    }

    /**
     * Filter out model control tokens from user-facing text.
     * This prevents leaked control tokens (like <|im_start|>, <|endoftext|>, etc.)
     * from being displayed to users.
     *
     * @param text the raw LLM response
     * @return the filtered text with control tokens removed
     */
    private String filterControlTokens(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        String filtered = CONTROL_TOKEN_PATTERN.matcher(text).replaceAll("");
        // Also trim any leading/trailing whitespace that might be left
        return filtered.trim();
    }
}
