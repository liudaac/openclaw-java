package openclaw.memory.embedding;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Ollama local embedding provider implementation.
 *
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public class OllamaEmbeddingProvider implements EmbeddingProvider {

    private static final String DEFAULT_MODEL = "nomic-embed-text";
    private static final int DEFAULT_DIMENSION = 768;
    private static final String DEFAULT_BASE_URL = "http://localhost:11434";

    private final String baseUrl;
    private final String model;
    private final int dimension;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public OllamaEmbeddingProvider() {
        this(DEFAULT_BASE_URL, DEFAULT_MODEL, DEFAULT_DIMENSION);
    }

    public OllamaEmbeddingProvider(String model) {
        this(DEFAULT_BASE_URL, model, DEFAULT_DIMENSION);
    }

    public OllamaEmbeddingProvider(String baseUrl, String model, int dimension) {
        this.baseUrl = baseUrl != null ? baseUrl : DEFAULT_BASE_URL;
        this.model = model != null ? model : DEFAULT_MODEL;
        this.dimension = dimension;
        this.httpClient = new OkHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String getId() {
        return "ollama";
    }

    @Override
    public String getName() {
        return "Ollama";
    }

    @Override
    public String getDefaultModel() {
        return DEFAULT_MODEL;
    }

    @Override
    public int getDimension() {
        return dimension;
    }

    @Override
    public CompletableFuture<EmbeddingVector> embed(String text) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return doEmbed(text);
            } catch (Exception e) {
                throw new EmbeddingException("Failed to embed with Ollama", e);
            }
        });
    }

    @Override
    public CompletableFuture<List<EmbeddingVector>> embedBatch(List<String> texts) {
        // Ollama doesn't support batch embedding natively, so we do it sequentially
        return CompletableFuture.supplyAsync(() -> {
            List<EmbeddingVector> results = new java.util.ArrayList<>();
            for (String text : texts) {
                try {
                    results.add(doEmbed(text));
                } catch (Exception e) {
                    throw new EmbeddingException("Failed to embed text: " + text.substring(0, Math.min(50, text.length())), e);
                }
            }
            return results;
        });
    }

    private EmbeddingVector doEmbed(String text) throws IOException {
        String jsonBody = objectMapper.writeValueAsString(new EmbeddingRequest(model, text));

        Request request = new Request.Builder()
                .url(baseUrl + "/api/embeddings")
                .header("Content-Type", "application/json")
                .post(RequestBody.create(jsonBody, MediaType.parse("application/json")))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new EmbeddingException("Ollama API error: " + response.code() + " " + response.message());
            }

            JsonNode root = objectMapper.readTree(response.body().string());
            JsonNode embedding = root.get("embedding");

            float[] vector = new float[embedding.size()];
            for (int i = 0; i < embedding.size(); i++) {
                vector[i] = (float) embedding.get(i).asDouble();
            }

            return new EmbeddingVector(text, vector, model, getId());
        }
    }

    @Override
    public CompletableFuture<Boolean> isAvailable() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Request request = new Request.Builder()
                        .url(baseUrl + "/api/tags")
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    return response.isSuccessful();
                }
            } catch (Exception e) {
                return false;
            }
        });
    }

    /**
     * Embedding request payload.
     */
    private record EmbeddingRequest(String model, String prompt) {
    }
}
