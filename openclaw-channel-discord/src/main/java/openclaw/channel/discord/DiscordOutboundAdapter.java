package openclaw.channel.discord;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.requests.GatewayIntent;
import openclaw.sdk.channel.ChannelMessage;
import openclaw.sdk.channel.ChannelOutboundAdapter;
import openclaw.sdk.channel.SendResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Discord Outbound Adapter
 */
public class DiscordOutboundAdapter implements ChannelOutboundAdapter {

    private static final Logger logger = LoggerFactory.getLogger(DiscordOutboundAdapter.class);

    private JDA jda;
    private DiscordChannelPlugin.DiscordAccount account;

    public void initialize(DiscordChannelPlugin.DiscordAccount account) throws Exception {
        this.account = account;

        this.jda = JDABuilder.createDefault(account.botToken())
                .enableIntents(GatewayIntent.MESSAGE_CONTENT)
                .build();

        // Wait for JDA to be ready
        jda.awaitReady();

        logger.info("Discord bot logged in as: {}", jda.getSelfUser().getName());
    }

    @Override
    public CompletableFuture<SendResult> send(ChannelMessage message) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String channelId = message.chatId();
                if (channelId == null || channelId.isEmpty()) {
                    return SendResult.failure("Channel ID is required");
                }

                TextChannel channel = jda.getTextChannelById(channelId);
                if (channel == null) {
                    return SendResult.failure("Channel not found: " + channelId);
                }

                // Build message
                String content = message.text();
                if (content == null || content.isEmpty()) {
                    return SendResult.failure("Message content is required");
                }

                // Send message
                Message sentMessage = channel.sendMessage(content).complete();

                logger.debug("Sent Discord message: {} to channel: {}",
                        sentMessage.getId(), channelId);

                return SendResult.success(sentMessage.getId());

            } catch (Exception e) {
                logger.error("Failed to send Discord message: {}", e.getMessage());
                return SendResult.failure(e.getMessage());
            }
        });
    }

    @Override
    public CompletableFuture<SendResult> sendText(DiscordChannelPlugin.DiscordAccount account,
                                                  String target,
                                                  String text,
                                                  Optional<String> replyTo) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                TextChannel channel = jda.getTextChannelById(target);
                if (channel == null) {
                    return SendResult.failure("Channel not found: " + target);
                }

                Message.MessageBuilder messageBuilder = new Message.MessageBuilder();
                messageBuilder.setContent(text);

                if (replyTo.isPresent()) {
                    Message referencedMessage = channel.retrieveMessageById(replyTo.get()).complete();
                    if (referencedMessage != null) {
                        messageBuilder.reply(referencedMessage);
                    }
                }

                Message sentMessage = channel.sendMessage(messageBuilder.build()).complete();
                return SendResult.success(sentMessage.getId());

            } catch (Exception e) {
                logger.error("Failed to send text: {}", e.getMessage());
                return SendResult.failure(e.getMessage());
            }
        });
    }

    @Override
    public CompletableFuture<SendResult> sendMarkdown(DiscordChannelPlugin.DiscordAccount account,
                                                      String target,
                                                      String markdown,
                                                      Optional<String> replyTo) {
        // Discord supports markdown natively
        return sendText(account, target, markdown, replyTo);
    }

    @Override
    public CompletableFuture<SendResult> sendMention(DiscordChannelPlugin.DiscordAccount account,
                                                     String target,
                                                     String userId,
                                                     String text) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                TextChannel channel = jda.getTextChannelById(target);
                if (channel == null) {
                    return SendResult.failure("Channel not found: " + target);
                }

                User user = jda.getUserById(userId);
                String mention = user != null ? user.getAsMention() : "<@" + userId + ">";
                String fullMessage = mention + " " + text;

                Message sentMessage = channel.sendMessage(fullMessage).complete();
                return SendResult.success(sentMessage.getId());

            } catch (Exception e) {
                logger.error("Failed to send mention: {}", e.getMessage());
                return SendResult.failure(e.getMessage());
            }
        });
    }

    /**
     * Send embed message
     */
    public CompletableFuture<SendResult> sendEmbed(String channelId, MessageEmbed embed) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                TextChannel channel = jda.getTextChannelById(channelId);
                if (channel == null) {
                    return SendResult.failure("Channel not found: " + channelId);
                }

                Message sentMessage = channel.sendMessageEmbeds(embed).complete();
                return SendResult.success(sentMessage.getId());

            } catch (Exception e) {
                logger.error("Failed to send embed: {}", e.getMessage());
                return SendResult.failure(e.getMessage());
            }
        });
    }

    /**
     * Send direct message
     */
    public CompletableFuture<SendResult> sendDM(String userId, String text) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                User user = jda.getUserById(userId);
                if (user == null) {
                    return SendResult.failure("User not found: " + userId);
                }

                user.openPrivateChannel()
                        .flatMap(channel -> channel.sendMessage(text))
                        .queue(
                                message -> logger.debug("Sent DM to {}: {}", userId, message.getId()),
                                error -> logger.error("Failed to send DM: {}", error.getMessage())
                        );

                return SendResult.success("dm-" + userId);

            } catch (Exception e) {
                logger.error("Failed to send DM: {}", e.getMessage());
                return SendResult.failure(e.getMessage());
            }
        });
    }

    public void shutdown() {
        if (jda != null) {
            jda.shutdown();
            logger.info("Discord JDA shutdown complete");
        }
    }

    public JDA getJda() {
        return jda;
    }
}
