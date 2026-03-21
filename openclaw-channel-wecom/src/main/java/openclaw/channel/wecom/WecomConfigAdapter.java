package openclaw.channel.wecom;

import openclaw.sdk.channel.ChannelConfigAdapter;
import openclaw.sdk.channel.ChannelConfigValidation;

import java.util.Map;

/**
 * WeCom channel configuration adapter.
 *
 * @author OpenClaw Team
 * @version 2026.3.21
 * @since 2026.3.21
 */
public class WecomConfigAdapter implements ChannelConfigAdapter<WecomChannelPlugin.WecomAccount> {

    @Override
    public WecomChannelPlugin.WecomAccount parse(Map<String, Object> config) {
        String corpId = getString(config, "corpId");
        String agentId = getString(config, "agentId");
        String secret = getString(config, "secret");
        String token = getString(config, "token");
        String encodingAesKey = getString(config, "encodingAesKey");

        return new WecomChannelPlugin.WecomAccount(corpId, agentId, secret, token, encodingAesKey);
    }

    @Override
    public ChannelConfigValidation validate(WecomChannelPlugin.WecomAccount account) {
        ChannelConfigValidation.Builder builder = ChannelConfigValidation.builder();

        if (isBlank(account.corpId())) {
            builder.addError("corpId", "Corp ID is required");
        }
        if (isBlank(account.agentId())) {
            builder.addError("agentId", "Agent ID is required");
        }
        if (isBlank(account.secret())) {
            builder.addError("secret", "Secret is required");
        }

        return builder.build();
    }

    @Override
    public Map<String, Object> serialize(WecomChannelPlugin.WecomAccount account) {
        return Map.of(
                "corpId", account.corpId(),
                "agentId", account.agentId(),
                "secret", mask(account.secret()),
                "token", mask(account.token()),
                "encodingAesKey", mask(account.encodingAesKey())
        );
    }

    private String getString(Map<String, Object> config, String key) {
        Object value = config.get(key);
        return value != null ? value.toString() : null;
    }

    private boolean isBlank(String str) {
        return str == null || str.isBlank();
    }

    private String mask(String str) {
        if (str == null || str.length() <= 4) {
            return "****";
        }
        return str.substring(0, 2) + "****" + str.substring(str.length() - 2);
    }
}
