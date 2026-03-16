package openclaw.channel.telegram;

import openclaw.sdk.channel.ChannelInboundAdapter;
import openclaw.sdk.channel.ChannelMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Telegram Inbound Adapter - Phase 2 Enhancement
 *
 * <p>Handles incoming messages from Telegram webhook.</p>
 */
public class TelegramInboundAdapter implements ChannelInboundAdapter {

    private static final Logger logger = LoggerFactory.getLogger(TelegramInboundAdapter.class);

    private final TelegramChannelPlugin.TelegramAccount account;
    private final TelegramOutboundAdapter outboundAdapter;
    private final CopyOnWriteArrayList<Consumer<ChannelMessage>> messageHandlers;

    public TelegramInboundAdapter(TelegramChannelPlugin.TelegramAccount account,
                                  TelegramOutboundAdapter outboundAdapter) {
        this.account = account;
        this.outboundAdapter = outboundAdapter;
        this.messageHandlers = new CopyOnWriteArrayList<>();
    }

    @Override
    public CompletableFuture<ChannelInboundAdapter.ProcessResult> onMessage(ChannelMessage message) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.debug("Processing inbound message: {}", message.messageId());

                // Notify all handlers
                for (Consumer<ChannelMessage> handler : messageHandlers) {
                    try {
                        handler.accept(message);
                    } catch (Exception e) {
                        logger.error("Handler error: {}", e.getMessage());
                    }
                }

                // Auto-reply logic can be added here
                if (shouldAutoReply(message)) {
                    sendAutoReply(message);
                }

                return ChannelInboundAdapter.ProcessResult.success();

            } catch (Exception e) {
                logger.error("Failed to process message: {}", e.getMessage());
                return ChannelInboundAdapter.ProcessResult.failure(e.getMessage());
            }
        });
    }

    @Override
    public void onMessage(Consumer<ChannelMessage> handler) {
        messageHandlers.add(handler);
        logger.info("Registered message handler, total handlers: {}", messageHandlers.size());
    }

    @Override
    public void removeHandler(Consumer<ChannelMessage> handler) {
        messageHandlers.remove(handler);
        logger.info("Removed message handler, total handlers: {}", messageHandlers.size());
    }

    /**
     * Check if should auto-reply
     */
    private boolean shouldAutoReply(ChannelMessage message) {
        // Check if bot is mentioned
        String text = message.text();
        if (text == null) return false;

        // Reply if:
        // 1. Direct message (private chat)
        // 2. Bot is mentioned by username
        // 3. Message starts with /
        String chatType = (String) message.metadata().getOrDefault("chatType", "");
        if ("private".equals(chatType)) return true;

        if (text.startsWith("/")) return true;

        String botUsername = account.botUsername();
        if (botUsername != null && text.contains("@" + botUsername)) return true;

        return false;
    }

    /**
     * Send auto-reply
     */
    private void sendAutoReply(ChannelMessage message) {
        String replyText = "Received: " + message.text();

        outboundAdapter.sendText(account, message.chatId(), replyText, Optional.empty())
                .thenAccept(result -> {
                    if (result.success()) {
                        logger.debug("Auto-reply sent: {}", result.messageId());
                    } else {
                        logger.error("Failed to send auto-reply: {}", result.error());
                    }
                });
    }

    /**
     * Get account info
     */
    public TelegramChannelPlugin.TelegramAccount getAccount() {
        return account;
    }
}
