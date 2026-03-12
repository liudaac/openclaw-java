package openclaw.sdk.provider;

import openclaw.sdk.core.OpenClawConfig;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Provider plugin interface for AI model providers.
 *
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public interface ProviderPlugin {

    /**
     * Gets the provider ID.
     *
     * @return the provider ID
     */
    String getId();

    /**
     * Gets the provider label.
     *
     * @return the label
     */
    String getLabel();

    /**
     * Gets the documentation path.
     *
     * @return the docs path if available
     */
    default Optional<String> getDocsPath() {
        return Optional.empty();
    }

    /**
     * Gets provider aliases.
     *
     * @return list of aliases
     */
    default List<String> getAliases() {
        return List.of();
    }

    /**
     * Gets environment variables used by this provider.
     *
     * @return list of env var names
     */
    default List<String> getEnvVars() {
        return List.of();
    }

    /**
     * Gets the model configuration.
     *
     * @return the model config if available
     */
    default Optional<ModelProviderConfig> getModels() {
        return Optional.empty();
    }

    /**
     * Gets authentication methods.
     *
     * @return list of auth methods
     */
    List<ProviderAuthMethod> getAuthMethods();

    /**
     * Formats an API key for this provider.
     *
     * @param credential the credential
     * @return the formatted key
     */
    default Optional<String> formatApiKey(Map<String, Object> credential) {
        return Optional.empty();
    }

    /**
     * Refreshes OAuth credentials.
     *
     * @param credential the current credential
     * @return the refreshed credential
     */
    default CompletableFuture<Optional<Map<String, Object>>> refreshOAuth(
            Map<String, Object> credential) {
        return CompletableFuture.completedFuture(Optional.of(credential));
    }
}
