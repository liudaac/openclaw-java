package openclaw.memory.vector;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Embedding Service - OpenAI Integration
 *
 * <p>Generates embeddings using OpenAI API.</p>
 */
@Service
public class EmbeddingService {

    private static final Logger logger = LoggerFactory.getLogger(EmbeddingService.class);

    @Value("${openai.api-key:}")
    private String apiKey;

    @Value("${openai.embedding.model:text-embedding-ada-002}")
    private String model;

    @Value("${openai.embedding.url:https://api.openai.com/v1/embeddings}")
    private String apiUrl;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public EmbeddingService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(30))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Generate embedding for single text
     */
    public CompletableFuture<float[]> embed(String text) {
        return embedBatch(List.of(text))
                .thenApply(embeddings -> embeddings.isEmpty() ? null : embeddings.get(0));
    }

    /**
     * Generate embeddings for batch of texts
     */
    public CompletableFuture<List<float[]>> embedBatch(List<String> texts) {
        return CompletableFuture.supplyAsync(() -> {
            if (apiKey == null || apiKey.isEmpty()) {
                logger.error("OpenAI API key not configured");
                return generateDummyEmbeddings(texts.size());
            }

            try {
                // Build request
                String requestBody = objectMapper.writeValueAsString(Map.of(
                        "model", model,
                        "input", texts
                ));

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(apiUrl))
                        .header("Authorization", "Bearer " + apiKey)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .build();

                HttpResponse<String> response = httpClient.send(request,
                        HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() != 200) {
                    logger.error("OpenAI API error: {}", response.body());
                    return generateDummyEmbeddings(texts.size());
                }

                // Parse response
                JsonNode root = objectMapper.readTree(response.body());
                JsonNode data = root.get("data");

                List<float[]> embeddings = new java.util.ArrayList<>();
                for (JsonNode item : data) {
                    JsonNode embeddingNode = item.get("embedding");
                    float[] embedding = new float[embeddingNode.size()];
                    for (int i = 0; i < embeddingNode.size(); i++) {
                        embedding[i] = (float) embeddingNode.get(i).asDouble();
                    }
                    embeddings.add(embedding);
                }

                logger.debug("Generated {} embeddings", embeddings.size());
                return embeddings;

            } catch (Exception e) {
                logger.error("Failed to generate embeddings: {}", e.getMessage());
                return generateDummyEmbeddings(texts.size());
            }
        });
    }

    /**
     * Generate dummy embeddings (fallback)
     */
    private List<float[]> generateDummyEmbeddings(int count) {
        List<float[]> embeddings = new java.util.ArrayList<>();
        int dimension = 1536;

        for (int i = 0; i < count; i++) {
            float[] embedding = new float[dimension];
            java.util.Random random = new java.util.Random(i);

            for (int j = 0; j < dimension; j++) {
                embedding[j] = (float) (random.nextGaussian() * 0.1);
            }

            // Normalize
            float norm = 0;
            for (float v : embedding) {
                norm += v * v;
            }
            norm = (float) Math.sqrt(norm);

            if (norm > 0) {
                for (int j = 0; j < dimension; j++) {
                    embedding[j] /= norm;
                }
            }

            embeddings.add(embedding);
        }

        return embeddings;
    }

    /**
     * Check if service is available
     */
    public boolean isAvailable() {
        return apiKey != null && !apiKey.isEmpty();
    }
}
