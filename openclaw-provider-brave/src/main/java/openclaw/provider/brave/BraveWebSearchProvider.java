package openclaw.provider.brave;

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
 * Brave Web Search Provider.
 *
 * @author OpenClaw Team
 * @version 2026.3.18
 */
public class BraveWebSearchProvider implements WebSearchProvider {

    private static final Logger logger = LoggerFactory.getLogger(BraveWebSearchProvider.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final String BRAVE_SEARCH_ENDPOINT = "https://api.search.brave.com/res/v1/web/search";
    private static final String BRAVE_LLM_CONTEXT_ENDPOINT = "https://api.search.brave.com/res/v1/llm/context";

    private static final Set<String> BRAVE_SEARCH_LANG_CODES = Set.of(
            "ar", "eu", "bn", "bg", "ca", "zh-hans", "zh-hant", "hr", "cs", "da", "nl",
            "en", "en-gb", "et", "fi", "fr", "gl", "de", "el", "gu", "he", "hi", "hu",
            "is", "it", "jp", "kn", "ko", "lv", "lt", "ms", "ml", "mr", "nb", "pl",
            "pt-br", "pt-pt", "pa", "ro", "ru", "sr", "sk", "sl", "es", "sv", "ta",
            "te", "th", "tr", "uk", "vi"
    );

    private static final Map<String, String> BRAVE_SEARCH_LANG_ALIASES = Map.of(
            "ja", "jp",
            "zh", "zh-hans",
            "zh-cn", "zh-hans",
            "zh-hk", "zh-hant",
            "zh-sg", "zh-hans",
            "zh-tw", "zh-hant"
    );

    @Override
    public String getId() { return "brave"; }

    @Override
    public String getLabel() { return "Brave Search"; }

    @Override
    public String getHint() { return "Structured results · country/language/time filters"; }

    @Override
    public String[] getEnvVars() { return new String[]{"BRAVE_API_KEY"}; }

    @Override
    public String getPlaceholder() { return "BSA..."; }

    @Override
    public String getSignupUrl() { return "https://brave.com/search/api/"; }

    @Override
    public Optional<String> getDocsUrl() { return Optional.of("https://docs.openclaw.ai/brave-search"); }

    @Override
    public int getAutoDetectOrder() { return 10; }

    @Override
    public String getCredentialPath() { return "plugins.entries.brave.config.webSearch.apiKey"; }

    @Override
    public Object getCredentialValue(Map<String, Object> searchConfig) {
        if (searchConfig == null) return null;
        Object config = searchConfig.get("brave");
        if (config instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) config;
            return map.get("apiKey");
        }
        return searchConfig.get("apiKey");
    }

    @Override
    public void setCredentialValue(Map<String, Object> target, Object value) {
        Object config = target.get("brave");
        if (!(config instanceof Map)) {
            config = new HashMap<>();
            target.put("brave", config);
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) config;
        map.put("apiKey", value);
    }

    @Override
    public WebSearchToolDefinition createTool(WebSearchContext ctx) {
        return WebSearchToolDefinition.builder()
                .providerId(getId())
                .description("Search the web using Brave Search API.")
                .parameters(createSchema())
                .execute(args -> executeSearch(args, ctx))
                .build();
    }

    private Map<String, Object> createSchema() {
        Map<String, Object> props = new HashMap<>();
        props.put("query", Map.of("type", "string", "description", "Search query string."));
        props.put("count", Map.of("type", "integer", "description", "Number of results (1-10).", "minimum", 1, "maximum", 10));
        props.put("country", Map.of("type", "string", "description", "2-letter country code."));
        props.put("language", Map.of("type", "string", "description", "ISO 639-1 language code."));
        props.put("freshness", Map.of("type", "string", "description", "Filter by time: day, week, month, or year."));

        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        schema.put("properties", props);
        schema.put("required", List.of("query"));
        return schema;
    }

    private CompletableFuture<Map<String, Object>> executeSearch(Map<String, Object> args, WebSearchContext ctx) {
        String apiKey = resolveApiKey(ctx);
        if (apiKey == null) {
            return CompletableFuture.completedFuture(Map.of(
                    "error", "missing_brave_api_key",
                    "message", "Set BRAVE_API_KEY environment variable."
            ));
        }

        String query = ValidationUtils.readStringParam(args, "query", true);
        String cacheKey = CacheUtils.buildSearchCacheKey("brave", query);

        Optional<Map<String, Object>> cached = CacheUtils.readCachedSearchPayload(cacheKey);
        if (cached.isPresent()) {
            return CompletableFuture.completedFuture(cached.get());
        }

        // Build URL and execute request
        StringBuilder url = new StringBuilder(BRAVE_SEARCH_ENDPOINT);
        url.append("?q=").append(java.net.URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8));

        Integer count = ValidationUtils.readNumberParam(args, "count");
        url.append("&count=").append(ValidationUtils.resolveSearchCount(count, ValidationUtils.DEFAULT_SEARCH_COUNT));

        String country = ValidationUtils.readStringParam(args, "country");
        if (country != null) url.append("&country=").append(country.toUpperCase());

        long startTime = System.currentTimeMillis();
        int timeout = ValidationUtils.resolveSearchTimeoutSeconds(ctx.getSearchConfig());

        return HttpUtils.withTrustedWebSearchEndpoint(
                url.toString(),
                timeout,
                builder -> builder.header("Accept", "application/json")
                        .header("X-Subscription-Token", apiKey),
                response -> {
                    if (response.statusCode() < 200 || response.statusCode() >= 300) {
                        throw new RuntimeException("Brave API error: " + response.statusCode());
                    }
                    return parseResponse(response.body(), query, startTime, cacheKey, ctx);
                }
        );
    }

    private Map<String, Object> parseResponse(String body, String query, long startTime,
                                               String cacheKey, WebSearchContext ctx) {
        JsonNode root;
        try {
            root = objectMapper.readTree(body);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Brave API response", e);
        }
        JsonNode results = root.path("web").path("results");

        List<Map<String, Object>> resultList = new ArrayList<>();
        if (results.isArray()) {
            for (JsonNode r : results) {
                Map<String, Object> item = new HashMap<>();
                item.put("title", r.path("title").asText());
                item.put("url", r.path("url").asText());
                item.put("description", r.path("description").asText());
                resultList.add(item);
            }
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("query", query);
        payload.put("provider", "brave");
        payload.put("count", resultList.size());
        payload.put("tookMs", System.currentTimeMillis() - startTime);
        payload.put("results", resultList);

        long cacheTtl = ValidationUtils.resolveSearchCacheTtlMs(ctx.getSearchConfig());
        CacheUtils.writeCachedSearchPayload(cacheKey, payload, cacheTtl);

        return payload;
    }

    private String resolveApiKey(WebSearchContext ctx) {
        Object key = getCredentialValue(ctx.getSearchConfig());
        if (key != null) return key.toString();
        return CredentialUtils.readProviderEnvValue("BRAVE_API_KEY");
    }
}
