package openclaw.tools.search;

import openclaw.plugin.sdk.websearch.WebSearchContext;
import openclaw.plugin.sdk.websearch.WebSearchProvider;
import openclaw.plugin.sdk.websearch.WebSearchProviderRegistry;
import openclaw.plugin.sdk.websearch.WebSearchToolDefinition;
import openclaw.sdk.tool.AgentTool;
import openclaw.sdk.tool.ToolExecuteContext;
import openclaw.sdk.tool.ToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Provider-based Web Search Tool.
 * Integrates Web Search Providers using the new plugin architecture.
 *
 * @author OpenClaw Team
 * @version 2026.3.18
 */
public class ProviderBasedWebSearchTool implements AgentTool {

    private static final Logger logger = LoggerFactory.getLogger(ProviderBasedWebSearchTool.class);

    private final WebSearchProviderRegistry registry;
    private final String defaultProvider;

    /**
     * Constructor with registry.
     */
    public ProviderBasedWebSearchTool() {
        this(new WebSearchProviderRegistry(), "brave");
    }

    /**
     * Constructor with custom registry and default provider.
     */
    public ProviderBasedWebSearchTool(WebSearchProviderRegistry registry, String defaultProvider) {
        this.registry = registry;
        this.defaultProvider = defaultProvider;
        logger.info("Initialized ProviderBasedWebSearchTool with {} providers",
                registry.getAllProviders().size());
    }

    @Override
    public String getName() {
        return "web_search";
    }

    @Override
    public String getDescription() {
        String providers = registry.getProviderIds().stream()
                .sorted()
                .collect(Collectors.joining(", "));
        return "Search the web using configured providers (" + providers + "). Supports region-specific " +
                "and localized search via country and language parameters.";
    }

    @Override
    public ToolParameters getParameters() {
        Map<String, PropertySchema> properties = new HashMap<>();

        // Query (required)
        properties.put("query", AgentTool.PropertySchema.string("Search query string."));

        // Provider selection
        List<String> providerIds = new ArrayList<>(registry.getProviderIds());
        properties.put("provider", AgentTool.PropertySchema.enum_("Search provider to use.", providerIds));

        // Count
        properties.put("count", AgentTool.PropertySchema.integer(
                "Number of results to return (1-10)."));

        // Country
        properties.put("country", AgentTool.PropertySchema.string(
                "2-letter country code for region-specific results (e.g., 'US', 'DE')."));

        // Language
        properties.put("language", AgentTool.PropertySchema.string(
                "ISO 639-1 language code for results (e.g., 'en', 'de', 'fr')."));

        // Freshness
        properties.put("freshness", AgentTool.PropertySchema.enum_(
                "Filter by time.", List.of("day", "week", "month", "year")));

        // Date filters
        properties.put("date_after", AgentTool.PropertySchema.string(
                "Only results published after this date (YYYY-MM-DD)."));
        properties.put("date_before", AgentTool.PropertySchema.string(
                "Only results published before this date (YYYY-MM-DD)."));

        return ToolParameters.builder()
                .properties(properties)
                .required(List.of("query"))
                .build();
    }

    @Override
    public CompletableFuture<ToolResult> execute(ToolExecuteContext context) {
        Map<String, Object> args = context.arguments();

        // Get provider
        String providerId = (String) args.getOrDefault("provider", defaultProvider);
        Optional<WebSearchProvider> providerOpt = registry.getProvider(providerId);

        if (providerOpt.isEmpty()) {
            return CompletableFuture.completedFuture(ToolResult.failure(
                    "Unknown provider: " + providerId + ". Available: " + registry.getProviderIds()));
        }

        WebSearchProvider provider = providerOpt.get();

        // Build search config from arguments
        Map<String, Object> searchConfig = buildSearchConfig(args);

        // Create context
        WebSearchContext webCtx = WebSearchContext.builder()
                .searchConfig(searchConfig)
                .build();

        // Check if configured
        if (!provider.isConfigured(webCtx)) {
            return CompletableFuture.completedFuture(ToolResult.failure(
                    "Provider '" + providerId + "' is not configured. " +
                            "Set " + String.join(" or ", provider.getEnvVars()) + " environment variable."));
        }

        // Create tool and execute
        WebSearchToolDefinition tool = provider.createTool(webCtx);
        if (tool == null) {
            return CompletableFuture.completedFuture(ToolResult.failure(
                    "Failed to create tool for provider: " + providerId));
        }

        return tool.execute(args)
                .thenApply(this::formatResult)
                .exceptionally(e -> {
                    logger.error("Search failed", e);
                    return ToolResult.failure("Search failed: " + e.getMessage());
                });
    }

    /**
     * Build search config from arguments.
     */
    private Map<String, Object> buildSearchConfig(Map<String, Object> args) {
        Map<String, Object> config = new HashMap<>();

        // Copy relevant args to config
        String[] keys = {"apiKey", "baseUrl", "model", "timeoutSeconds", "cacheTtlMinutes"};
        for (String key : keys) {
            if (args.containsKey(key)) {
                config.put(key, args.get(key));
            }
        }

        return config;
    }

    /**
     * Format search result for display.
     */
    @SuppressWarnings("unchecked")
    private ToolResult formatResult(Map<String, Object> result) {
        // Check for error
        if (result.containsKey("error")) {
            String error = (String) result.get("error");
            String message = (String) result.getOrDefault("message", error);
            return ToolResult.failure(message);
        }

        StringBuilder sb = new StringBuilder();

        // Query info
        String query = (String) result.get("query");
        String provider = (String) result.get("provider");
        Integer count = (Integer) result.get("count");
        Long tookMs = (Long) result.get("tookMs");

        sb.append("Search results for: ").append(query).append("\n");
        sb.append("Provider: ").append(provider);
        if (count != null) {
            sb.append(" | Results: ").append(count);
        }
        if (tookMs != null) {
            sb.append(" | Time: ").append(tookMs).append("ms");
        }
        sb.append("\n\n");

        // Results
        List<Map<String, Object>> results = (List<Map<String, Object>>) result.get("results");
        if (results != null) {
            for (int i = 0; i < results.size(); i++) {
                Map<String, Object> item = results.get(i);
                sb.append("[").append(i + 1).append("] ");
                sb.append(item.getOrDefault("title", "No title")).append("\n");
                sb.append("URL: ").append(item.getOrDefault("url", "N/A")).append("\n");

                String description = (String) item.get("description");
                if (description != null && !description.isEmpty()) {
                    sb.append(description).append("\n");
                }

                String published = (String) item.get("published");
                if (published != null) {
                    sb.append("Published: ").append(published).append("\n");
                }

                sb.append("\n");
            }
        }

        return ToolResult.success(sb.toString());
    }
}