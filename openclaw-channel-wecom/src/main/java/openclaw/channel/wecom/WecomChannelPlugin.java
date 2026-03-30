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
        return new ChannelMeta("WeCom", "WeChat Work (WeCom) channel adapter", "2026.3.30");
    }

    @Override
    public ChannelCapabilities getCapabilities() {
        return new ChannelCapabilities(true, true, true, true, true, true, true, true, true);
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
