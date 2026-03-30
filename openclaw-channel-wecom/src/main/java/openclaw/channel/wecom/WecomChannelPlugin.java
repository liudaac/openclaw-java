package openclaw.channel.wecom;

import openclaw.sdk.channel.ChannelId;
import openclaw.sdk.channel.ChannelPlugin;
import org.springframework.stereotype.Component;

/**
 * WeCom (WeChat Work) channel plugin.
 *
 * @author OpenClaw Team
 * @version 2026.3.30
 */
@Component
public class WecomChannelPlugin implements ChannelPlugin {

    public static final ChannelId CHANNEL_ID = new ChannelId("wecom");

    @Override
    public ChannelId getId() {
        return CHANNEL_ID;
    }

    @Override
    public String getName() {
        return "WeCom";
    }

    @Override
    public String getDescription() {
        return "WeChat Work (WeCom) channel adapter";
    }

    @Override
    public String getVersion() {
        return "2026.3.30";
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
