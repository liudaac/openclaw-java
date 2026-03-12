package openclaw.sdk;

import openclaw.sdk.channel.*;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for ChannelPlugin interfaces.
 */
class ChannelPluginTest {

    @Test
    void testChannelId() {
        assertThat(ChannelId.TELEGRAM.id()).isEqualTo("telegram");
        assertThat(ChannelId.FEISHU.id()).isEqualTo("feishu");
        assertThat(ChannelId.SLACK.id()).isEqualTo("slack");
    }

    @Test
    void testChannelCapabilitiesBuilder() {
        ChannelCapabilities caps = ChannelCapabilities.builder()
                .supportsText(true)
                .supportsImages(true)
                .supportsFiles(true)
                .supportsGroups(true)
                .supportsDMs(true)
                .build();

        assertThat(caps.supportsText()).isTrue();
        assertThat(caps.supportsImages()).isTrue();
        assertThat(caps.supportsFiles()).isTrue();
        assertThat(caps.supportsGroups()).isTrue();
        assertThat(caps.supportsDMs()).isTrue();
        assertThat(caps.supportsVoice()).isFalse();
    }

    @Test
    void testChannelMetaBuilder() {
        ChannelMeta meta = ChannelMeta.builder()
                .name("Telegram")
                .description("Telegram messaging")
                .icon("telegram-icon")
                .color("#0088cc")
                .docsUrl("https://docs.openclaw.ai/telegram")
                .build();

        assertThat(meta.name()).isEqualTo("Telegram");
        assertThat(meta.description()).isEqualTo("Telegram messaging");
        assertThat(meta.icon()).hasValue("telegram-icon");
        assertThat(meta.color()).hasValue("#0088cc");
    }

    @Test
    void testSendResult() {
        SendResult success = SendResult.success("msg-123");
        assertThat(success.success()).isTrue();
        assertThat(success.messageId()).hasValue("msg-123");
        assertThat(success.error()).isEmpty();

        SendResult failure = SendResult.failure("Network error");
        assertThat(failure.success()).isFalse();
        assertThat(failure.error()).hasValue("Network error");
    }

    @Test
    void testMediaUploadResult() {
        MediaUploadResult success = MediaUploadResult.successWithUrl("media-123", "https://example.com/media");
        assertThat(success.success()).isTrue();
        assertThat(success.mediaId()).hasValue("media-123");
        assertThat(success.url()).hasValue("https://example.com/media");
    }

    @Test
    void testConfigUiHintBuilder() {
        ConfigUiHint hint = ConfigUiHint.builder()
                .label("API Token")
                .help("Your API token")
                .sensitive(true)
                .advanced(false)
                .placeholder("Enter token...")
                .build();

        assertThat(hint.label()).hasValue("API Token");
        assertThat(hint.sensitive()).isTrue();
        assertThat(hint.advanced()).isFalse();
    }

    @Test
    void testChannelInfoBuilder() {
        ChannelInfo info = ChannelInfo.builder()
                .id("123")
                .name("Test Channel")
                .type(ChannelInfo.ChannelType.GROUP)
                .status(ChannelInfo.ChannelStatus.ACTIVE)
                .build();

        assertThat(info.id()).isEqualTo("123");
        assertThat(info.type()).isEqualTo(ChannelInfo.ChannelType.GROUP);
        assertThat(info.status()).isEqualTo(ChannelInfo.ChannelStatus.ACTIVE);
    }
}
