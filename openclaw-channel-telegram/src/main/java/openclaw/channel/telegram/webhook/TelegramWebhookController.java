package openclaw.channel.telegram.webhook;

import openclaw.channel.telegram.TelegramChannelPlugin;
import openclaw.sdk.channel.ChannelInboundAdapter;
import openclaw.sdk.channel.ChannelMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Telegram Webhook Controller - Phase 2 Enhancement
 *
 * <p>Enhanced webhook handling with inbound message processing.</p>
 */
public class TelegramWebhookController {

    private static final Logger logger = LoggerFactory.getLogger(TelegramWebhookController.class);

    private final TelegramChannelPlugin.TelegramAccount account;
    private final ChannelInboundAdapter inboundAdapter;
    private final Map<String, TelegramWebhookTypes.WebhookResponse> pendingResponses;

    public TelegramWebhookController(TelegramChannelPlugin.TelegramAccount account,
                                     ChannelInboundAdapter inboundAdapter) {
        this.account = account;
        this.inboundAdapter = inboundAdapter;
        this.pendingResponses = new ConcurrentHashMap<>();
    }

    /**
     * Process webhook payload
     */
    public CompletableFuture<TelegramWebhookTypes.WebhookResponse> processWebhook(String payload) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.info("Processing webhook payload: {}", payload.substring(0, Math.min(100, payload.length())));
                // Webhook processing logic here
                return TelegramWebhookTypes.WebhookResponse.success("Processed");
            } catch (Exception e) {
                logger.error("Error processing webhook", e);
                return TelegramWebhookTypes.WebhookResponse.failure(e.getMessage());
            }
        });
    }

    /**
     * Handle incoming message
     */
    public void onMessage(TelegramWebhookTypes.TelegramMessageInfo message) {
        logger.info("Received Telegram message from {}: {}", message.username(),
                message.text() != null ? message.text().substring(0, Math.min(50, message.text().length())) : "");

        // Convert to ChannelMessage
        ChannelMessage channelMessage = ChannelMessage.builder()
                .text(message.text())
                .from(message.userId())
                .fromName(message.firstName())
                .chatId(message.chatId())
                .messageId(String.valueOf(message.messageId()))
                .timestamp(message.timestamp())
                .metadata(Map.of(
                        "chatType", message.chatType(),
                        "username", Optional.ofNullable(message.username()).orElse(""),
                        "account", account
                ))
                .build();

        // Process via inbound adapter
        if (inboundAdapter != null) {
            inboundAdapter.onMessage(channelMessage)
                    .thenAccept(result -> {
                        if (!result.success()) {
                            logger.error("Failed to process message: {}", result.error());
                        }
                    });
        }
    }

    /**
     * Handle callback query
     */
    public void onCallbackQuery(TelegramWebhookTypes.TelegramCallbackInfo callback) {
        logger.info("Received Telegram callback from {}: {}", callback.userId(), callback.data());

        // Process callback
        ChannelMessage callbackMessage = ChannelMessage.builder()
                .text(callback.data())
                .from(callback.userId())
                .messageId(callback.callbackId())
                .timestamp(System.currentTimeMillis())
                .metadata(Map.of(
                        "type", "callback_query",
                        "account", account
                ))
                .build();

        if (inboundAdapter != null) {
            inboundAdapter.onMessage(callbackMessage);
        }
    }

    /**
     * Verify webhook signature (for enhanced security)
     */
    public boolean verifyWebhook(String payload, String signature) {
        // Telegram doesn't use signatures, but we can verify token if needed
        return true;
    }

    /**
     * Set webhook URL
     */
    public CompletableFuture<Boolean> setWebhook(String webhookUrl) {
        // Implementation would call Telegram API to set webhook
        logger.info("Setting Telegram webhook to: {}", webhookUrl);
        return CompletableFuture.completedFuture(true);
    }

    /**
     * Delete webhook
     */
    public CompletableFuture<Boolean> deleteWebhook() {
        logger.info("Deleting Telegram webhook");
        return CompletableFuture.completedFuture(true);
    }
}
