package openclaw.channel.feishu;

import openclaw.sdk.channel.ChannelConfigAdapter;
import openclaw.sdk.channel.ChannelConfigSchema;
import openclaw.sdk.channel.ConfigUiHint;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Feishu configuration adapter.
 *
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public class FeishuConfigAdapter implements ChannelConfigAdapter<FeishuChannelPlugin.FeishuAccount> {

    @Override
    public CompletableFuture<ConfigValidationResult> validate(Map<String, Object> config) {
        return CompletableFuture.supplyAsync(() -> {
            java.util.List<String> errors = new java.util.ArrayList<>();

            // Check required fields
            if (!config.containsKey("appId")) {
                errors.add("appId is required");
            }
            if (!config.containsKey("appSecret")) {
                errors.add("appSecret is required");
            }

            // Validate appId format
            if (config.containsKey("appId")) {
                String appId = config.get("appId").toString();
                if (appId.length() < 10) {
                    errors.add("Invalid appId format");
                }
            }

            if (errors.isEmpty()) {
                return ConfigValidationResult.success();
            }
            return ConfigValidationResult.failure(errors);
        });
    }

    @Override
    public CompletableFuture<Optional<FeishuChannelPlugin.FeishuAccount>> resolveAccount(Map<String, Object> config) {
        return validate(config).thenApply(result -> {
            if (!result.valid()) {
                return Optional.empty();
            }

            String appId = config.get("appId").toString();
            String appSecret = config.get("appSecret").toString();
            String apiUrl = config.getOrDefault("apiUrl", "https://open.feishu.cn").toString();

            Optional<String> encryptKey = Optional.ofNullable(config.get("encryptKey")).map(Object::toString);
            Optional<String> verificationToken = Optional.ofNullable(config.get("verificationToken")).map(Object::toString);


            return Optional.of(new FeishuChannelPlugin.FeishuAccount(
                    appId, appSecret, encryptKey, verificationToken, apiUrl
            ));
        });
    }

    @Override
    public ChannelConfigSchema getSchema() {
        Map<String, Object> schema = Map.of(
                "type", "object",
                "properties", Map.of(
                        "appId", Map.of(
                                "type", "string",
                                "description", "Feishu app ID"
                        ),
                        "appSecret", Map.of(
                                "type", "string",
                                "description", "Feishu app secret"
                        ),
                        "encryptKey", Map.of(
                                "type", "string",
                                "description", "Webhook encrypt key"
                        ),
                        "verificationToken", Map.of(
                                "type", "string",
                                "description", "Webhook verification token"
                        ),
                        "apiUrl", Map.of(
                                "type", "string",
                                "description", "Feishu API URL",
                                "default", "https://open.feishu.cn"
                        )
                ),
                "required", List.of("appId", "appSecret")
        );

        Map<String, ConfigUiHint> hints = Map.of(
                "appId", ConfigUiHint.builder()
                        .label("App ID")
                        .help("From Feishu Developer Console")
                        .build(),
                "appSecret", ConfigUiHint.builder()
                        .label("App Secret")
                        .help("From Feishu Developer Console")
                        .sensitive(true)
                        .build(),
                "encryptKey", ConfigUiHint.builder()
                        .label("Encrypt Key")
                        .help("For webhook security")
                        .sensitive(true)
                        .advanced(true)
                        .build()
        );

        return ChannelConfigSchema.withHints(schema, hints);
    }

    @Override
    public Map<String, Object> getDefaults() {
        return Map.of(
                "apiUrl", "https://open.feishu.cn"
        );
    }
}
