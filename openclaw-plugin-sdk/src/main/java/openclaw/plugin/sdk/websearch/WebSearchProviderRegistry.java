package openclaw.plugin.sdk.websearch;

import openclaw.plugin.sdk.annotation.PublicApi;
import openclaw.plugin.sdk.internal.ServiceLoaderHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Web Search Provider Registry.
 * Loads and manages Web Search Providers using ServiceLoader (SPI).
 *
 * @author OpenClaw Team
 * @version 2026.3.18
 * @since 2026.3.0
 */
@PublicApi(since = "2026.3.0", stability = "stable",
           description = "Registry for Web Search Providers")
public class WebSearchProviderRegistry {

    private static final Logger logger = LoggerFactory.getLogger(WebSearchProviderRegistry.class);

    private final Map<String, WebSearchProvider> providers = new HashMap<>();

    /**
     * Constructor - loads all providers via SPI.
     */
    public WebSearchProviderRegistry() {
        loadProviders();
    }

    /**
     * Load providers using ServiceLoader.
     */
    private void loadProviders() {
        List<WebSearchProvider> loaded = ServiceLoaderHelper.loadServices(WebSearchProvider.class);
        for (WebSearchProvider provider : loaded) {
            register(provider);
        }
        logger.info("Loaded {} web search providers: {}",
                providers.size(),
                String.join(", ", providers.keySet()));
    }

    /**
     * Register a provider.
     *
     * @param provider the provider
     */
    @PublicApi(since = "2026.3.0")
    public void register(WebSearchProvider provider) {
        providers.put(provider.getId(), provider);
        logger.debug("Registered web search provider: {}", provider.getId());
    }

    /**
     * Get a provider by ID.
     *
     * @param id the provider ID
     * @return optional provider
     */
    @PublicApi(since = "2026.3.0")
    public Optional<WebSearchProvider> getProvider(String id) {
        return Optional.ofNullable(providers.get(id));
    }

    /**
     * Get all providers.
     *
     * @return list of providers
     */
    @PublicApi(since = "2026.3.0")
    public List<WebSearchProvider> getAllProviders() {
        return new ArrayList<>(providers.values());
    }

    /**
     * Get providers sorted by auto-detect order.
     *
     * @return sorted list
     */
    @PublicApi(since = "2026.3.0")
    public List<WebSearchProvider> getProvidersByAutoDetectOrder() {
        return providers.values().stream()
                .sorted(Comparator.comparingInt(WebSearchProvider::getAutoDetectOrder))
                .toList();
    }

    /**
     * Check if a provider exists.
     *
     * @param id the provider ID
     * @return true if exists
     */
    @PublicApi(since = "2026.3.0")
    public boolean hasProvider(String id) {
        return providers.containsKey(id);
    }

    /**
     * Get provider IDs.
     *
     * @return set of IDs
     */
    @PublicApi(since = "2026.3.0")
    public Set<String> getProviderIds() {
        return new HashSet<>(providers.keySet());
    }
}
