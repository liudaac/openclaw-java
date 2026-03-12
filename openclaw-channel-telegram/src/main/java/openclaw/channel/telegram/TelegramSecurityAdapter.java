package openclaw.channel.telegram;

import openclaw.sdk.channel.ChannelSecurityAdapter;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;


/**
 * Telegram security adapter.
 *
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public class TelegramSecurityAdapter implements ChannelSecurityAdapter<TelegramChannelPlugin.TelegramAccount> {

    @Override
    public CompletableFuture<Optional<TelegramChannelPlugin.TelegramAccount>> authenticate(
            Map<String, Object> credentials) {
        // Telegram uses bot tokens, no additional auth needed
        String token = credentials.get("botToken").toString();
        String username = credentials.getOrDefault("botUsername", "").toString();
        
        return CompletableFuture.completedFuture(Optional.of(
                new TelegramChannelPlugin.TelegramAccount(
                        token,
                        username,
                        Optional.empty(),
                        "https://api.telegram.org"
                )
        ));
    }

    @Override
    public CompletableFuture<Boolean> authorize(
            TelegramChannelPlugin.TelegramAccount account,
            String action,
            String resource) {
        // Bot tokens have all permissions
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<WebhookValidationResult> validateWebhook(
            Map<String, String> headers,
            String body) {
        // Validate Telegram webhook signature
        // In production, verify X-Telegram-Bot-Api-Secret-Token
        return CompletableFuture.completedFuture(
                WebhookValidationResult.valid(null)
        );
    }
}
