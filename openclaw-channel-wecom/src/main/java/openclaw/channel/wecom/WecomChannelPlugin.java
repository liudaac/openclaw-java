package openclaw.channel.wecom;

import openclaw.sdk.channel.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * WeCom (Enterprise WeChat) channel plugin implementation.
 *
 * @author OpenClaw Team
 * @version 2026.3.21
 * @since 2026.3.21
 */
public class WecomChannelPlugin implements ChannelPlugin<WecomChannelPlugin.WecomAccount, Void, Void> {

    @Override
    public ChannelId getId() {
        return ChannelId.WECOM;
    }

    @Override
    public ChannelMeta getMeta() {
        return ChannelMeta.builder()
                .name("WeCom")
                .description("Enterprise WeChat messaging platform")
                .icon("wecom")
                .color("#2BAD31")
                .docsUrl("https://docs.openclaw.ai/channels/wecom")
                .tags(List.of("messaging", "enterprise", "wechat"))
                .build();
    }

    @Override
    public ChannelCapabilities getCapabilities() {
        return ChannelCapabilities.builder()
                .supportsText(true)
                .supportsImages(true)
                .supportsFiles(true)
                .supportsVoice(true)
                .supportsVideo(true)
                .supportsTyping(false)
                .supportsReactions(false)
                .supportsThreads(false)
                .supportsEditing(false)
                .supportsDeleting(false)
                .supportsMarkdown(false)
                .supportsMentions(true)
                .supportsGroups(true)
                .supportsDMs(true)
                .supportsPolling(false)
                .build();
    }

    @Override
    public ChannelConfigAdapter<WecomAccount> getConfigAdapter() {
        return new WecomConfigAdapter();
    }

    @Override
    public Optional<ChannelOutboundAdapter> getOutboundAdapter() {
        return Optional.of(new WecomOutboundAdapter());
    }

    @Override
    public Optional<ChannelInboundAdapter> getInboundAdapter() {
        return Optional.of(new WecomInboundAdapter());
    }

    @Override
    public Optional<ChannelSecurityAdapter<WecomAccount>> getSecurityAdapter() {
        return Optional.of(new WecomSecurityAdapter());
    }

    /**
     * WeCom account configuration.
     *
     * @param corpId the corp ID
     * @param agentId the agent ID
     * @param secret the secret
     * @param token the token
     * @param encodingAesKey the encoding AES key
     */
    public record WecomAccount(
            String corpId,
            String agentId,
            String secret,
            String token,
            String encodingAesKey
    ) {}
}
