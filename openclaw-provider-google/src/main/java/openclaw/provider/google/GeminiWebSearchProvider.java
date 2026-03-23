package openclaw.provider.google;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import openclaw.plugin.sdk.websearch.WebSearchContext;
import openclaw.plugin.sdk.websearch.WebSearchProvider;
import openclaw.plugin.sdk.websearch.WebSearchToolDefinition;
import openclaw.plugin.sdk.websearch.utils.CacheUtils;
import openclaw.plugin.sdk.websearch.utils.CredentialUtils;
import openclaw.plugin.sdk.websearch.utils.HttpUtils;
import openclaw.plugin.sdk.websearch.utils.ValidationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Google Gemini Web Search Provider.
 * Uses Gemini API with Google Search grounding.
 *
 * @author OpenClaw Team
 * @version 2026.3.18
 */
public class GeminiWebSearchProvider implements WebSearchProvider {

    private static final Logger logger = LoggerFactory.getLogger(GeminiWebSearchProvider.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final String DEFAULT_MODEL = "gemini-2.5-flash";
    private static final String API_BASE = "https://generativelanguage.googleapis.com/v1beta";

    @Override
    public String getId() { return "google"; }

    @Override
    public String getLabel() { return "Google Gemini"; }

    @Override
    public String getHint() { return "AI-synthesized answers · Google Search grounding"; }

    @Override
    public String[] getEnvVars() { return new String[]{"GEMINI_API_KEY"}; }

    @Override
    public String getPlaceholder() { return "AIza..."; }

    @Override
    public String getSignupUrl() { return "https://ai.google.dev/"; }

    @Override
    public Optional<String> getDocsUrl() { return Optional.of("https://docs.openclaw.ai/google-gemini"); }

    @Override
    public int getAutoDetectOrder() { return 20; }

    @Override
    public String getCredentialPath() { return "plugins.entries.google.config.webSearch.apiKey"; }

    @Override
    public Object getCredentialValue(Map<String, Object> searchConfig) {
        if (searchConfig == null) return null;
        Object config = searchConfig.get("google");
        if (config instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) config;
            return map.get("apiKey");
        }
        return searchConfig.get("apiKey");
    }

    @Override
    public void setCredentialValue(Map<String, Object> target, Object value) {
        Object config = target.get("google");
        if (!(config instanceof Map)) {
            config = new HashMap<>();
            target.put("google", config);
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) config;
        map.put("apiKey", value);
    }

    @Override
    public WebSearchToolDefinition createTool(WebSearchContext ctx) {
        GeminiConfig config = resolveConfig(ctx);

        return WebSearchToolDefinition.builder()
                .providerId(getId())
                .description("Search the web using Google Gemini with Google Search grounding. " +
                        "Returns AI-synthesized answers with citations from web sources.")
                .parameters(createSchema())
                .execute(args -> executeSearch(args, ctx, config))
                .build();
    }

    private Map<String, Object> createSchema() {
        Map<String, Object> props = new HashMap<>();
        props.put("query", Map.of("type", "string", "description", "Search query string."));
        props.put("count", Map.of("type", "integer", "description", "Max tokens for response (approximate result size).", "minimum", 100, "maximum", 2000));

        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        schema.put("properties", props);
        schema.put("required", List.of("query"));
        return schema;
    }

