package openclaw.sdk.llm;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * LLM Service interface for OpenClaw.
 *
 * <p>Provides LLM operations abstraction.</p>
 *
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public interface LlmService {

    /**
     * Generate a chat completion
     *
     * @param message the input message
     * @return future with the response
     */
    CompletableFuture<String> chat(String message);

    /**
     * Generate a chat completion with system prompt
     *
     * @param systemPrompt the system prompt
     * @param userMessage the user message
     * @return future with the response
     */
    CompletableFuture<String> chat(String systemPrompt, String userMessage);

    /**
     * Generate a streaming chat completion
     *
     * @param message the input message
     * @return future with the response stream
     */
    CompletableFuture<String> chatStream(String message);

    /**
     * Generate a completion
     *
     * @param prompt the prompt
     * @return future with the response
     */
    CompletableFuture<String> complete(String prompt);

    /**
     * Generate embeddings for text
     *
     * @param text the text to embed
     * @return future with the embedding
     */
    CompletableFuture<float[]> embed(String text);

    /**
     * Generate embeddings for multiple texts
     *
     * @param texts the texts to embed
     * @return future with the embeddings
     */
    CompletableFuture<float[][]> embedBatch(String[] texts);

    /**
     * Summarize text
     *
     * @param text the text to summarize
     * @param maxLength maximum length of summary
     * @return future with the summary
     */
    default CompletableFuture<String> summarize(String text, int maxLength) {
        String prompt = String.format(
            "Please summarize the following text in no more than %d words:\n\n%s",
            maxLength, text
        );
        return complete(prompt);
    }

    /**
     * Check if the service is available
     *
     * @return true if available
     */
    boolean isAvailable();

    /**
     * Get model information
     *
     * @return model info
     */
    Map<String, Object> getModelInfo();
}
