package openclaw.channel.wecom;

import openclaw.sdk.channel.*;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * WeCom (WeChat Work) channel plugin.
 *
 * @author OpenClaw Team
 * @version 2026.3.30
 */
@Component
public class WecomChannelPlugin implements ChannelPlugin<WecomChannelPlugin.WecomAccount, Void, Void> {

    public static final ChannelId CHANNEL_ID = new ChannelId("wecom");

    private final WecomConfigAdapter configAdapter = new WecomConfigAdapter();
    private final WecomInboundAdapter inboundAdapter = new WecomInboundAdapter();
    private final WecomOutboundAdapter outboundAdapter = new WecomOutboundAdapter();
    private final WecomSecurityAdapter securityAdapter = new WecomSecurityAdapter();

    @Override
    public ChannelId getId() {
        return CHANNEL_ID;
    }

    @Override
    public ChannelMeta getMeta() {
        return ChannelMeta.builder()
                .name("WeCom")
                .description("WeChat Work (WeCom) channel adapter")
                .build();
    }

    @Override
    public ChannelCapabilities getCapabilities() {
        return ChannelCapabilities.builder()
                .supportsText(true)
                .supportsImages(true)
                .supportsFiles(true)
                .supportsTyping(true)
                .supportsGroups(true)
                .supportsDMs(true)
                .build();
    }

    @Override
    public ChannelConfigAdapter<WecomAccount> getConfigAdapter() {
        return configAdapter;
    }

    @Override
    public Optional<ChannelInboundAdapter> getInboundAdapter() {
        return Optional.of(inboundAdapter);
    }

    @Override
    public Optional<ChannelOutboundAdapter> getOutboundAdapter() {
        return Optional.of(outboundAdapter);
    }

    @Override
    public Optional<ChannelSecurityAdapter<WecomAccount>> getSecurityAdapter() {
        return Optional.of(securityAdapter);
    }

    /**
     * WeCom account configuration.
     */
    public record WecomAccount(
            String corpId,
            String agentId,
            String secret,
            String token,
            String encodingAesKey
    ) {
    }
}
