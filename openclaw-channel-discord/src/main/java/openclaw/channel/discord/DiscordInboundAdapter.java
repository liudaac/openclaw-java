package openclaw.channel.discord;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
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
 * Discord Inbound Adapter
 */
public class DiscordInboundAdapter extends ListenerAdapter implements ChannelInboundAdapter {

    private static final Logger logger = LoggerFactory.getLogger(DiscordInboundAdapter.class);

    private DiscordChannelPlugin.DiscordAccount account;
    private DiscordOutboundAdapter outboundAdapter;
    private JDA jda;
    private final CopyOnWriteArrayList<Consumer<ChannelMessage>> messageHandlers = new CopyOnWriteArrayList<>();

    public void initialize(DiscordChannelPlugin.DiscordAccount account,
                          DiscordOutboundAdapter outboundAdapter) {
        this.account = account;
        this.outboundAdapter = outboundAdapter;
        this.jda = outboundAdapter.getJda();

        // Register event listener
        jda.addEventListener(this);

        logger.info("Discord inbound adapter initialized");
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        // Ignore messages from bots (including ourselves)
        if (event.getAuthor().isBot()) {
            return;
        }

        Message message = event.getMessage();
        String content = message.getContentRaw();

        // Check if bot is mentioned
        boolean botMentioned = message.getMentionedUsers().stream()
                .anyMatch(user -> user.getId().equals(jda.getSelfUser().getId()));

        // Check if message starts with mention or command prefix
        boolean shouldProcess = botMentioned ||
                content.startsWith("/") ||
                event.getChannelType().equals(net.dv8tion.jda.api.entities.ChannelType.PRIVATE);

        if (!shouldProcess) {
            return;
        }

        logger.debug("Received Discord message from {} in {}: {}",
                event.getAuthor().getName(),
                event.getChannel().getName(),
                content.substring(0, Math.min(50, content.length())));

        // Convert to ChannelMessage
        ChannelMessage channelMessage = ChannelMessage.builder()
                .text(content)
                .from(event.getAuthor().getId())
                .fromName(event.getAuthor().getName())
                .chatId(event.getChannel().getId())
                .messageId(message.getId())
                .timestamp(System.currentTimeMillis())
                .metadata(Map.of(
                        "channelType", event.getChannelType().name(),
                        "guildId", event.getGuild() != null ? event.getGuild().getId() : "",
                        "botMentioned", botMentioned,
                        "account", account
                ))
                .build();

        // Process message
        onMessage(channelMessage).thenAccept(result -> {
            if (!result.success()) {
                logger.error("Failed to process Discord message: {}", result.error());
            }
        });
    }

    @Override
    public CompletableFuture<ProcessResult> onMessage(ChannelMessage message) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.debug("Processing Discord message: {}", message.messageId());

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
        logger.info("Registered Discord message handler, total: {}", messageHandlers.size());
    }

    @Override
    public void removeHandler(Consumer<ChannelMessage> handler) {
        messageHandlers.remove(handler);
        logger.info("Removed Discord message handler, total: {}", messageHandlers.size());
    }

    private boolean shouldAutoReply(ChannelMessage message) {
        String text = message.text();
        if (text == null || text.isEmpty()) {
            return false;
        }

        // Reply if:
        // 1. Direct message
        // 2. Bot is mentioned
        // 3. Message starts with /
        String channelType = (String) message.metadata().getOrDefault("channelType", "");
        if ("PRIVATE".equals(channelType)) {
            return true;
        }

        if (text.startsWith("/")) {
            return true;
        }

        Boolean botMentioned = (Boolean) message.metadata().getOrDefault("botMentioned", false);
        if (botMentioned) {
            return true;
        }

        return false;
    }

    private void sendAutoReply(ChannelMessage message) {
        String replyText = "Received: " + message.text();

        outboundAdapter.sendText(account, message.chatId(), replyText, Optional.of(message.messageId()))
                .thenAccept(result -> {
                    if (result.success()) {
                        logger.debug("Auto-reply sent: {}", result.messageId());
                    } else {
                        logger.error("Failed to send auto-reply: {}", result.error());
                    }
                });
    }

    public void shutdown() {
        if (jda != null) {
            jda.removeEventListener(this);
            logger.info("Discord inbound adapter shutdown");
        }
    }
}
