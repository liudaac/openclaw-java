package openclaw.provider.perplexity;

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
 * Perplexity Web Search Provider.
 * Supports both Search API and Chat Completions API.
 *
 * @author OpenClaw Team
 * @version 2026.3.18
 */
public class PerplexityWebSearchProvider implements WebSearchProvider {

    private static final Logger logger = LoggerFactory.getLogger(PerplexityWebSearchProvider.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    // API Endpoints
    private static final String DEFAULT_BASE_URL = "https://openrouter.ai/api/v1";
    private static final String DIRECT_BASE_URL = "https://api.perplexity.ai";
    private static final String SEARCH_ENDPOINT = "https://api.perplexity.ai/search";
    private static final String DEFAULT_MODEL = "perplexity/sonar-pro";

    // Key prefixes
    private static final String[] PERPLEXITY_KEY_PREFIXES = {"pplx-"};
    private static final String[] OPENROUTER_KEY_PREFIXES = {"sk-or-"};

    @Override
    public String getId() { return "perplexity"; }

    @Override
    public String getLabel() { return "Perplexity Search"; }

    @Override
    public String getHint() { return "AI-synthesized answers · citations · structured filters"; }

    @Override
    public String[] getEnvVars() { return new String[]{"PERPLEXITY_API_KEY", "OPENROUTER_API_KEY"}; }

    @Override
    public String getPlaceholder() { return "pplx-..."; }

    @Override
    public String getSignupUrl() { return "https://www.perplexity.ai/settings/api"; }

    @Override
    public Optional<String> getDocsUrl() { return Optional.of("https://docs.openclaw.ai/perplexity"); }

    @Override
    public int getAutoDetectOrder() { return 50; }

    @Override
    public String getCredentialPath() { return "plugins.entries.perplexity.config.webSearch.apiKey"; }

    @Override
    public Object getCredentialValue(Map<String, Object> searchConfig) {
        if (searchConfig == null) return null;
        Object config = searchConfig.get("perplexity");
        if (config instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) config;
            return map.get("apiKey");
        }
        return searchConfig.get("apiKey");
    }

    @Override
    public void setCredentialValue(Map<String, Object> target, Object value) {
        Object config = target.get("perplexity");
        if (!(config instanceof Map)) {
            config = new HashMap<>();
            target.put("perplexity", config);
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) config;
        map.put("apiKey", value);
    }

    @Override
    public WebSearchToolDefinition createTool(WebSearchContext ctx) {
        PerplexityConfig config = resolveConfig(ctx);
        Transport transport = resolveTransport(config);

        return WebSearchToolDefinition.builder()
                .providerId(getId())
                .description(transport == Transport.SEARCH_API
                        ? "Search the web using Perplexity. Supports structured filters (domain, country, language, time)."
                        : "Search the web using Perplexity Sonar via chat completions. Returns AI-synthesized answers with citations.")
                .parameters(createSchema(transport))
                .execute(args -> executeSearch(args, ctx, config, transport))
                .build();
    }

    private Map<String, Object> createSchema(Transport transport) {
        Map<String, Object> props = new HashMap<>();
        props.put("query", Map.of("type", "string", "description", "Search query string."));
        props.put("count", Map.of("type", "integer", "description", "Number of results (1-10).", "minimum", 1, "maximum", 10));
        props.put("freshness", Map.of("type", "string", "description", "Filter by time: day, week, month, or year."));

        if (transport == Transport.SEARCH_API) {
            props.put("country", Map.of("type", "string", "description", "2-letter country code."));
            props.put("language", Map.of("type", "string", "description", "ISO 639-1 language code."));
            props.put("date_after", Map.of("type", "string", "description", "Only results after this date (YYYY-MM-DD)."));
            props.put("date_before", Map.of("type", "string", "description", "Only results before this date (YYYY-MM-DD)."));
            props.put("domain_filter", Map.of("type", "array", "description", "Domain filter (max 20).", "items", Map.of("type", "string")));
        }

        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        schema.put("properties", props);
        schema.put("required", List.of("query"));
        return schema;
    }

