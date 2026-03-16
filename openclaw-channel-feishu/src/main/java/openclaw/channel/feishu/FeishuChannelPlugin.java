package openclaw.channel.feishu;

import openclaw.sdk.channel.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Feishu (Lark) channel plugin implementation.
 *
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public class FeishuChannelPlugin implements ChannelPlugin<FeishuChannelPlugin.FeishuAccount, Void, Void> {

    private FeishuConfigAdapter configAdapter;
    private FeishuOutboundAdapter outboundAdapter;

    @Override
    public ChannelId getId() {
        return ChannelId.FEISHU;
    }

    @Override
    public ChannelMeta getMeta() {
        return ChannelMeta.builder()
                .name("Feishu")
                .description("Feishu (Lark) messaging platform")
                .icon("feishu")
                .color("#3370FF")
                .docsUrl("https://docs.openclaw.ai/channels/feishu")
                .tags(List.of("messaging", "enterprise", "china"))
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
                .supportsTyping(true)
                .supportsReactions(true)
                .supportsThreads(true)
                .supportsEditing(true)
                .supportsDeleting(true)
                .supportsMarkdown(true)
                .supportsMentions(true)
                .supportsGroups(true)
                .supportsDMs(true)
                .supportsPolling(true)
                .build();
    }

    @Override
    public ChannelConfigAdapter<FeishuAccount> getConfigAdapter() {
        if (configAdapter == null) {
            configAdapter = new FeishuConfigAdapter();
        }
        return configAdapter;
    }

    @Override
    public Optional<ChannelOutboundAdapter> getOutboundAdapter() {
        if (outboundAdapter == null) {
            outboundAdapter = new FeishuOutboundAdapter();
        }
        return Optional.of(outboundAdapter);
    }

    @Override
    public Optional<ChannelGroupAdapter> getGroupAdapter() {
        return Optional.of(new FeishuGroupAdapter());
    }

    @Override
    public Optional<ChannelSecurityAdapter<FeishuAccount>> getSecurityAdapter() {
        return Optional.of(new FeishuSecurityAdapter());
    }

    @Override
    public Optional<ChannelMentionAdapter> getMentionAdapter() {
        return Optional.of(new FeishuMentionAdapter());
    }

    @Override
    public Optional<ChannelDirectoryAdapter> getDirectoryAdapter() {
        return Optional.of(new FeishuDirectoryAdapter());
    }

    @Override
    public Optional<ChannelInboundAdapter> getInboundAdapter() {
        return Optional.of(new FeishuInboundAdapter(
                new FeishuAccount("", "", Optional.empty(), Optional.empty(), "https://open.feishu.cn/open-apis"),
                outboundAdapter != null ? outboundAdapter : new FeishuOutboundAdapter()
        ));
    }

    /**
     * Feishu account.
     *
     * @param appId the app ID
     * @param appSecret the app secret
     * @param encryptKey the encrypt key for webhook
     * @param verificationToken the verification token
     * @param apiUrl the API URL
     */
    public record FeishuAccount(
            String appId,
            String appSecret,
            Optional<String> encryptKey,
            Optional<String> verificationToken,
            String apiUrl
    ) {
    }
}
