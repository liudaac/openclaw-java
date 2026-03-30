package openclaw.channel.wecom;

import openclaw.sdk.channel.ChannelConfigAdapter;
import openclaw.sdk.channel.ChannelConfigSchema;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * WeCom channel configuration adapter.
 *
 * @author OpenClaw Team
 * @version 2026.3.30
 */
public class WecomConfigAdapter implements ChannelConfigAdapter<WecomChannelPlugin.WecomAccount> {

    @Override
    public CompletableFuture<ConfigValidationResult> validate(Map<String, Object> config) {
        return CompletableFuture.supplyAsync(() -> {
            java.util.List<String> errors = new java.util.ArrayList<>();

            String corpId = getString(config, "corpId");
            String agentId = getString(config, "agentId");
            String secret = getString(config, "secret");

            if (isBlank(corpId)) {
                errors.add("corpId is required");
            }
            if (isBlank(agentId)) {
                errors.add("agentId is required");
            }
            if (isBlank(secret)) {
                errors.add("secret is required");
            }

            if (errors.isEmpty()) {
                return ConfigValidationResult.success();
            }
            return ConfigValidationResult.failure(errors);
        });
    }

    @Override
    public CompletableFuture<Optional<WecomChannelPlugin.WecomAccount>> resolveAccount(Map<String, Object> config) {
        return CompletableFuture.supplyAsync(() -> {
            String corpId = getString(config, "corpId");
            String agentId = getString(config, "agentId");
            String secret = getString(config, "secret");
            String token = getString(config, "token");
            String encodingAesKey = getString(config, "encodingAesKey");

            if (isBlank(corpId) || isBlank(agentId) || isBlank(secret)) {
                return Optional.empty();
            }

            return Optional.of(new WecomChannelPlugin.WecomAccount(corpId, agentId, secret, token, encodingAesKey));
        });
    }

    @Override
    public ChannelConfigSchema getSchema() {
        return new ChannelConfigSchema(
                "WeCom Configuration",
                "Configure WeChat Work (WeCom) integration",
                Map.of(
                        "corpId", new ChannelConfigSchema.FieldSchema("Corp ID", "string", true, null),
                        "agentId", new ChannelConfigSchema.FieldSchema("Agent ID", "string", true, null),
                        "secret", new ChannelConfigSchema.FieldSchema("Secret", "string", true, null),
                        "token", new ChannelConfigSchema.FieldSchema("Token", "string", false, null),
                        "encodingAesKey", new ChannelConfigSchema.FieldSchema("Encoding AES Key", "string", false, null)
                )
        );
    }

    @Override
    public Map<String, Object> getDefaults() {
        return Map.of();
    }

    private String getString(Map<String, Object> config, String key) {
        Object value = config.get(key);
        return value != null ? value.toString() : null;
    }

    private boolean isBlank(String str) {
        return str == null || str.isBlank();
    }
}
