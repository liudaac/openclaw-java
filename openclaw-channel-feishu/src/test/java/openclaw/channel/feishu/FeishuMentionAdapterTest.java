package openclaw.channel.feishu;

import openclaw.sdk.channel.ChannelMentionAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for FeishuMentionAdapter.
 *
 * @author OpenClaw Team
 * @version 2026.3.25
 */
class FeishuMentionAdapterTest {

    private FeishuMentionAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new FeishuMentionAdapter();
    }

    @Test
    void testFormatMention_WithUserName() {
        String result = adapter.formatMention("ou_123", Optional.of("张三"));
        assertEquals("<at user_id=\"ou_123\">张三</at>", result);
    }

    @Test
    void testFormatMention_WithoutUserName() {
        String result = adapter.formatMention("ou_123", Optional.empty());
        assertEquals("<at user_id=\"ou_123\">User</at>", result);
    }

    @Test
    void testParseMentions_SingleMention() {
        String text = "Hello <at user_id=\"ou_123\">张三</at>, how are you?";
        List<ChannelMentionAdapter.ParsedMention> mentions = adapter.parseMentions(text);

        assertEquals(1, mentions.size());
        ChannelMentionAdapter.ParsedMention mention = mentions.get(0);
        assertEquals("ou_123", mention.userId());
        assertEquals(Optional.of("张三"), mention.userName());
    }

    @Test
    void testParseMentions_MultipleMentions() {
        String text = "<at user_id=\"ou_123\">张三</at> and <at user_id=\"ou_456\">李四</at> are here";
        List<ChannelMentionAdapter.ParsedMention> mentions = adapter.parseMentions(text);

        assertEquals(2, mentions.size());
        assertEquals("ou_123", mentions.get(0).userId());
        assertEquals("ou_456", mentions.get(1).userId());
    }

    @Test
    void testParseMentions_NoMentions() {
        String text = "Hello everyone, how are you?";
        List<ChannelMentionAdapter.ParsedMention> mentions = adapter.parseMentions(text);

        assertTrue(mentions.isEmpty());
    }

    @Test
    void testParseMentions_EmptyText() {
        String text = "";
        List<ChannelMentionAdapter.ParsedMention> mentions = adapter.parseMentions(text);

        assertTrue(mentions.isEmpty());
    }

    @Test
    void testParseMentions_MentionPositions() {
        String text = "Start <at user_id=\"ou_123\">User</at> end";
        List<ChannelMentionAdapter.ParsedMention> mentions = adapter.parseMentions(text);

        assertEquals(1, mentions.size());
        ChannelMentionAdapter.ParsedMention mention = mentions.get(0);
        assertEquals(6, mention.startIndex());
        assertEquals(39, mention.endIndex());
    }

    @Test
    void testResolveMention() {
        CompletableFuture<Optional<String>> future = adapter.resolveMention("ou_123");
        Optional<String> result = future.join();

        // Feishu requires API call to resolve, so should return empty
        assertFalse(result.isPresent());
    }
}
