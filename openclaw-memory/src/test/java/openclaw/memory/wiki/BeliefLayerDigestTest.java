package openclaw.memory.wiki;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for BeliefLayerDigest.
 *
 * @author OpenClaw Team
 * @version 2026.4.8
 */
class BeliefLayerDigestTest {

    @Test
    void testDefaultConstructor() {
        BeliefLayerDigest digest = new BeliefLayerDigest();

        assertNotNull(digest.getDigestId());
        assertEquals(1.0, digest.getConfidence());
        assertEquals(0, digest.getReferenceCount());
        assertNotNull(digest.getCreatedAt());
        assertNotNull(digest.getUpdatedAt());
        assertTrue(digest.isHighConfidence());
    }

    @Test
    void testParameterizedConstructor() {
        BeliefLayerDigest digest = new BeliefLayerDigest(
                "session-1",
                "This is a belief",
                MemoryWikiService.DigestType.BELIEF
        );

        assertEquals("session-1", digest.getSessionKey());
        assertEquals("This is a belief", digest.getContent());
        assertEquals(MemoryWikiService.DigestType.BELIEF, digest.getType());
        assertNotNull(digest.getDigestId());
    }

    @Test
    void testConfidenceBounds() {
        BeliefLayerDigest digest = new BeliefLayerDigest();

        // Test upper bound
        digest.setConfidence(1.5);
        assertEquals(1.0, digest.getConfidence());
        assertTrue(digest.isHighConfidence());

        // Test lower bound
        digest.setConfidence(-0.5);
        assertEquals(0.0, digest.getConfidence());
        assertFalse(digest.isHighConfidence());

        // Test high confidence threshold
        digest.setConfidence(0.8);
        assertTrue(digest.isHighConfidence());

        digest.setConfidence(0.79);
        assertFalse(digest.isHighConfidence());
    }

    @Test
    void testReferenceCount() {
        BeliefLayerDigest digest = new BeliefLayerDigest();

        assertEquals(0, digest.getReferenceCount());

        digest.incrementReferenceCount();
        assertEquals(1, digest.getReferenceCount());

        digest.incrementReferenceCount();
        assertEquals(2, digest.getReferenceCount());
    }

    @Test
    void testTypeTransitions() {
        BeliefLayerDigest digest = new BeliefLayerDigest();

        digest.setType(MemoryWikiService.DigestType.OBSERVATION);
        assertEquals(MemoryWikiService.DigestType.OBSERVATION, digest.getType());

        digest.setType(MemoryWikiService.DigestType.REFLECTION);
        assertEquals(MemoryWikiService.DigestType.REFLECTION, digest.getType());

        digest.setType(MemoryWikiService.DigestType.SUMMARY);
        assertEquals(MemoryWikiService.DigestType.SUMMARY, digest.getType());
    }

    @Test
    void testToString() {
        BeliefLayerDigest digest = new BeliefLayerDigest(
                "session-1",
                "Content",
                MemoryWikiService.DigestType.BELIEF
        );

        String str = digest.toString();
        assertTrue(str.contains("BeliefLayerDigest"));
        assertTrue(str.contains(digest.getDigestId()));
        assertTrue(str.contains("BELIEF"));
    }
}
