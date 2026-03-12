package openclaw.memory.embedding;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Mistral embedding provider implementation.
 *
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public class MistralEmbeddingProvider implements EmbeddingProvider {

    private static final String DEFAULT_MODEL = "mistral-embed";
    private static final int DEFAULT_DIMENSION = 1024;
    private static final String DEFAULT_BASE_URL = "https://api.mistral.ai/v1";

    private final String apiKey;
    private final String baseUrl;
    private final String model;
    private final int dimension;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public MistralEmbeddingProvider(String apiKey) {
        this(apiKey, DEFAULT_BASE_URL, DEFAULT_MODEL, DEFAULT_DIMENSION);
    }

    public MistralEmbeddingProvider(String apiKey, String baseUrl, String model, int dimension) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl != null ? baseUrl : DEFAULT_BASE_URL;
        this.model = model != null ? model : DEFAULT_MODEL;
        this.dimension = dimension;
        this.httpClient = new OkHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String getId() {
        return "mistral";
    }

    @Override
    public String getName() {
        return "Mistral";
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
        return embedBatch(List.of(text))
                .thenApply(results -> results.get(0));
    }

    @Override
    public CompletableFuture<List<EmbeddingVector>> embedBatch(List<String> texts) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return doEmbedBatch(texts);
            } catch (Exception e) {
                throw new EmbeddingException("Failed to embed batch with Mistral", e);
            }
        });
    }

    private List<EmbeddingVector> doEmbedBatch(List<String> texts) throws IOException {
        String jsonBody = objectMapper.writeValueAsString(new EmbeddingRequest(model, texts));

        Request request = new Request.Builder()
                .url(baseUrl + "/embeddings")
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .post(RequestBody.create(jsonBody, MediaType.parse("application/json")))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new EmbeddingException("Mistral API error: " + response.code() + " " + response.message());
            }

            JsonNode root = objectMapper.readTree(response.body().string());
            JsonNode data = root.get("data");

            List<EmbeddingVector> results = new java.util.ArrayList<>();
            for (int i = 0; i < data.size(); i++) {
                JsonNode item = data.get(i);
                int index = item.get("index").asInt();
                JsonNode embedding = item.get("embedding");

                float[] vector = new float[embedding.size()];
                for (int j = 0; j < embedding.size(); j++) {
                    vector[j] = (float) embedding.get(j).asDouble();
                }

                results.add(new EmbeddingVector(
                        texts.get(index),
                        vector,
                        model,
                        getId()
                ));
            }

            return results;
        }
    }

    @Override
    public CompletableFuture<Boolean> isAvailable() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Request request = new Request.Builder()
                        .url(baseUrl + "/models")
                        .header("Authorization", "Bearer " + apiKey)
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
    private record EmbeddingRequest(String model, List<String> input) {
    }
}
