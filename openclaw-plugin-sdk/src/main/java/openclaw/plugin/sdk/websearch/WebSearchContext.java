package openclaw.plugin.sdk.websearch;

import openclaw.plugin.sdk.annotation.PublicApi;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

/**
 * Web Search Context.
 * Provides context for web search operations.
 *
 * @author OpenClaw Team
 * @version 2026.3.18
 * @since 2026.3.0
 */
@PublicApi(since = "2026.3.0", stability = "stable",
           description = "Context for web search operations")
public class WebSearchContext {

    private final WebSearchProvider.OpenClawConfig config;
    private final Map<String, Object> searchConfig;
    private final Map<String, Object> runtimeMetadata;

    /**
     * Constructor.
     */
    @PublicApi(since = "2026.3.0")
    public WebSearchContext() {
        this(null, null, null);
    }

    /**
     * Constructor with config.
     *
     * @param config the OpenClaw config
     */
    public WebSearchContext(WebSearchProvider.OpenClawConfig config) {
        this(config, null, null);
    }

    /**
     * Constructor with config and search config.
     *
     * @param config the OpenClaw config
     * @param searchConfig the search config
     */
    public WebSearchContext(WebSearchProvider.OpenClawConfig config, Map<String, Object> searchConfig) {
        this(config, searchConfig, null);
    }

    /**
     * Full constructor.
     *
     * @param config the OpenClaw config
     * @param searchConfig the search config
     * @param runtimeMetadata the runtime metadata
     */
    public WebSearchContext(WebSearchProvider.OpenClawConfig config,
                           Map<String, Object> searchConfig,
                           Map<String, Object> runtimeMetadata) {
        this.config = config;
        this.searchConfig = searchConfig != null ? searchConfig : Collections.emptyMap();
        this.runtimeMetadata = runtimeMetadata != null ? runtimeMetadata : Collections.emptyMap();
    }

    /**
     * Get the OpenClaw config.
     *
     * @return optional config
     */
    @PublicApi(since = "2026.3.0")
    public Optional<WebSearchProvider.OpenClawConfig> getConfig() {
        return Optional.ofNullable(config);
    }

    /**
     * Get the search config.
     *
     * @return search config map
     */
    @PublicApi(since = "2026.3.0")
    public Map<String, Object> getSearchConfig() {
        return searchConfig;
    }

    /**
     * Get a value from search config.
     *
     * @param key the key
     * @return optional value
     */
    public Optional<Object> getSearchConfigValue(String key) {
        return Optional.ofNullable(searchConfig.get(key));
    }

    /**
     * Get string value from search config.
     *
     * @param key the key
     * @return optional string value
     */
    public Optional<String> getSearchConfigString(String key) {
        return getSearchConfigValue(key).map(Object::toString);
    }

    /**
     * Get integer value from search config.
     *
     * @param key the key
     * @return optional integer value
     */
    public Optional<Integer> getSearchConfigInt(String key) {
        return getSearchConfigValue(key)
                .filter(v -> v instanceof Number)
                .map(v -> ((Number) v).intValue());
    }

    /**
     * Get boolean value from search config.
     *
     * @param key the key
     * @return optional boolean value
     */
    public Optional<Boolean> getSearchConfigBoolean(String key) {
        return getSearchConfigValue(key)
                .filter(v -> v instanceof Boolean)
                .map(v -> (Boolean) v);
    }

    /**
     * Get the runtime metadata.
     *
     * @return runtime metadata map
     */
    public Map<String, Object> getRuntimeMetadata() {
        return runtimeMetadata;
    }

    /**
     * Get a value from runtime metadata.
     *
     * @param key the key
     * @return optional value
     */
    public Optional<Object> getRuntimeMetadataValue(String key) {
        return Optional.ofNullable(runtimeMetadata.get(key));
    }

    /**
     * Builder for creating context.
     */
    @PublicApi(since = "2026.3.0")
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class.
     */
    @PublicApi(since = "2026.3.0")
    public static class Builder {
        private WebSearchProvider.OpenClawConfig config;
        private Map<String, Object> searchConfig;
        private Map<String, Object> runtimeMetadata;

        public Builder config(WebSearchProvider.OpenClawConfig config) {
            this.config = config;
            return this;
        }

        public Builder searchConfig(Map<String, Object> searchConfig) {
            this.searchConfig = searchConfig;
            return this;
        }

        public Builder runtimeMetadata(Map<String, Object> runtimeMetadata) {
            this.runtimeMetadata = runtimeMetadata;
            return this;
        }

        public WebSearchContext build() {
            return new WebSearchContext(config, searchConfig, runtimeMetadata);
        }
    }
}