    private CompletableFuture<Map<String, Object>> executeSearch(
            Map<String, Object> args, WebSearchContext ctx, GeminiConfig config) {

        String apiKey = resolveApiKey(config);
        if (apiKey == null) {
            return CompletableFuture.completedFuture(Map.of(
                    "error", "missing_gemini_api_key",
                    "message", "Set GEMINI_API_KEY environment variable."
            ));
        }

        String query = ValidationUtils.readStringParam(args, "query", true);
        String cacheKey = CacheUtils.buildSearchCacheKey("google", "gemini", query);

        Optional<Map<String, Object>> cached = CacheUtils.readCachedSearchPayload(cacheKey);
        if (cached.isPresent()) {
            return CompletableFuture.completedFuture(cached.get());
        }

        Integer count = ValidationUtils.readNumberParam(args, "count");
        int maxTokens = count != null ? count : 500;

        long startTime = System.currentTimeMillis();
        int timeout = ValidationUtils.resolveSearchTimeoutSeconds(ctx.getSearchConfig());

        String endpoint = API_BASE + "/models/" + config.model + ":generateContent";

        Map<String, Object> body = new HashMap<>();
        body.put("contents", List.of(
                Map.of("parts", List.of(Map.of("text", query)))
        ));
        body.put("tools", List.of(Map.of("google_search", Map.of())));
        body.put("generationConfig", Map.of("maxOutputTokens", maxTokens));

        try {
            String jsonBody = objectMapper.writeValueAsString(body);

            return HttpUtils.withTrustedWebSearchEndpoint(
                    endpoint + "?key=" + apiKey,
                    timeout,
                    builder -> builder
                            .header("Content-Type", "application/json")
                            .POST(java.net.http.HttpRequest.BodyPublishers.ofString(jsonBody)),
                    response -> {
                        if (response.statusCode() < 200 || response.statusCode() >= 300) {
                            throw new RuntimeException("Gemini API error: " + response.statusCode());
                        }
                        return parseResponse(response.body(), query, startTime, cacheKey, ctx);
                    }
            );
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseResponse(String body, String query, long startTime,
                                               String cacheKey, WebSearchContext ctx) {
        JsonNode root;
        try {
            root = objectMapper.readTree(body);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Gemini API response", e);
        }

        // Check for error
        if (root.has("error")) {
            JsonNode error = root.path("error");
            String message = error.path("message").asText("Unknown error");
            throw new RuntimeException("Gemini API error: " + message);
        }

        JsonNode candidate = root.path("candidates").get(0);
        JsonNode content = candidate.path("content");
        JsonNode groundingMetadata = candidate.path("groundingMetadata");

        // Extract text
        StringBuilder textBuilder = new StringBuilder();
        JsonNode parts = content.path("parts");
        if (parts.isArray()) {
            for (JsonNode part : parts) {
                textBuilder.append(part.path("text").asText());
            }
        }

        // Extract citations
        List<Map<String, Object>> citations = new ArrayList<>();
        JsonNode groundingChunks = groundingMetadata.path("groundingChunks");
        if (groundingChunks.isArray()) {
            for (JsonNode chunk : groundingChunks) {
                JsonNode web = chunk.path("web");
                String uri = web.path("uri").asText(null);
                if (uri != null) {
                    Map<String, Object> citation = new HashMap<>();
                    citation.put("url", uri);
                    citation.put("title", web.path("title").asText(null));
                    citations.add(citation);
                }
            }
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("query", query);
        payload.put("provider", "google");
        payload.put("model", "gemini");
        payload.put("answer", textBuilder.toString());
        payload.put("citations", citations);
        payload.put("citationCount", citations.size());
        payload.put("tookMs", System.currentTimeMillis() - startTime);

        long cacheTtl = ValidationUtils.resolveSearchCacheTtlMs(ctx.getSearchConfig());
        CacheUtils.writeCachedSearchPayload(cacheKey, payload, cacheTtl);

        return payload;
    }

    private GeminiConfig resolveConfig(WebSearchContext ctx) {
        GeminiConfig config = new GeminiConfig();

        Object cfg = ctx.getSearchConfig().get("google");
        if (cfg instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) cfg;
            config.apiKey = map.get("apiKey") != null ? map.get("apiKey").toString() : null;
            config.model = map.get("model") != null ? map.get("model").toString() : null;
        }

        if (config.model == null) {
            config.model = DEFAULT_MODEL;
        }

        return config;
    }

    private String resolveApiKey(GeminiConfig config) {
        if (config.apiKey != null) return config.apiKey;
        return CredentialUtils.readProviderEnvValue("GEMINI_API_KEY");
    }

    private static class GeminiConfig {
        String apiKey;
        String model;
    }
}