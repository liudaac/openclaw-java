package openclaw.plugin.sdk.websearch.utils;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ValidationUtils.
 *
 * @author OpenClaw Team
 * @version 2026.3.18
 */
class ValidationUtilsTest {

    @Test
    void testReadStringParam() {
        Map<String, Object> params = Map.of("key", "value");
        assertEquals("value", ValidationUtils.readStringParam(params, "key"));
        assertNull(ValidationUtils.readStringParam(params, "missing"));
    }

    @Test
    void testReadStringParamRequired() {
        Map<String, Object> params = Map.of("key", "value");
        assertEquals("value", ValidationUtils.readStringParam(params, "key", true));

        assertThrows(IllegalArgumentException.class, () ->
                ValidationUtils.readStringParam(params, "missing", true));
    }

    @Test
    void testReadNumberParam() {
        Map<String, Object> params = Map.of("num", 42);
        assertEquals(42, ValidationUtils.readNumberParam(params, "num"));
        assertNull(ValidationUtils.readNumberParam(params, "missing"));
    }

    @Test
    void testNormalizeFreshness() {
        assertEquals("day", ValidationUtils.normalizeFreshness("day", "brave"));
        assertEquals("day", ValidationUtils.normalizeFreshness("24h", "brave"));
        assertEquals("week", ValidationUtils.normalizeFreshness("week", "brave"));
        assertEquals("month", ValidationUtils.normalizeFreshness("month", "brave"));
        assertEquals("year", ValidationUtils.normalizeFreshness("year", "brave"));
        assertNull(ValidationUtils.normalizeFreshness("invalid", "brave"));
        assertNull(ValidationUtils.normalizeFreshness(null, "brave"));
    }

    @Test
    void testNormalizeToIsoDate() {
        assertEquals("2024-03-18", ValidationUtils.normalizeToIsoDate("2024-03-18"));
        assertEquals("2024-03-18", ValidationUtils.normalizeToIsoDate("2024/03/18"));
        assertEquals("2024-03-18", ValidationUtils.normalizeToIsoDate("18-03-2024"));
        assertNull(ValidationUtils.normalizeToIsoDate("invalid"));
    }

    @Test
    void testResolveSearchCount() {
        assertEquals(5, ValidationUtils.resolveSearchCount(null, 5));
        assertEquals(3, ValidationUtils.resolveSearchCount(3, 5));
        assertEquals(1, ValidationUtils.resolveSearchCount(0, 5)); // min 1
        assertEquals(10, ValidationUtils.resolveSearchCount(100, 5)); // max 10
    }

    @Test
    void testResolveSiteName() {
        assertEquals("example.com", ValidationUtils.resolveSiteName("https://example.com/path"));
        assertEquals("example.com", ValidationUtils.resolveSiteName("https://www.example.com/"));
        assertNull(ValidationUtils.resolveSiteName("invalid"));
        assertNull(ValidationUtils.resolveSiteName(null));
    }

    @Test
    void testIsValidLanguageCode() {
        assertTrue(ValidationUtils.isValidLanguageCode("en"));
        assertTrue(ValidationUtils.isValidLanguageCode("DE"));
        assertFalse(ValidationUtils.isValidLanguageCode("english"));
        assertFalse(ValidationUtils.isValidLanguageCode("e"));
        assertFalse(ValidationUtils.isValidLanguageCode(null));
    }

    @Test
    void testIsValidCountryCode() {
        assertTrue(ValidationUtils.isValidCountryCode("US"));
        assertTrue(ValidationUtils.isValidCountryCode("de"));
        assertFalse(ValidationUtils.isValidCountryCode("USA"));
        assertFalse(ValidationUtils.isValidCountryCode("u"));
        assertFalse(ValidationUtils.isValidCountryCode(null));
    }

    @Test
    void testWrapWebContent() {
        String wrapped = ValidationUtils.wrapWebContent("Hello", "web_search");
        assertTrue(wrapped.contains("Hello"));
        assertTrue(wrapped.contains("web_search"));
        assertEquals("", ValidationUtils.wrapWebContent(null, "web_search"));
    }
}
