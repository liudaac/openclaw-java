package openclaw.memory.wiki;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for PublicArtifact.
 *
 * @author OpenClaw Team
 * @version 2026.4.8
 */
class PublicArtifactTest {

    @Test
    void testDefaultConstructor() {
        PublicArtifact artifact = new PublicArtifact();

        assertNotNull(artifact.getArtifactId());
        assertTrue(artifact.isPublic());
        assertEquals(0, artifact.getAccessCount());
        assertNotNull(artifact.getCreatedAt());
        assertNotNull(artifact.getUpdatedAt());
        assertEquals(0, artifact.getSize());
    }

    @Test
    void testParameterizedConstructor() {
        PublicArtifact artifact = new PublicArtifact(
                "session-1",
                "config.json",
                "{\"key\": \"value\"}",
                MemoryWikiService.ArtifactType.CONFIG
        );

        assertEquals("session-1", artifact.getSessionKey());
        assertEquals("config.json", artifact.getName());
        assertEquals("{\"key\": \"value\"}", artifact.getContent());
        assertEquals(MemoryWikiService.ArtifactType.CONFIG, artifact.getType());
        assertNotNull(artifact.getArtifactId());
    }

    @Test
    void testAccessCount() {
        PublicArtifact artifact = new PublicArtifact();

        assertEquals(0, artifact.getAccessCount());

        artifact.incrementAccessCount();
        assertEquals(1, artifact.getAccessCount());

        artifact.incrementAccessCount();
        assertEquals(2, artifact.getAccessCount());
    }

    @Test
    void testSize() {
        PublicArtifact artifact = new PublicArtifact();

        assertEquals(0, artifact.getSize());

        artifact.setContent("Hello World");
        assertEquals(11, artifact.getSize());

        artifact.setContent("");
        assertEquals(0, artifact.getSize());
    }

    @Test
    void testPublicFlag() {
        PublicArtifact artifact = new PublicArtifact();

        assertTrue(artifact.isPublic());

        artifact.setPublic(false);
        assertFalse(artifact.isPublic());

        artifact.setPublic(true);
        assertTrue(artifact.isPublic());
    }

    @Test
    void testTypeTransitions() {
        PublicArtifact artifact = new PublicArtifact();

        artifact.setType(MemoryWikiService.ArtifactType.DOCUMENT);
        assertEquals(MemoryWikiService.ArtifactType.DOCUMENT, artifact.getType());

        artifact.setType(MemoryWikiService.ArtifactType.CODE);
        assertEquals(MemoryWikiService.ArtifactType.CODE, artifact.getType());

        artifact.setType(MemoryWikiService.ArtifactType.DATA);
        assertEquals(MemoryWikiService.ArtifactType.DATA, artifact.getType());
    }

    @Test
    void testToString() {
        PublicArtifact artifact = new PublicArtifact(
                "session-1",
                "test.txt",
                "content",
                MemoryWikiService.ArtifactType.DOCUMENT
        );

        String str = artifact.toString();
        assertTrue(str.contains("PublicArtifact"));
        assertTrue(str.contains(artifact.getArtifactId()));
        assertTrue(str.contains("test.txt"));
    }
}
