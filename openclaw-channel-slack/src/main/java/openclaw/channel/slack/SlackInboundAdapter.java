package openclaw.channel.slack;

import openclaw.sdk.channel.ChannelInboundAdapter;
import openclaw.sdk.channel.ChannelMessage;
import openclaw.sdk.channel.ProcessResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Slack Inbound Adapter
 */
public class SlackInboundAdapter implements ChannelInboundAdapter {

    private static final Logger logger = LoggerFactory.getLogger(SlackInboundAdapter.class);

    private SlackChannelPlugin.SlackAccount account;
    private SlackChannelPlugin.SlackConfig config;
    private SlackOutboundAdapter outboundAdapter;
    private final CopyOnWriteArrayList<Consumer<ChannelMessage>> messageHandlers = new CopyOnWriteArrayList<>();

    public void initialize(SlackChannelPlugin.SlackAccount account,
                          SlackChannelPlugin.SlackConfig config,
                          SlackOutboundAdapter outboundAdapter) {
        this.account = account;
        this.config = config;
        this.outboundAdapter = outboundAdapter;

        logger.info("Slack inbound adapter initialized");
    }

    @Override
    public CompletableFuture<ProcessResult> onMessage(ChannelMessage message) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.debug("Processing Slack message: {}", message.messageId());

                // Notify all handlers
                for (Consumer<ChannelMessage> handler : messageHandlers) {
                    try {
                        handler.accept(message);
                    } catch (Exception e) {
                        logger.error("Handler error: {}", e.getMessage());
                    }
                }

                // Auto-reply logic
                if (shouldAutoReply(message)) {
                    sendAutoReply(message);
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
        logger.info("Registered Slack message handler, total: {}", messageHandlers.size());
    }

    @Override
    public void removeHandler(Consumer<ChannelMessage> handler) {
        messageHandlers.remove(handler);
        logger.info("Removed Slack message handler, total: {}", messageHandlers.size());
    }

    /**
     * Process webhook payload
     */
    public CompletableFuture<ProcessResult> onWebhookPayload(Map<String, Object> payload) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String type = (String) payload.get("type");

                if ("url_verification".equals(type)) {
                    // URL verification for webhook setup
                    return ProcessResult.success();
                }

                if (!"event_callback".equals(type)) {
                    return ProcessResult.success();
                }

                @SuppressWarnings("unchecked")
                Map<String, Object> event = (Map<String, Object>) payload.get("event");
                if (event == null) {
                    return ProcessResult.failure("No event in payload");
                }

                String eventType = (String) event.get("type");

                switch (eventType) {
                    case "message":
                        return handleMessageEvent(event);
                    case "app_mention":
                        return handleAppMention(event);
                    default:
                        logger.debug("Unhandled Slack event type: {}", eventType);
                        return ProcessResult.success();
                }

            } catch (Exception e) {
                logger.error("Failed to process webhook: {}", e.getMessage());
                return ProcessResult.failure(e.getMessage());
            }
        });
    }

    private ProcessResult handleMessageEvent(Map<String, Object> event) {
        // Ignore bot messages
        if (event.containsKey("bot_id")) {
            return ProcessResult.success();
        }

        // Ignore message updates
        if (event.containsKey("subtype")) {
            return ProcessResult.success();
        }

        String text = (String) event.get("text");
        String user = (String) event.get("user");
        String channel = (String) event.get("channel");
        String ts = (String) event.get("ts");
        String threadTs = (String) event.get("thread_ts");

        if (text == null || text.isEmpty()) {
            return ProcessResult.success();
        }

        ChannelMessage message = ChannelMessage.builder()
                .text(text)
                .from(user)
                .chatId(channel)
                .messageId(ts)
                .timestamp(System.currentTimeMillis())
                .metadata(Map.of(
                        "thread_ts", threadTs != null ? threadTs : "",
                        "account", account
                ))
                .build();

        return onMessage(message).join();
    }

    private ProcessResult handleAppMention(Map<String, Object> event) {
        String text = (String) event.get("text");
        String user = (String) event.get("user");
        String channel = (String) event.get("channel");
        String ts = (String) event.get("ts");

        ChannelMessage message = ChannelMessage.builder()
                .text(text)
                .from(user)
                .chatId(channel)
                .messageId(ts)
                .timestamp(System.currentTimeMillis())
                .metadata(Map.of(
                        "type", "app_mention",
                        "account", account
                ))
                .build();

        return onMessage(message).join();
    }

    private boolean shouldAutoReply(ChannelMessage message) {
        String text = message.text();
        if (text == null || text.isEmpty()) {
            return false;
        }

        // Reply if:
        // 1. Direct message (IM)
        // 2. Bot is mentioned
        // 3. Message starts with /
        String channel = message.chatId();
        if (channel != null && channel.startsWith("D")) {
            // Direct message channel
            return true;
        }

        if (text.startsWith("/")) {
            return true;
        }

        if (text.contains("<@" + account.botUserId() + ">")) {
            return true;
        }

        return false;
    }

    private void sendAutoReply(ChannelMessage message) {
        String replyText = "Received: " + message.text();

        outboundAdapter.sendText(account, message.chatId(), replyText, java.util.Optional.of(message.messageId()))
                .thenAccept(result -> {
                    if (result.success()) {
                        logger.debug("Auto-reply sent: {}", result.messageId());
                    } else {
                        logger.error("Failed to send auto-reply: {}", result.error());
                    }
                });
    }

    public void shutdown() {
        logger.info("Slack inbound adapter shutdown");
    }
}
