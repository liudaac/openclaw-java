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
        Map<String, Object> schema = Map.of(
                "type", "object",
                "properties", Map.of(
                        "corpId", Map.of("type", "string", "description", "Corp ID"),
                        "agentId", Map.of("type", "string", "description", "Agent ID"),
                        "secret", Map.of("type", "string", "description", "Secret"),
                        "token", Map.of("type", "string", "description", "Token"),
                        "encodingAesKey", Map.of("type", "string", "description", "Encoding AES Key")
                ),
                "required", java.util.List.of("corpId", "agentId", "secret")
        );
        return ChannelConfigSchema.of(schema);
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
