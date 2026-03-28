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
 * @version 2026.3.28
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
     * Schema meta keys to preserve.
     */
    public static final Set<String> SCHEMA_META_KEYS = Set.of("description", "title", "default");

    /**
     * Cleans a schema for Gemini compatibility.
     *
     * @param schema the schema to clean
     * @return the cleaned schema
     */
    public static JsonNode cleanSchema(JsonNode schema) {
        return cleanSchemaWithDefs(schema, null, null);
    }

    /**
     * Cleans a schema with definitions support.
     */
    private static JsonNode cleanSchemaWithDefs(JsonNode schema, Map<String, JsonNode> defs, Set<String> refStack) {
        if (schema == null || !schema.isObject()) {
            return schema;
        }

        ObjectNode obj = (ObjectNode) schema.deepCopy();
        Map<String, JsonNode> nextDefs = extendSchemaDefs(defs, obj);

        // Handle $ref
        if (obj.has("$ref")) {
            String refValue = obj.get("$ref").asText();
            if (refStack != null && refStack.contains(refValue)) {
                return OBJECT_MAPPER.createObjectNode();
            }

            JsonNode resolved = tryResolveLocalRef(refValue, nextDefs);
            if (resolved != null) {
                Set<String> nextRefStack = refStack != null ? new HashSet<>(refStack) : new HashSet<>();
                nextRefStack.add(refValue);

                JsonNode cleaned = cleanSchemaWithDefs(resolved, nextDefs, nextRefStack);
                if (cleaned == null || !cleaned.isObject()) {
                    return cleaned;
                }

                ObjectNode result = ((ObjectNode) cleaned).deepCopy();
                copySchemaMeta(obj, result);
                return result;
            }

            ObjectNode result = OBJECT_MAPPER.createObjectNode();
            copySchemaMeta(obj, result);
            return result;
        }

        // Handle anyOf/oneOf
        boolean hasAnyOf = obj.has("anyOf") && obj.get("anyOf").isArray();
        boolean hasOneOf = obj.has("oneOf") && obj.get("oneOf").isArray();

        ArrayNode cleanedAnyOf = null;
        ArrayNode cleanedOneOf = null;

        if (hasAnyOf) {
            cleanedAnyOf = OBJECT_MAPPER.createArrayNode();
            for (JsonNode variant : obj.get("anyOf")) {
                cleanedAnyOf.add(cleanSchemaWithDefs(variant, nextDefs, refStack));
            }

            SimplifiedResult simplified = simplifyUnionVariants(obj, cleanedAnyOf);
            if (simplified.simplified != null) {
                return simplified.simplified;
            }
            cleanedAnyOf = simplified.variants;
        }

        if (hasOneOf) {
            cleanedOneOf = OBJECT_MAPPER.createArrayNode();
            for (JsonNode variant : obj.get("oneOf")) {
                cleanedOneOf.add(cleanSchemaWithDefs(variant, nextDefs, refStack));
            }

            SimplifiedResult simplified = simplifyUnionVariants(obj, cleanedOneOf);
            if (simplified.simplified != null) {
                return simplified.simplified;
            }
            cleanedOneOf = simplified.variants;
        }

        // Clean properties
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

            // Handle const -> enum
            if (key.equals("const")) {
                ArrayNode enumArray = OBJECT_MAPPER.createArrayNode();
                enumArray.add(value);
                cleaned.set("enum", enumArray);
                continue;
            }

            // Skip empty required arrays
            if (key.equals("required") && value.isArray() && value.isEmpty()) {
                continue;
            }

            // Skip type when anyOf/oneOf present
            if (key.equals("type") && (hasAnyOf || hasOneOf)) {
                continue;
            }

            // Handle type array (remove null)
            if (key.equals("type") && value.isArray()) {
                ArrayNode types = (ArrayNode) value;
                ArrayNode nonNullTypes = OBJECT_MAPPER.createArrayNode();
                for (JsonNode type : types) {
                    if (!type.asText().equals("null")) {
                        nonNullTypes.add(type);
                    }
                }
                if (nonNullTypes.size() == 1) {
                    cleaned.set("type", nonNullTypes.get(0));
                } else if (nonNullTypes.size() > 1) {
                    cleaned.set("type", nonNullTypes);
                }
                continue;
            }

            // Handle properties
            if (key.equals("properties") && value.isObject()) {
                ObjectNode props = (ObjectNode) value;
                ObjectNode cleanedProps = OBJECT_MAPPER.createObjectNode();
                Iterator<Map.Entry<String, JsonNode>> propFields = props.fields();
                while (propFields.hasNext()) {
                    Map.Entry<String, JsonNode> prop = propFields.next();
                    cleanedProps.set(prop.getKey(), cleanSchemaWithDefs(prop.getValue(), nextDefs, refStack));
                }
                cleaned.set("properties", cleanedProps);
            }
            // Handle items
            else if (key.equals("items") && value != null) {
                if (value.isArray()) {
                    ArrayNode cleanedItems = OBJECT_MAPPER.createArrayNode();
                    for (JsonNode item : value) {
                        cleanedItems.add(cleanSchemaWithDefs(item, nextDefs, refStack));
                    }
                    cleaned.set("items", cleanedItems);
                } else if (value.isObject()) {
                    cleaned.set("items", cleanSchemaWithDefs(value, nextDefs, refStack));
                } else {
                    cleaned.set("items", value);
                }
            }
            // Handle anyOf/oneOf/allOf
            else if (key.equals("anyOf") && value.isArray()) {
                cleaned.set("anyOf", cleanedAnyOf != null ? cleanedAnyOf : cleanArray(value, nextDefs,