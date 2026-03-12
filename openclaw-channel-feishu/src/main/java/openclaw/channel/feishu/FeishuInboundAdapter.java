package openclaw.channel.feishu;

import openclaw.sdk.channel.ChannelInboundAdapter;
import openclaw.sdk.channel.ChannelMessage;
import openclaw.sdk.channel.ProcessResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Feishu Inbound Adapter - Phase 2 Enhancement
 *
 * <p>Handles incoming messages from Feishu webhook.</p>
 */
public class FeishuInboundAdapter implements ChannelInboundAdapter {

    private static final Logger logger = LoggerFactory.getLogger(FeishuInboundAdapter.class);

    private final FeishuChannelPlugin.FeishuAccount account;
    private final FeishuOutboundAdapter outboundAdapter;
    private final CopyOnWriteArrayList<Consumer<ChannelMessage>> messageHandlers;

    public FeishuInboundAdapter(FeishuChannelPlugin.FeishuAccount account,
                                FeishuOutboundAdapter outboundAdapter) {
        this.account = account;
        this.outboundAdapter = outboundAdapter;
        this.messageHandlers = new CopyOnWriteArrayList<>();
    }

    @Override
    public CompletableFuture<ProcessResult> onMessage(ChannelMessage message) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.debug("Processing Feishu message: {}", message.messageId());

                // Notify all handlers
                for (Consumer<ChannelMessage> handler : messageHandlers) {
                    try {
                        handler.accept(message);
                    } catch (Exception e) {
                        logger.error("Handler error: {}", e.getMessage());
                    }
                }

                return ProcessResult.success();

            } catch (Exception e) {
                logger.error("Failed to process message: {}", e.getMessage());
                return ProcessResult.failure(e.getMessage());
            }
        });
    }

    @Override
    public void onMessage(Consumer<ChannelMessage> handler) {
        messageHandlers.add(handler);
        logger.info("Registered Feishu message handler, total: {}", messageHandlers.size());
    }

    @Override
    public void removeHandler(Consumer<ChannelMessage> handler) {
        messageHandlers.remove(handler);
        logger.info("Removed Feishu message handler, total: {}", messageHandlers.size());
    }

    /**
     * Process Feishu event
     */
    public CompletableFuture<ProcessResult> onEvent(FeishuEvent event) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.info("Processing Feishu event: {}", event.type());

                switch (event.type()) {
                    case "im.message.receive_v1":
                        return handleMessageEvent(event);
                    case "application.bot.menu_v6":
                        return handleMenuEvent(event);
                    case "card.action.trigger":
                        return handleCardAction(event);
                    default:
                        logger.debug("Unhandled event type: {}", event.type());
                        return ProcessResult.success();
                }

            } catch (Exception e) {
                logger.error("Failed to process event: {}", e.getMessage());
                return ProcessResult.failure(e.getMessage());
            }
        });
    }

    private ProcessResult handleMessageEvent(FeishuEvent event) {
        // Convert to ChannelMessage
        ChannelMessage message = ChannelMessage.builder()
                .text(event.content())
                .from(event.sender())
                .chatId(event.chatId())
                .messageId(event.messageId())
                .timestamp(System.currentTimeMillis())
                .metadata(Map.of(
                        "eventType", event.type(),
                        "account", account
                ))
                .build();

        return onMessage(message).join();
    }

    private ProcessResult handleMenuEvent(FeishuEvent event) {
        logger.info("Menu event from user: {}", event.sender());
        return ProcessResult.success();
    }

    private ProcessResult handleCardAction(FeishuEvent event) {
        logger.info("Card action from user: {}", event.sender());
        return ProcessResult.success();
    }

    /**
     * Get account info
     */
    public FeishuChannelPlugin.FeishuAccount getAccount() {
        return account;
    }

    /**
     * Feishu event record
     */
    public record FeishuEvent(
            String type,
            String sender,
            String chatId,
            String messageId,
            String content,
            Map<String, Object> extra
    ) {
    }
}
