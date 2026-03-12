package openclaw.memory.embedding;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Embedding provider interface for text vectorization.
 *
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public interface EmbeddingProvider {

    /**
     * Gets the provider ID.
     *
     * @return the provider ID
     */
    String getId();

    /**
     * Gets the provider name.
     *
     * @return the name
     */
    String getName();

    /**
     * Gets the default model.
     *
     * @return the default model
     */
    String getDefaultModel();

    /**
     * Gets the embedding dimension.
     *
     * @return the dimension
     */
    int getDimension();

    /**
     * Embeds a single text.
     *
     * @param text the text to embed
     * @return the embedding vector
     */
    CompletableFuture<EmbeddingVector> embed(String text);

    /**
     * Embeds multiple texts.
     *
     * @param texts the texts to embed
     * @return the embedding vectors
     */
    CompletableFuture<List<EmbeddingVector>> embedBatch(List<String> texts);

    /**
     * Checks if the provider is available.
     *
     * @return true if available
     */
    CompletableFuture<Boolean> isAvailable();

    /**
     * Embedding vector result.
     *
     * @param text the original text
     * @param vector the embedding vector
     * @param model the model used
     * @param provider the provider ID
     */
    record EmbeddingVector(
            String text,
            float[] vector,
            String model,
            String provider
    ) {

        /**
         * Gets the vector dimension.
         *
         * @return the dimension
         */
        public int dimension() {
            return vector.length;
        }
    }
}