    private CompletableFuture<Map<String, Object>> executeSearch(
            Map<String, Object> args, WebSearchContext ctx,
            PerplexityConfig config, Transport transport) {

        String apiKey = resolveApiKey(config);
        if (apiKey == null) {
            return CompletableFuture.completedFuture(Map.of(
                    "error", "missing_perplexity_api_key",
                    "message", "Set PERPLEXITY_API_KEY or OPENROUTER_API_KEY environment variable."
            ));
        }

        String query = ValidationUtils.readStringParam(args, "query", true);
        String cacheKey = CacheUtils.buildSearchCacheKey("perplexity", transport.name(), query);

        Optional<Map<String, Object>> cached = CacheUtils.readCachedSearchPayload(cacheKey);
        if (cached.isPresent()) {
            return CompletableFuture.completedFuture(cached.get());
        }

        long startTime = System.currentTimeMillis();
        int timeout = ValidationUtils.resolveSearchTimeoutSeconds(ctx.getSearchConfig());

        if (transport == Transport.SEARCH_API) {
            return runSearchApi(query, apiKey, timeout, args, startTime, cacheKey, ctx);
        } else {
            return runChatCompletions(query, apiKey, config.baseUrl, config.model, timeout, args, startTime, cacheKey, ctx);
        }
    }

