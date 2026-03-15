package openclaw.server.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

/**
 * LLM Service for OpenClaw
 *
 * <p>Provides LLM operations using Spring AI 1.1.2.</p>
 */
@Service
public class LlmService implements openclaw.sdk.llm.LlmService {

    private static final Logger logger = LoggerFactory.getLogger(LlmService.class);

    // Pattern to strip model control tokens from user-facing text
    private static final Pattern CONTROL_TOKEN_PATTERN = Pattern.compile(
        "<\\|im_(start|end)\\|>|" +
        "<\\|endoftext\\|>|" +
        "<\\|startoftext\\|>|" +
        "<\\|assistant\\|>|" +
        "<\\|user\\|>|" +
        "<\\|system\\|>|" +
        "<\\|tool\\|>|" +
        "<\\|end\\|>"
    );

    private final ChatClient chatClient;

    public LlmService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public CompletableFuture<String> chat(String message) {
        return CompletableFuture.supplyAsync(() -> {
            logger.debug("Sending chat request: {}", message.substring(0, Math.min(100, message.length())));
            String content = chatClient.prompt(message).call().content();
            return filterControlTokens(content);
        });
    }

    @Override
    public CompletableFuture<String> chat(String systemPrompt, String userMessage) {
        return CompletableFuture.supplyAsync(() -> {
            String content = chatClient.prompt()
                .system(systemPrompt)
                .user(userMessage)
                .call()
                .content();
            return filterControlTokens(content);
        });
    }

    @Override
    public CompletableFuture<String> complete(String prompt) {
        return chat(prompt);
    }

    @Override
    public CompletableFuture<String> chatStream(String message) {
        // For streaming, we collect all chunks into a single string
        return CompletableFuture.supplyAsync(() -> {
            StringBuilder sb = new StringBuilder();
            chatClient.prompt(message).stream().content()
                .doOnNext(sb::append)
                .blockLast();
            return filterControlTokens(sb.toString());
        });
    }

    public Flux<String> streamChat(String message) {
        return chatClient.prompt(message).stream().content()
            .map(this::filterControlTokens)
            .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public CompletableFuture<float[]> embed(String text) {
        // Embedding not implemented in this version
        return CompletableFuture.completedFuture(new float[0]);
    }

    @Override
    public CompletableFuture<float[][]> embedBatch(String[] texts) {
        // Embedding not implemented in this version
        return CompletableFuture.completedFuture(new float[texts.length][0]);
    }

    @Override
    public boolean isAvailable() {
        try {
            chatClient.prompt("Hello").call().content();
            return true;
        } catch (Exception e) {
            logger.warn("LLM service not available: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public Map<String, Object> getModelInfo() {
        return Map.of(
            "provider", "openai",
            "model", "gpt-4",
            "version", "2026.3.9"
        );
    }

    private String filterControlTokens(String text) {
        if (text == null) return "";
        return CONTROL_TOKEN_PATTERN.matcher(text).replaceAll("").trim();
    }
}
