package openclaw.plugin.sdk.websearch;

import java.util.Map;
import java.util.Optional;

/**
 * Web Search Runtime Metadata Context.
 * Context for resolving runtime metadata.
 *
 * @author OpenClaw Team
 * @version 2026.3.18
 */
public class WebSearchRuntimeMetadataContext {

    private final WebSearchProvider.OpenClawConfig config;
    private final Map<String, Object> searchConfig;
    private final Map<String, Object> runtimeMetadata;
    private final ResolvedCredential resolvedCredential;

    /**
     * Constructor.
     */
    public WebSearchRuntimeMetadataContext() {
        this(null, null, null, null);
    }

    /**
     * Constructor.
     */
    public WebSearchRuntimeMetadataContext(WebSearchProvider.OpenClawConfig config,
                                           Map<String, Object> searchConfig,
                                           Map<String, Object> runtimeMetadata,
                                           ResolvedCredential resolvedCredential) {
        this.config = config;
        this.searchConfig = searchConfig;
        this.runtimeMetadata = runtimeMetadata;
        this.resolvedCredential = resolvedCredential;
    }

    /**
     * Get the config.
     *
     * @return optional config
     */
    public Optional<WebSearchProvider.OpenClawConfig> getConfig() {
        return Optional.ofNullable(config);
    }

    /**
     * Get the search config.
     *
     * @return search config
     */
    public Map<String, Object> getSearchConfig() {
        return searchConfig;
    }

    /**
     * Get the runtime metadata.
     *
     * @return runtime metadata
     */
    public Map<String, Object> getRuntimeMetadata() {
        return runtimeMetadata;
    }

    /**
     * Get the resolved credential.
     *
     * @return optional resolved credential
     */
    public Optional<ResolvedCredential> getResolvedCredential() {
        return Optional.ofNullable(resolvedCredential);
    }

    /**
     * Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class.
     */
    public static class Builder {
        private WebSearchProvider.OpenClawConfig config;
        private Map<String, Object> searchConfig;
        private Map<String, Object> runtimeMetadata;
        private ResolvedCredential resolvedCredential;

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

        public Builder resolvedCredential(ResolvedCredential resolvedCredential) {
            this.resolvedCredential = resolvedCredential;
            return this;
        }

        public WebSearchRuntimeMetadataContext build() {
            return new WebSearchRuntimeMetadataContext(config, searchConfig, runtimeMetadata, resolvedCredential);
        }
    }

    /**
     * Resolved credential information.
     */
    public static class ResolvedCredential {
        private final String value;
        private final CredentialSource source;
        private final String fallbackEnvVar;

        public ResolvedCredential(String value, CredentialSource source) {
            this(value, source, null);
        }

        public ResolvedCredential(String value, CredentialSource source, String fallbackEnvVar) {
            this.value = value;
            this.source = source;
            this.fallbackEnvVar = fallbackEnvVar;
        }

        public String getValue() {
            return value;
        }

        public CredentialSource getSource() {
            return source;
        }

        public String getFallbackEnvVar() {
            return fallbackEnvVar;
        }
    }

    /**
     * Credential source enum.
     */
    public enum CredentialSource {
        CONFIG,
        SECRET_REF,
        ENV,
        MISSING
    }
}