    private CompletableFuture<Map<String, Object>> runSearchApi(
            String query, String apiKey, int timeout,
            Map<String, Object> args, long startTime, String cacheKey, WebSearchContext ctx) {

        Map<String, Object> body = new HashMap<>();
        body.put("query", query);

        Integer count = ValidationUtils.readNumberParam(args, "count");
        body.put("max_results", ValidationUtils.resolveSearchCount(count, ValidationUtils.DEFAULT_SEARCH_COUNT));

        String country = ValidationUtils.readStringParam(args, "country");
        if (country != null) body.put("country", country);

        String language = ValidationUtils.readStringParam(args, "language");
        if (language != null) body.put("search_language_filter", List.of(language));

        String freshness = ValidationUtils.readStringParam(args, "freshness");
        if (freshness != null) body.put("search_recency_filter", freshness);

        // Domain filter
        String[] domains = ValidationUtils.readStringArrayParam(args, "domain_filter");
        if (domains.length > 0) {
            body.put("domain_filter", List.of(domains));
        }

        try {
            String jsonBody = objectMapper.writeValueAsString(body);

            return HttpUtils.postJson(SEARCH_ENDPOINT, timeout, Map.of("Authorization", "Bearer " + apiKey), jsonBody)
                    .thenApply(response -> parseSearchApiResponse(response, query, startTime, cacheKey, ctx));

        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    private CompletableFuture<Map<String, Object>> runChatCompletions(
            String query, String apiKey, String baseUrl, String model,
            int timeout, Map<String, Object> args, long startTime,
            String cacheKey, WebSearchContext ctx) {

        String endpoint = baseUrl + "/chat/completions";

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", List.of(
                Map.of("role", "user", "content", query)
        ));

        Integer count = ValidationUtils.readNumberParam(args, "count");
        body.put("max_tokens", count != null ? count * 100 : 500);

        String freshness = ValidationUtils.readStringParam(args, "freshness");
        if (freshness != null) {
            body.put("search_recency_filter", freshness);
        }

        try {
            String jsonBody = objectMapper.writeValueAsString(body);

            return HttpUtils.postJson(endpoint, timeout, Map.of("Authorization", "Bearer " + apiKey), jsonBody)
                    .thenApply(response -> parseChatResponse(response, query, startTime, cacheKey, ctx));

        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseSearchApiResponse(String body, String query, long startTime,
                                                        String cacheKey, WebSearchContext ctx) throws Exception {
        JsonNode root = objectMapper.readTree(body);
        JsonNode results = root.path("results");

        List<Map<String, Object>> resultList = new ArrayList<>();
        if (results.isArray()) {
            for (JsonNode r : results) {
                Map<String, Object> item = new HashMap<>();
                item.put("title", r.path("title").asText());
                item.put("url", r.path("url").asText());
                item.put("description", r.path("snippet").asText());
                String date = r.path("date").asText(null);
                if (date != null) item.put("published", date);
                resultList.add(item);
            }
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("query", query);
        payload.put("provider", "perplexity");
        payload.put("mode", "search_api");
        payload.put("count", resultList.size());
        payload.put("tookMs", System.currentTimeMillis() - startTime);
        payload.put("results", resultList);

        long cacheTtl = ValidationUtils.resolveSearchCacheTtlMs(ctx.getSearchConfig());
        CacheUtils.writeCachedSearchPayload(cacheKey, payload, cacheTtl);

        return payload;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseChatResponse(String body, String query, long startTime,
                                                   String cacheKey, WebSearchContext ctx) throws Exception {
        JsonNode root = objectMapper.readTree(body);
        JsonNode choices = root.path("choices");

        String content = "";
        List<Map<String, Object>> citations = new ArrayList<>();

        if (choices.isArray() && choices.size() > 0) {
            JsonNode message = choices.get(0).path("message");
            content = message.path("content").asText();

            JsonNode annotations = message.path("annotations");
            if (annotations.isArray()) {
                for (JsonNode ann : annotations) {
                    String type = ann.path("type").asText();
                    if ("url_citation".equals(type)) {
                        Map<String, Object> citation = new HashMap<>();
                        citation.put("url", ann.path("url_citation").path("url").asText());
                        citations.add(citation);
                    }
                }
            }
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("query", query);
        payload.put("provider", "perplexity");
        payload.put("mode", "chat_completions");
        payload.put("answer", content);
        payload.put("citations", citations);
        payload.put("tookMs", System.currentTimeMillis() - startTime);

        long cacheTtl = ValidationUtils.resolveSearchCacheTtlMs(ctx.getSearchConfig());
        CacheUtils.writeCachedSearchPayload(cacheKey, payload, cacheTtl);

        return payload;
    }

    // Helper methods

    private PerplexityConfig resolveConfig(WebSearchContext ctx) {
        PerplexityConfig config = new PerplexityConfig();

        Object cfg = ctx.getSearchConfig().get("perplexity");
        if (cfg instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) cfg;
            config.apiKey = map.get("apiKey") != null ? map.get("apiKey").toString() : null;
            config.baseUrl = map.get("baseUrl") != null ? map.get("baseUrl").toString() : null;
            config.model = map.get("model") != null ? map.get("model").toString() : null;
        }

        // Resolve base URL
        if (config.baseUrl == null) {
            BaseUrlHint hint = inferBaseUrlFromApiKey(config.apiKey);
            config.baseUrl = hint == BaseUrlHint.OPENROUTER ? DEFAULT_BASE_URL : DIRECT_BASE_URL;
        }

        // Resolve model
        if (config.model == null) {
            config.model = DEFAULT_MODEL;
        }

        return config;
    }

    private String resolveApiKey(PerplexityConfig config) {
        if (config.apiKey != null) return config.apiKey;
        String key = CredentialUtils.readProviderEnvValue("PERPLEXITY_API_KEY");
        if (key != null) return key;
        return CredentialUtils.readProviderEnvValue("OPENROUTER_API_KEY");
    }

    private Transport resolveTransport(PerplexityConfig config) {
        // Default to chat_completions for now
        return Transport.CHAT_COMPLETIONS;
    }

    private BaseUrlHint inferBaseUrlFromApiKey(String apiKey) {
        if (apiKey == null) return null;
        String normalized = apiKey.toLowerCase();
        for (String prefix : PERPLEXITY_KEY_PREFIXES) {
            if (normalized.startsWith(prefix)) return BaseUrlHint.DIRECT;
        }
        for (String prefix : OPENROUTER_KEY_PREFIXES) {
            if (normalized.startsWith(prefix)) return BaseUrlHint.OPENROUTER;
        }
        return null;
    }

    // Inner classes

    private static class PerplexityConfig {
        String apiKey;
        String baseUrl;
        String model;
    }

    private enum Transport {
        SEARCH_API,
        CHAT_COMPLETIONS
    }

    private enum BaseUrlHint {
        DIRECT,
        OPENROUTER
    }
}