package openclaw.plugin.sdk.bundle;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link BundledWebSearchIds}.
 *
 * @author OpenClaw Team
 * @version 2026.3.21
 */
class BundledWebSearchIdsTest {

    @Test
    void testListBundledIds() {
        List<String> ids = BundledWebSearchIds.listBundledIds();
        
        assertNotNull(ids);
        assertEquals(7, ids.size());
        assertTrue(ids.contains(BundledWebSearchIds.BRAVE));
        assertTrue(ids.contains(BundledWebSearchIds.FIRECRAWL));
        assertTrue(ids.contains(BundledWebSearchIds.GOOGLE));
        assertTrue(ids.contains(BundledWebSearchIds.MOONSHOT));
        assertTrue(ids.contains(BundledWebSearchIds.PERPLEXITY));
        assertTrue(ids.contains(BundledWebSearchIds.TAVILY));
        assertTrue(ids.contains(BundledWebSearchIds.XAI));
    }

    @Test
    void testBundledIdsIsUnmodifiable() {
        List<String> ids = BundledWebSearchIds.listBundledIds();
        
        assertThrows(UnsupportedOperationException.class, () -> ids.add("new-provider"));
        assertThrows(UnsupportedOperationException.class, () -> ids.remove(0));
    }

    @Test
    void testIsBundled() {
        assertTrue(BundledWebSearchIds.isBundled("brave"));
        assertTrue(BundledWebSearchIds.isBundled("google"));
        assertTrue(BundledWebSearchIds.isBundled("xai"));
        assertTrue(BundledWebSearchIds.isBundled("BRAVE")); // case insensitive
        assertTrue(BundledWebSearchIds.isBundled("Google")); // case insensitive
        
        assertFalse(BundledWebSearchIds.isBundled("unknown"));
        assertFalse(BundledWebSearchIds.isBundled(""));
        assertFalse(BundledWebSearchIds.isBundled(null));
    }

    @Test
    void testGetBundledCount() {
        assertEquals(7, BundledWebSearchIds.getBundledCount());
    }

    @Test
    void testConstants() {
        assertEquals("brave", BundledWebSearchIds.BRAVE);
        assertEquals("firecrawl", BundledWebSearchIds.FIRECRAWL);
        assertEquals("google", BundledWebSearchIds.GOOGLE);
        assertEquals("moonshot", BundledWebSearchIds.MOONSHOT);
        assertEquals("perplexity", BundledWebSearchIds.PERPLEXITY);
        assertEquals("tavily", BundledWebSearchIds.TAVILY);
        assertEquals("xai", BundledWebSearchIds.XAI);
    }
}
