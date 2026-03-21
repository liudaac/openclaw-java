package openclaw.channel.telegram;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link AutoTopicLabelConfig}.
 *
 * @author OpenClaw Team
 * @version 2026.3.21
 */
class AutoTopicLabelConfigTest {

    @Test
    void testDisabled() {
        AutoTopicLabelConfig config = AutoTopicLabelConfig.disabled();
        assertFalse(config.isEnabled());
        assertNull(config.getPrompt());
    }

    @Test
    void testEnabled() {
        AutoTopicLabelConfig config = AutoTopicLabelConfig.enabled();
        assertTrue(config.isEnabled());
        assertNotNull(config.getPrompt());
    }

    @Test
    void testWithPrompt() {
        String customPrompt = "Custom prompt for testing";
        AutoTopicLabelConfig config = AutoTopicLabelConfig.withPrompt(customPrompt);
        assertTrue(config.isEnabled());
        assertEquals(customPrompt, config.getPrompt());
    }

    @Test
    void testResolveDirectPriority() {
        // Direct true takes priority
        AutoTopicLabelConfig config = AutoTopicLabelConfig.resolve(true, false);
        assertTrue(config.isEnabled());

        // Direct false takes priority
        config = AutoTopicLabelConfig.resolve(false, true);
        assertFalse(config.isEnabled());
    }

    @Test
    void testResolveFallbackToAccount() {
        // Direct null, account true
        AutoTopicLabelConfig config = AutoTopicLabelConfig.resolve(null, true);
        assertTrue(config.isEnabled());

        // Direct null, account false
        config = AutoTopicLabelConfig.resolve(null, false);
        assertFalse(config.isEnabled());
    }

    @Test
    void testResolveDefaultDisabled() {
        // Both null
        AutoTopicLabelConfig config = AutoTopicLabelConfig.resolve(null, null);
        assertFalse(config.isEnabled());
    }

    @Test
    void testResolveWithCustomPrompt() {
        String customPrompt = "Custom prompt";
        AutoTopicLabelConfig config = AutoTopicLabelConfig.resolve(true, false, customPrompt);
        assertTrue(config.isEnabled());
        assertEquals(customPrompt, config.getPrompt());
    }

    @Test
    void testResolveWithCustomPromptDisabled() {
        String customPrompt = "Custom prompt";
        AutoTopicLabelConfig config = AutoTopicLabelConfig.resolve(false, true, customPrompt);
        assertFalse(config.isEnabled());
    }

    @Test
    void testResolveWithEmptyCustomPrompt() {
        AutoTopicLabelConfig config = AutoTopicLabelConfig.resolve(true, false, "");
        assertTrue(config.isEnabled());
        assertNotNull(config.getPrompt()); // Should use default
    }

    @Test
    void testWithPromptNullThrows() {
        assertThrows(NullPointerException.class, () ->
                AutoTopicLabelConfig.withPrompt(null)
        );
    }

    @Test
    void testToString() {
        AutoTopicLabelConfig config = AutoTopicLabelConfig.enabled();
        String str = config.toString();
        assertTrue(str.contains("enabled=true"));
        assertTrue(str.contains("prompt=default"));
    }
}
