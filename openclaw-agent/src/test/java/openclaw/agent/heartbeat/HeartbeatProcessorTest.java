package openclaw.agent.heartbeat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for HeartbeatProcessor.
 *
 * @author OpenClaw Team
 * @version 2026.3.17
 */
class HeartbeatProcessorTest {

    private HeartbeatConfig config;
    private HeartbeatProcessor processor;

    @BeforeEach
    void setUp() {
        config = new HeartbeatConfig();
        config.setAckMaxChars(300);
        processor = new HeartbeatProcessor(config);
    }

    @Test
    void testIsHeartbeatContentEffectivelyEmpty_Null() {
        assertFalse(processor.isHeartbeatContentEffectivelyEmpty(null));
    }

    @Test
    void testIsHeartbeatContentEffectivelyEmpty_Empty() {
        assertTrue(processor.isHeartbeatContentEffectivelyEmpty(""));
    }

    @Test
    void testIsHeartbeatContentEffectivelyEmpty_OnlyComments() {
        String content = "# This is a comment\n\n# Another comment\n";
        assertTrue(processor.isHeartbeatContentEffectivelyEmpty(content));
    }

    @Test
    void testIsHeartbeatContentEffectivelyEmpty_WithTasks() {
        String content = "# Tasks\n- Check email\n- Review calendar";
        assertFalse(processor.isHeartbeatContentEffectivelyEmpty(content));
    }

    @Test
    void testStripHeartbeatToken_ExactMatch() {
        HeartbeatProcessor.StripResult result = processor.stripHeartbeatToken(
            HeartbeatConfig.HEARTBEAT_TOKEN,
            HeartbeatProcessor.StripMode.HEARTBEAT
        );
        assertTrue(result.shouldSkip());
    }

    @Test
    void testStripHeartbeatToken_WithContent() {
        String text = "Found an issue! " + HeartbeatConfig.HEARTBEAT_TOKEN;
        HeartbeatProcessor.StripResult result = processor.stripHeartbeatToken(
            text,
            HeartbeatProcessor.StripMode.HEARTBEAT
        );
        assertFalse(result.shouldSkip());
        assertEquals("Found an issue!", result.text());
    }

    @Test
    void testStripHeartbeatToken_InMarkdown() {
        String text = "**" + HeartbeatConfig.HEARTBEAT_TOKEN + "**";
        HeartbeatProcessor.StripResult result = processor.stripHeartbeatToken(
            text,
            HeartbeatProcessor.StripMode.HEARTBEAT
        );
        assertTrue(result.shouldSkip());
    }

    @Test
    void testStripHeartbeatToken_InHtml() {
        String text = "<b>" + HeartbeatConfig.HEARTBEAT_TOKEN + "</b>";
        HeartbeatProcessor.StripResult result = processor.stripHeartbeatToken(
            text,
            HeartbeatProcessor.StripMode.HEARTBEAT
        );
        assertTrue(result.shouldSkip());
    }

    @Test
    void testIsSilentReplyText_True() {
        assertTrue(processor.isSilentReplyText(HeartbeatConfig.SILENT_REPLY_TOKEN));
    }

    @Test
    void testIsSilentReplyText_False() {
        assertFalse(processor.isSilentReplyText("Hello world"));
    }

    @Test
    void testShouldSkipHeartbeatOnlyDelivery_OnlyToken() {
        assertTrue(processor.shouldSkipHeartbeatOnlyDelivery(
            HeartbeatConfig.HEARTBEAT_TOKEN,
            false
        ));
    }

    @Test
    void testShouldSkipHeartbeatOnlyDelivery_WithMedia() {
        assertFalse(processor.shouldSkipHeartbeatOnlyDelivery(
            HeartbeatConfig.HEARTBEAT_TOKEN,
            true
        ));
    }

    @Test
    void testShouldSkipHeartbeatOnlyDelivery_WithLongContent() {
        String longContent = HeartbeatConfig.HEARTBEAT_TOKEN + " " + "a".repeat(400);
        assertFalse(processor.shouldSkipHeartbeatOnlyDelivery(longContent, false));
    }
}
