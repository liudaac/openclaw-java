package openclaw.channel.telegram;

import openclaw.sdk.channel.ChannelConfigAdapter;
import openclaw.sdk.channel.ChannelConfigSchema;
import openclaw.sdk.channel.ConfigUiHint;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Telegram configuration adapter.
 *
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public class TelegramConfigAdapter implements ChannelConfigAdapter<TelegramChannelPlugin.TelegramAccount> {

    @Override
    public CompletableFuture<ConfigValidationResult> validate(Map<String, Object> config) {
        return CompletableFuture.supplyAsync(() -> {
            List<String> errors = new java.util.ArrayList<>();

            // Check required fields
            if (!config.containsKey("botToken")) {
                errors.add("botToken is required");
            }

            // Validate bot token format
            Object token = config.get("botToken");
            if (token != null) {
                String tokenStr = token.toString();
                if (!tokenStr.matches("\\d+:[A-Za-z0-9_-]+")) {
                    errors.add("Invalid bot token format");
                }
            }

            // Validate webhook URL if present
            if (config.containsKey("webhookUrl")) {
                String webhook = config.get("webhookUrl").toString();
                if (!webhook.startsWith("https://")) {
                    errors.add("Webhook URL must use HTTPS");
                }
            }

            if (errors.isEmpty()) {
                return ConfigValidationResult.success();
            }
            return ConfigValidationResult.failure(errors);
        });
    }

    @Override
    public CompletableFuture<Optional<TelegramChannelPlugin.TelegramAccount>> resolveAccount(Map<String, Object> config) {
        return validate(config).thenApply(result -> {
            if (!result.valid()) {
                return Optional.empty();
            }


            String token = config.get("botToken").toString();
            String username = config.getOrDefault("botUsername", "").toString();
            String apiUrl = config.getOrDefault("apiUrl", "https://api.telegram.org").toString();
            Optional<String> webhook = Optional.ofNullable(config.get("webhookUrl")).map(Object::toString);

            return Optional.of(new TelegramChannelPlugin.TelegramAccount(
                    token, username, webhook, apiUrl
            ));
        });
    }

    @Override
    public ChannelConfigSchema getSchema() {
        Map<String, Object> schema = Map.of(
                "type", "object",
                "properties", Map.of(
                        "botToken", Map.of(
                                "type", "string",
                                "description", "Telegram bot token"
                        ),
                        "botUsername", Map.of(
                                "type", "string",
                                "description", "Bot username"
                        ),
                        "webhookUrl", Map.of(
                                "type", "string",
                                "description", "Webhook URL for receiving updates"
                        ),
                        "apiUrl", Map.of(
                                "type", "string",
                                "description", "Telegram API URL",
                                "default", "https://api.telegram.org"
                        )
                ),
                "required", List.of("botToken")
        );

        Map<String, ConfigUiHint> hints = Map.of(
                "botToken", ConfigUiHint.builder()
                        .label("Bot Token")
                        .help("Get from @BotFather")
                        .sensitive(true)
                        .build(),
                "botUsername", ConfigUiHint.builder()
                        .label("Bot Username")
                        .help("Your bot's username")
                        .build(),
                "webhookUrl", ConfigUiHint.builder()
                        .label("Webhook URL")
                        .help("HTTPS URL for receiving updates")
                        .advanced(true)
                        .build()
        );

        return ChannelConfigSchema.withHints(schema, hints);
    }

    @Override
    public Map<String, Object> getDefaults() {
        return Map.of(
                "apiUrl", "https://api.telegram.org"
        );
    }
}
