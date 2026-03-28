package openclaw.tools.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for GeminiSchemaCleaner.
 *
 * <p>Ported from TypeScript: src/agents/schema/clean-for-gemini.test.ts</p>
 *
 * @author OpenClaw Team
 * @version 2026.3.28
 */
class GeminiSchemaCleanerTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void testRemovesUnsupportedKeywords() {
        String schema = """
            {
                "type": "object",
                "properties": {
                    "name": { "type": "string", "minLength": 1, "maxLength": 100 }
                },
                "patternProperties": { "^test": { "type": "string" } },
                "additionalProperties": false
            }
            """;

        JsonNode input = parse(schema);
        JsonNode result = GeminiSchemaCleaner.cleanSchema(input);

        assertTrue(result.has("type"));
        assertTrue(result.has("properties"));
        assertFalse(result.has("patternProperties"));
        assertFalse(result.has("additionalProperties"));

        JsonNode nameProp = result.get("properties").get("name");
        assertFalse(nameProp.has("minLength"));
        assertFalse(nameProp.has("maxLength"));
    }

    @Test
    void testConvertsConstToEnum() {
        String schema = """
            {
                "type": "object",
                "properties": {
                    "status": { "const": "active" }
                }
            }
            """;

        JsonNode input = parse(schema);
        JsonNode result = GeminiSchemaCleaner.cleanSchema(input);

        JsonNode status = result.get("properties").get("status");
        assertFalse(status.has("const"));
        assertTrue(status.has("enum"));
        assertEquals("active", status.get("enum").get(0).asText());
    }

    @Test
    void testRemovesEmptyRequiredArray() {
        String schema = """
            {
                "type": "object",
                "properties": {},
                "required": []
            }
            """;

        JsonNode input = parse(schema);
        JsonNode result = GeminiSchemaCleaner.cleanSchema(input);

        assertFalse(result.has("required"));
    }

    @Test
    void testKeepsNonEmptyRequiredArray() {
        String schema = """
            {
                "type": "object",
                "properties": {
                    "name": { "type": "string" }
                },
                "required": ["name"]
            }
            """;

        JsonNode input = parse(schema);
        JsonNode result = GeminiSchemaCleaner.cleanSchema(input);

        assertTrue(result.has("required"));
        assertEquals("name", result.get("required").get(0).asText());
    }

    @Test
    void testRemovesNullFromTypeArray() {
        String schema = """
            {
                "type": ["string", "null"]
            }
            """;

        JsonNode input = parse(schema);
        JsonNode result = GeminiSchemaCleaner.cleanSchema(input);

        assertEquals("string", result.get("type").asText());
    }

    @Test
    void testHandlesNestedObjects() {
        String schema = """
            {
                "type": "object",
                "properties": {
                    "user": {
                        "type": "object",
                        "properties": {
                            "name": { "type": "string", "minLength": 1 }
                        }
                    }
                }
            }
            """;

        JsonNode input = parse(schema);
        JsonNode result = GeminiSchemaCleaner.cleanSchema(input);

        JsonNode user = result.get("properties").get("user");
        JsonNode name = user.get("properties").get("name");
        assertFalse(name.has("minLength"));
    }

    @Test
    void testHandlesArrays() {
        String schema = """
            {
                "type": "object",
                "properties": {
                    "items": {
                        "type": "array",
                        "items": { "type": "string", "minLength": 1 }
                    }
                }
            }
            """;

        JsonNode input = parse(schema);
        JsonNode result = GeminiSchemaCleaner.cleanSchema(input);

        JsonNode items = result.get("properties").get("items");
        assertFalse(items.get("items").has("minLength"));
    }

    @Test
    void testPreservesDescription() {
        String schema = """
            {
                "type": "string",
                "description": "A test field",
                "minLength": 1
            }
            """;

        JsonNode input = parse(schema);
        JsonNode result = GeminiSchemaCleaner.cleanSchema(input);

        assertEquals("A test field", result.get("description").asText());
        assertFalse(result.has("minLength"));
    }

    private JsonNode parse(String json) {
        try {
            return OBJECT_MAPPER.readTree(json);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
