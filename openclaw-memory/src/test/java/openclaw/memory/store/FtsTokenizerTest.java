package openclaw.memory.store;

import openclaw.memory.config.MemoryConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for FTS5 tokenizer configuration
 */
public class FtsTokenizerTest {

    @Test
    void testDefaultTokenizerIsPorter() {
        // Default tokenizer should be porter
        MemoryConfig.FtsConfig ftsConfig = new MemoryConfig.FtsConfig();
        assertEquals("porter", ftsConfig.getTokenizer());
    }

    @Test
    void testFtsEnabledByDefault() {
        // FTS should be enabled by default
        MemoryConfig.FtsConfig ftsConfig = new MemoryConfig.FtsConfig();
        assertTrue(ftsConfig.isEnabled());
    }

    @Test
    void testFtsOnlyDefaultIsFalse() {
        // FTS-only mode should be disabled by default
        MemoryConfig.FtsConfig ftsConfig = new MemoryConfig.FtsConfig();
        assertFalse(ftsConfig.isFtsOnly());
    }

    @Test
    void testTokenizerEnumValues() {
        // Verify all tokenizer enum values
        assertEquals("porter", FtsTokenizer.PORTER.getValue());
        assertEquals("icu", FtsTokenizer.ICU.getValue());
        assertEquals("unicode61", FtsTokenizer.UNICODE61.getValue());
        assertEquals("trigram", FtsTokenizer.TRIGRAM.getValue());
    }

    @Test
    void testSetTokenizer() {
        // Test setting different tokenizers
        MemoryConfig.FtsConfig ftsConfig = new MemoryConfig.FtsConfig();

        ftsConfig.setTokenizer("icu");
        assertEquals("icu", ftsConfig.getTokenizer());

        ftsConfig.setTokenizer("unicode61");
        assertEquals("unicode61", ftsConfig.getTokenizer());

        ftsConfig.setTokenizer("trigram");
        assertEquals("trigram", ftsConfig.getTokenizer());
    }

    @Test
    void testSetFtsOnly() {
        // Test setting FTS-only mode
        MemoryConfig.FtsConfig ftsConfig = new MemoryConfig.FtsConfig();

        ftsConfig.setFtsOnly(true);
        assertTrue(ftsConfig.isFtsOnly());

        ftsConfig.setFtsOnly(false);
        assertFalse(ftsConfig.isFtsOnly());
    }

    @Test
    void testSetFtsEnabled() {
        // Test enabling/disabling FTS
        MemoryConfig.FtsConfig ftsConfig = new MemoryConfig.FtsConfig();

        ftsConfig.setEnabled(false);
        assertFalse(ftsConfig.isEnabled());

        ftsConfig.setEnabled(true);
        assertTrue(ftsConfig.isEnabled());
    }

    @Test
    void testCjkSupportWithIcuTokenizer() {
        // ICU tokenizer is recommended for CJK text
        MemoryConfig.FtsConfig ftsConfig = new MemoryConfig.FtsConfig();
        ftsConfig.setTokenizer("icu");
        assertEquals("icu", ftsConfig.getTokenizer());

        // ICU tokenizer supports CJK (Chinese, Japanese, Korean) text segmentation
        // This is important for proper full-text search in Asian languages
    }

    @Test
    void testTrigramTokenizerForCjkFallback() {
        // Trigram tokenizer can be used as a fallback for CJK
        // when ICU extension is not available
        MemoryConfig.FtsConfig ftsConfig = new MemoryConfig.FtsConfig();
        ftsConfig.setTokenizer("trigram");
        assertEquals("trigram", ftsConfig.getTokenizer());
    }

    @Test
    void testMemoryConfigFtsIntegration() {
        // Test that MemoryConfig properly integrates FtsConfig
        MemoryConfig config = new MemoryConfig();
        assertNotNull(config.getFts());
        assertEquals("porter", config.getFts().getTokenizer());
        assertTrue(config.getFts().isEnabled());
        assertFalse(config.getFts().isFtsOnly());
    }

    @Test
    void testFtsTokenizerEnumUsage() {
        // Test using enum to set tokenizer
        MemoryConfig.FtsConfig ftsConfig = new MemoryConfig.FtsConfig();

        ftsConfig.setTokenizer(FtsTokenizer.ICU.getValue());
        assertEquals("icu", ftsConfig.getTokenizer());

        ftsConfig.setTokenizer(FtsTokenizer.TRIGRAM.getValue());
        assertEquals("trigram", ftsConfig.getTokenizer());

        ftsConfig.setTokenizer(FtsTokenizer.UNICODE61.getValue());
        assertEquals("unicode61", ftsConfig.getTokenizer());
    }
}
