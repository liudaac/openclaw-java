package openclaw.sdk.tool;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Agent tool interface.
 *
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public interface AgentTool {

    /**
     * Gets the tool name.
     *
     * @return the name
     */
    String getName();

    /**
     * Gets the tool description.
     *
     * @return the description
     */
    String getDescription();

    /**
     * Gets the tool parameters schema.
     *
     * @return the parameters
     */
    ToolParameters getParameters();

    /**
     * Executes the tool.
     *
     * @param context the execution context
     * @return the result
     */
    CompletableFuture<ToolResult> execute(ToolExecuteContext context);

    /**
     * Tool parameters.
     *
     * @param type the parameter type (usually "object")
     * @param properties the parameter properties
     * @param required the required parameter names
     */
    record ToolParameters(
            String type,
            Map<String, PropertySchema> properties,
            java.util.List<String> required
    ) {

        /**
         * Creates a builder for ToolParameters.
         *
         * @return a new builder
         */
        public static Builder builder() {
            return new Builder();
        }

        /**
         * Builder for ToolParameters.
         */
        public static class Builder {
            private String type = "object";
            private Map<String, PropertySchema> properties = Map.of();
            private java.util.List<String> required = List.of();

            public Builder type(String type) {
                this.type = type;
                return this;
            }

            public Builder properties(Map<String, PropertySchema> properties) {
                this.properties = properties != null ? properties : Map.of();
                return this;
            }

            public Builder required(java.util.List<String> required) {
                this.required = required != null ? required : List.of();
                return this;
            }

            public ToolParameters build() {
                return new ToolParameters(type, properties, required);
            }
        }
    }

    /**
     * Property schema.
     *
     * @param type the property type
     * @param description the description
     * @param enumValues the enum values if applicable
     * @param items the items schema if array type
     */
    record PropertySchema(
            String type,
            String description,
            java.util.List<String> enumValues,
            PropertySchema items
    ) {

        /**
         * Creates a string property.
         *
         * @param description the description
         * @return the schema
         */
        public static PropertySchema string(String description) {
            return new PropertySchema("string", description, null, null);
        }

        /**
         * Creates an integer property.
         *
         * @param description the description
         * @return the schema
         */
        public static PropertySchema integer(String description) {
            return new PropertySchema("integer", description, null, null);
        }

        /**
         * Creates a boolean property.
         *
         * @param description the description
         * @return the schema
         */
        public static PropertySchema boolean_(String description) {
            return new PropertySchema("boolean", description, null, null);
        }

        /**
         * Creates an enum property.
         *
         * @param description the description
         * @param values the enum values
         * @return the schema
         */
        public static PropertySchema enum_(String description, java.util.List<String> values) {
            return new PropertySchema("string", description, values, null);
        }

        /**
         * Creates an array property.
         *
         * @param description the description
         * @param items the items schema
         * @return the schema
         */
        public static PropertySchema array(String description, PropertySchema items) {
            return new PropertySchema("array", description, null, items);
        }
    }
}
