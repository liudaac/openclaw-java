package openclaw.tools.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;

import java.util.*;

/**
 * Cleans JSON schemas for Google Gemini compatibility.
 *
 * <p>Cloud Code Assist API rejects a subset of JSON Schema keywords.
 * This utility scrubs/normalizes tool schemas to keep Gemini happy.</p>
 *
 * <p>Ported from TypeScript: src/agents/schema/clean-for-gemini.ts</p>
 *
 * @author OpenClaw Team
 * @version 2026.3.30
 * @since 2026.3.28
 */
public class GeminiSchemaCleaner {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * Keywords that Cloud Code Assist API rejects.
     */
    public static final Set<String> GEMINI_UNSUPPORTED_SCHEMA_KEYWORDS = Set.of(
            "patternProperties",
            "additionalProperties",
            "$schema",
            "$id",
            "$ref",
            "$defs",
            "definitions",
            "examples",
            "minLength",
            "maxLength",
            "minimum",
            "maximum",
            "multipleOf",
            "pattern",
            "format",
            "minItems",
            "maxItems",
            "uniqueItems",
            "minProperties",
            "maxProperties"
    );

    /**
     * Cleans a schema for Gemini compatibility.
     * This is the main entry point.
     */
    public static JsonNode cleanSchema(JsonNode schema) {
        if (schema == null || !schema.isObject()) {
            return schema;
        }

        return cleanSchemaInternal((ObjectNode) schema);
    }

    /**
     * Cleans a schema string for Gemini compatibility.
     */
    public static String cleanSchema(String schemaJson) throws Exception {
        JsonNode schema = OBJECT_MAPPER.readTree(schemaJson);
        JsonNode cleaned = cleanSchema(schema);
        return OBJECT_MAPPER.writeValueAsString(cleaned);
    }

    /**
     * Internal method to clean schema object.
     */
    private static JsonNode cleanSchemaInternal(ObjectNode obj) {
        ObjectNode cleaned = OBJECT_MAPPER.createObjectNode();

        Iterator<Map.Entry<String, JsonNode>> fields = obj.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            String key = entry.getKey();
            JsonNode value = entry.getValue();

            // Skip unsupported keywords
            if (GEMINI_UNSUPPORTED_SCHEMA_KEYWORDS.contains(key)) {
                continue;
            }

            // Handle nested objects
            if (value.isObject()) {
                cleaned.set(key, cleanSchemaInternal((ObjectNode) value));
            }
            // Handle arrays
            else if (value.isArray()) {
                ArrayNode cleanedArray = OBJECT_MAPPER.createArrayNode();
                for (JsonNode item : value) {
                    if (item.isObject()) {
                        cleanedArray.add(cleanSchemaInternal((ObjectNode) item));
                    } else {
                        cleanedArray.add(item);
                    }
                }
                cleaned.set(key, cleanedArray);
            }
            // Copy other values as-is
            else {
                cleaned.set(key, value);
            }
        }

        return cleaned;
    }
}
