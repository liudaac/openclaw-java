package openclaw.plugin.sdk.websearch;

import openclaw.plugin.sdk.annotation.PublicApi;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Web Search Tool Definition.
 * Defines the schema and execution logic for a web search tool.
 *
 * @author OpenClaw Team
 * @version 2026.3.18
 * @since 2026.3.0
 */
@PublicApi(since = "2026.3.0", stability = "stable",
           description = "Tool definition for web search")
public interface WebSearchToolDefinition {

    /**
     * Tool description for LLM.
     *
     * @return the description
     */
    @PublicApi(since = "2026.3.0")
    String getDescription();

    /**
     * JSON Schema for tool parameters.
     * Should follow JSON Schema format.
     *
     * @return the parameters schema
     */
    @PublicApi(since = "2026.3.0")
    Map<String, Object> getParameters();

    /**
     * Execute the search.
     *
     * @param args the arguments
     * @return future with result
     */
    @PublicApi(since = "2026.3.0")
    CompletableFuture<Map<String, Object>> execute(Map<String, Object> args);

    /**
     * Get the provider ID.
     *
     * @return the provider ID
     */
    @PublicApi(since = "2026.3.0")
    default String getProviderId() {
        return "unknown";
    }

    /**
     * Get the provider mode (if applicable).
     *
     * @return optional mode
     */
    default String getMode() {
        return null;
    }

    /**
     * Builder for creating tool definitions.
     */
    @PublicApi(since = "2026.3.0")
    static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class.
     */
    class Builder {
        private String description;
        private Map<String, Object> parameters;
        private java.util.function.Function<Map<String, Object>, CompletableFuture<Map<String, Object>>> executeFunction;
        private String providerId = "unknown";
        private String mode;

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder parameters(Map<String, Object> parameters) {
            this.parameters = parameters;
            return this;
        }

        public Builder execute(java.util.function.Function<Map<String, Object>, CompletableFuture<Map<String, Object>>> executeFunction) {
            this.executeFunction = executeFunction;
            return this;
        }

        public Builder providerId(String providerId) {
            this.providerId = providerId;
            return this;
        }

        public Builder mode(String mode) {
            this.mode = mode;
            return this;
        }

        public WebSearchToolDefinition build() {
            if (description == null || parameters == null || executeFunction == null) {
                throw new IllegalStateException("Description, parameters, and execute function are required");
            }

            return new WebSearchToolDefinition() {
                @Override
                public String getDescription() {
                    return description;
                }

                @Override
                public Map<String, Object> getParameters() {
                    return parameters;
                }

                @Override
                public CompletableFuture<Map<String, Object>> execute(Map<String, Object> args) {
                    return executeFunction.apply(args);
                }

                @Override
                public String getProviderId() {
                    return providerId;
                }

                @Override
                public String getMode() {
                    return mode;
                }
            };
        }
    }
}
