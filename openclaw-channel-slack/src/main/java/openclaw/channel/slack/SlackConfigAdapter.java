package openclaw.channel.slack;

import openclaw.sdk.channel.ChannelConfigAdapter;
import openclaw.sdk.channel.ChannelConfigSchema;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Slack configuration adapter.
 */
public class SlackConfigAdapter implements ChannelConfigAdapter<SlackChannelPlugin.SlackAccount> {

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
    public CompletableFuture<Optional<SlackChannelPlugin.SlackAccount>> resolveAccount(Map<String, Object> config) {
        return CompletableFuture.supplyAsync(() -> {
            String token = config.get("botToken").toString();
            String username = config.getOrDefault("botUsername", "").toString();
            Optional<String> webhook = Optional.ofNullable(config.get("webhookUrl")).map(Object::toString);

            return Optional.of(new SlackChannelPlugin.SlackAccount(token, username, webhook));
        });
    }

    @Override
    public ChannelConfigSchema getSchema() {
        Map<String, Object> schema = Map.of(
                "type", "object",
                "properties", Map.of(
                        "botToken", Map.of(
                                "type", "string",
                                "description", "Slack bot token"
                        )
                ),
                "required", List.of("botToken")
        );

        return new ChannelConfigSchema(schema, Optional.of(Map.of()));
    }

    @Override
    public Map<String, Object> getDefaults() {
        return Map.of();
    }
}
