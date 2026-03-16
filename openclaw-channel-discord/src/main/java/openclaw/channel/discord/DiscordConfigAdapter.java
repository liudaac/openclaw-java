package openclaw.channel.discord;

import openclaw.sdk.channel.ChannelConfigAdapter;
import openclaw.sdk.channel.ChannelConfigSchema;
import openclaw.sdk.channel.ConfigUiHint;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Discord configuration adapter.
 */
public class DiscordConfigAdapter implements ChannelConfigAdapter<DiscordChannelPlugin.DiscordAccount> {

    @Override
    public CompletableFuture<ConfigValidationResult> validate(Map<String, Object> config) {
        return CompletableFuture.supplyAsync(() -> {
            List<String> errors = new java.util.ArrayList<>();

            if (!config.containsKey("botToken")) {
                errors.add("botToken is required");
            }

            return errors.isEmpty()
                    ? ConfigValidationResult.success()
                    : ConfigValidationResult.failure(errors);
        });
    }

    @Override
    public CompletableFuture<Optional<DiscordChannelPlugin.DiscordAccount>> parse(Map<String, Object> config) {
        return CompletableFuture.supplyAsync(() -> {
            String token = config.get("botToken").toString();
            String username = config.getOrDefault("botUsername", "").toString();
            Optional<String> webhook = Optional.ofNullable(config.get("webhookUrl")).map(Object::toString);

            return Optional.of(new DiscordChannelPlugin.DiscordAccount(token, username, webhook));
        });
    }

    @Override
    public ChannelConfigSchema getSchema() {
        Map<String, Object> schema = Map.of(
                "type", "object",
                "properties", Map.of(
                        "botToken", Map.of(
                                "type", "string",
                                "description", "Discord bot token"
                        ),
                        "botUsername", Map.of(
                                "type", "string",
                                "description", "Bot username"
                        )
                ),
                "required", List.of("botToken")
        );

        return new ChannelConfigSchema(schema, Map.of());
    }
}
