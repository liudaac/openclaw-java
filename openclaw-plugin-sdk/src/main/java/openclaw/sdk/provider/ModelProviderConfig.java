package openclaw.sdk.provider;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Model provider configuration.
 *
 * @param baseUrl the API base URL
 * @param models list of available models
 * @param defaultModel the default model
 * @param headers additional headers
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public record ModelProviderConfig(
        Optional<String> baseUrl,
        List<ModelInfo> models,
        Optional<String> defaultModel,
        Map<String, String> headers
) {

    /**
     * Creates a builder for ModelProviderConfig.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for ModelProviderConfig.
     */
    public static class Builder {
        private String baseUrl;
        private List<ModelInfo> models = List.of();
        private String defaultModel;
        private Map<String, String> headers = Map.of();

        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public Builder models(List<ModelInfo> models) {
            this.models = models != null ? models : List.of();
            return this;
        }

        public Builder defaultModel(String defaultModel) {
            this.defaultModel = defaultModel;
            return this;
        }

        public Builder headers(Map<String, String> headers) {
            this.headers = headers != null ? headers : Map.of();
            return this;
        }

        public ModelProviderConfig build() {
            return new ModelProviderConfig(
                    Optional.ofNullable(baseUrl),
                    models,
                    Optional.ofNullable(defaultModel),
                    headers
            );
        }
    }

    /**
     * Model information.
     *
     * @param id the model ID
     * @param name the model name
     * @param description the description
     * @param contextWindow the context window size
     * @param maxTokens the max output tokens
     * @param supportsVision whether vision is supported
     * @param supportsTools whether tools are supported
     * @param metadata additional metadata
     */
    public record ModelInfo(
            String id,
            String name,
            Optional<String> description,
            int contextWindow,
            int maxTokens,
            boolean supportsVision,
            boolean supportsTools,
            Map<String, Object> metadata
    ) {

        /**
         * Creates a builder for ModelInfo.
         *
         * @return a new builder
         */
        public static Builder builder() {
            return new Builder();
        }

        /**
         * Builder for ModelInfo.
         */
        public static class Builder {
            private String id;
            private String name;
            private String description;
            private int contextWindow = 4096;
            private int maxTokens = 4096;
            private boolean supportsVision = false;
            private boolean supportsTools = false;
            private Map<String, Object> metadata = Map.of();

            public Builder id(String id) {
                this.id = id;
                return this;
            }

            public Builder name(String name) {
                this.name = name;
                return this;
            }

            public Builder description(String description) {
                this.description = description;
                return this;
            }

            public Builder contextWindow(int contextWindow) {
                this.contextWindow = contextWindow;
                return this;
            }

            public Builder maxTokens(int maxTokens) {
                this.maxTokens = maxTokens;
                return this;
            }

            public Builder supportsVision(boolean supportsVision) {
                this.supportsVision = supportsVision;
                return this;
            }

            public Builder supportsTools(boolean supportsTools) {
                this.supportsTools = supportsTools;
                return this;
            }

            public Builder metadata(Map<String, Object> metadata) {
                this.metadata = metadata != null ? metadata : Map.of();
                return this;
            }

            public ModelInfo build() {
                return new ModelInfo(
                        id,
                        name,
                        Optional.ofNullable(description),
                        contextWindow,
                        maxTokens,
                        supportsVision,
                        supportsTools,
                        metadata
                );
            }
        }
    }
}
