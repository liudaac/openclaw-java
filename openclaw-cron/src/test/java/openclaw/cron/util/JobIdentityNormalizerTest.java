package openclaw.cron.util;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for JobIdentityNormalizer.
 *
 * @author OpenClaw Team
 * @version 2026.4.8
 */
class JobIdentityNormalizerTest {

    @Test
    void testNormalizeNull() {
        assertNull(JobIdentityNormalizer.normalize(null));
    }

    @Test
    void testNormalizeEmptyMap() {
        Map<String, Object> map = new HashMap<>();
        Map<String, Object> result = JobIdentityNormalizer.normalize(map);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testNormalizeWithLegacyJobId() {
        Map<String, Object> map = new HashMap<>();
        map.put("jobId", "legacy-job-123");
        map.put("name", "Test Job");

        Map<String, Object> result = JobIdentityNormalizer.normalize(map);

        assertEquals("legacy-job-123", result.get("id"));
        assertEquals("legacy-job-123", result.get("jobId"));
        assertEquals("Test Job", result.get("name"));
    }

    @Test
    void testNormalizeWithBothFields() {
        // When both id and jobId exist, id takes precedence
        Map<String, Object> map = new HashMap<>();
        map.put("id", "new-id-456");
        map.put("jobId", "legacy-job-123");
        map.put("name", "Test Job");

        Map<String, Object> result = JobIdentityNormalizer.normalize(map);

        assertEquals("new-id-456", result.get("id"));
        assertEquals("legacy-job-123", result.get("jobId"));
    }

    @Test
    void testNormalizeWithOnlyId() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", "new-id-456");
        map.put("name", "Test Job");

        Map<String, Object> result = JobIdentityNormalizer.normalize(map);

        assertEquals("new-id-456", result.get("id"));
        assertNull(result.get("jobId"));
    }

    @Test
    void testNormalizeIdWithWhitespace() {
        String normalized = JobIdentityNormalizer.normalizeId("  job-with-spaces  ");
        assertEquals("job-with-spaces", normalized);
    }

    @Test
    void testNormalizeIdNull() {
        assertNull(JobIdentityNormalizer.normalizeId(null));
    }

    @Test
    void testNormalizeIdEmpty() {
        assertEquals("", JobIdentityNormalizer.normalizeId(""));
    }

    @Test
    void testNormalizeIdAlreadyClean() {
        String id = "clean-job-id";
        assertEquals(id, JobIdentityNormalizer.normalizeId(id));
    }

    @Test
    void testHasLegacyJobId() {
        Map<String, Object> withLegacy = new HashMap<>();
        withLegacy.put("jobId", "legacy");

        Map<String, Object> withBoth = new HashMap<>();
        withBoth.put("id", "new");
        withBoth.put("jobId", "legacy");

        Map<String, Object> withOnlyId = new HashMap<>();
        withOnlyId.put("id", "new");

        Map<String, Object> empty = new HashMap<>();

        assertTrue(JobIdentityNormalizer.hasLegacyJobId(withLegacy));
        assertFalse(JobIdentityNormalizer.hasLegacyJobId(withBoth)); // Has both, not legacy
        assertFalse(JobIdentityNormalizer.hasLegacyJobId(withOnlyId));
        assertFalse(JobIdentityNormalizer.hasLegacyJobId(empty));
        assertFalse(JobIdentityNormalizer.hasLegacyJobId(null));
    }

    @Test
    void testNormalizeNullJobId() {
        Map<String, Object> map = new HashMap<>();
        map.put("jobId", null);
        map.put("name", "Test");

        Map<String, Object> result = JobIdentityNormalizer.normalize(map);

        // Null jobId should not create id field
        assertFalse(result.containsKey("id"));
        assertNull(result.get("jobId"));
    }
}
