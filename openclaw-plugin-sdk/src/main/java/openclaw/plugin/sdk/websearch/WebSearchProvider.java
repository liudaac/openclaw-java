package openclaw.plugin.sdk.websearch;

import openclaw.plugin.sdk.annotation.PublicApi;
import openclaw.plugin.sdk.annotation.SpiApi;

import java.util.Map;
import java.util.Optional;

/**
 * Web Search Provider interface.
 * Defines the contract for web search providers (Brave, Perplexity, Google, etc.)
 *
 * <p>This is a Service Provider Interface (SPI) that can be implemented by plugins
 * to provide web search functionality.
 *
 * @author OpenClaw Team
 * @version 2026.3.18
 * @since 2026.3.0
 */
@SpiApi(serviceType = "web_search", multipleImplementations = true)
@PublicApi(since = "2026.3.0", stability = "stable",
           description = "Interface for web search providers")
public interface WebSearchProvider {

    /**
     * Provider ID (e.g., "brave", "perplexity", "google")
     *
     * @return the provider ID
     */
    @PublicApi(since = "2026.3.0")
    String getId();

    /**
     * Display label for the provider.
     *
     * @return the label
     */
    String getLabel();

    /**
     * Short hint describing the provider's capabilities.
     *
     * @return the hint
     */
    String getHint();

    /**
     * Environment variable names for API key detection.
     *
     * @return array of environment variable names
     */
    String[] getEnvVars();

    /**
     * Placeholder text for API key input.
     *
     * @return the placeholder
     */
    String getPlaceholder();

    /**
     * URL for provider signup/API key registration.
     *
     * @return the signup URL
     */
    String getSignupUrl();

    /**
     * Documentation URL (optional).
     *
     * @return optional docs URL
     */
    default Optional<String> getDocsUrl() {
        return Optional.empty();
    }

    /**
     * Auto-detection order priority (lower = higher priority).
     *
     * @return the order
     */
    default int getAutoDetectOrder() {
        return 100;
    }

    /**
     * Configuration path for the credential.
     *
     * @return the credential path
     */
    String getCredentialPath();

    /**
     * Inactive/alternative secret paths.
     *
     * @return array of inactive secret paths
     */
    default String[] getInactiveSecretPaths() {
        return new String[0];
    }

    /**
     * Get credential value from search config.
     *
     * @param searchConfig the search configuration
     * @return the credential value
     */
    Object getCredentialValue(Map<String, Object> searchConfig);

    /**
     * Set credential value in search config.
     *
     * @param searchConfigTarget the target config
     * @param value the value to set
     */
    void setCredentialValue(Map<String, Object> searchConfigTarget, Object value);

    /**
     * Get configured credential value from OpenClaw config.
     *
     * @param config the OpenClaw config
     * @return optional credential value
     */
    default Optional<Object> getConfiguredCredentialValue(OpenClawConfig config) {
        return Optional.empty();
    }

    /**
     * Set configured credential value in OpenClaw config.
     *
     * @param configTarget the target config
     * @param value the value to set
     */
    default void setConfiguredCredentialValue(OpenClawConfig configTarget, Object value) {
        // Default no-op
    }

    /**
     * Apply selection config (optional).
     *
     * @param config the config
     * @return modified config
     */
    default OpenClawConfig applySelectionConfig(OpenClawConfig config) {
        return config;
    }

    /**
     * Resolve runtime metadata.
     *
     * @param ctx the runtime metadata context
     * @return map of metadata
     */
    default Map<String, Object> resolveRuntimeMetadata(WebSearchRuntimeMetadataContext ctx) {
        return Map.of();
    }

    /**
     * Create the search tool for this provider.
     *
     * @param ctx the web search context
     * @return the tool definition, or null if not configured
     */
    @PublicApi(since = "2026.3.0")
    WebSearchToolDefinition createTool(WebSearchContext ctx);

    /**
     * Check if this provider is configured and ready to use.
     *
     * @param ctx the web search context
     * @return true if configured
     */
    @PublicApi(since = "2026.3.0")
    default boolean isConfigured(WebSearchContext ctx) {
        return getCredentialValue(ctx.getSearchConfig()) != null ||
               ctx.getConfig().flatMap(this::getConfiguredCredentialValue).isPresent();
    }

    /**
     * OpenClaw configuration placeholder.
     * This will be replaced with the actual config class from openclaw-server.
     */
    @PublicApi(since = "2026.3.0")
    interface OpenClawConfig {
        // Placeholder for configuration interface
    }
}
