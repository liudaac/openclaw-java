package openclaw.channel.slack;

import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.request.chat.ChatPostMessageRequest;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import openclaw.sdk.channel.ChannelMessage;
import openclaw.sdk.channel.ChannelOutboundAdapter;
import openclaw.sdk.channel.SendResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Slack Outbound Adapter
 */
public class SlackOutboundAdapter implements ChannelOutboundAdapter {

    private static final Logger logger = LoggerFactory.getLogger(SlackOutboundAdapter.class);

    private SlackChannelPlugin.SlackAccount account;
    private MethodsClient methods;

    public void initialize(SlackChannelPlugin.SlackAccount account, MethodsClient methods) {
        this.account = account;
        this.methods = methods;
    }

    @Override
    public CompletableFuture<SendResult> send(ChannelMessage message) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String channel = message.chatId();
                if (channel == null || channel.isEmpty()) {
                    return SendResult.failure("Channel ID is required");
                }

                String text = message.text();
                if (text == null || text.isEmpty()) {
                    return SendResult.failure("Message text is required");
                }

                // Build request
                ChatPostMessageRequest.ChatPostMessageRequestBuilder requestBuilder =
                        ChatPostMessageRequest.builder()
                                .channel(channel)
                                .text(text);

                // Add thread support
                if (message.metadata().containsKey("thread_ts")) {
                    requestBuilder.threadTs((String) message.metadata().get("thread_ts"));
                }

                // Send message
                ChatPostMessageResponse response = methods.chatPostMessage(requestBuilder.build());

                if (!response.isOk()) {
                    logger.error("Failed to send Slack message: {}", response.getError());
                    return SendResult.failure("Slack API error: " + response.getError());
                }

                logger.debug("Sent Slack message: {} to channel: {}",
                        response.getTs(), channel);

                return SendResult.success(response.getTs());

            } catch (Exception e) {
                logger.error("Failed to send Slack message: {}", e.getMessage());
                return SendResult.failure(e.getMessage());
            }
        });
    }

    @Override
    public CompletableFuture<SendResult> sendText(SlackChannelPlugin.SlackAccount account,
                                                  String target,
                                                  String text,
                                                  Optional<String> replyTo) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                ChatPostMessageRequest.ChatPostMessageRequestBuilder requestBuilder =
                        ChatPostMessageRequest.builder()
                                .channel(target)
                                .text(text);

                if (replyTo.isPresent()) {
                    requestBuilder.threadTs(replyTo.get());
                }

                ChatPostMessageResponse response = methods.chatPostMessage(requestBuilder.build());

                if (!response.isOk()) {
                    return SendResult.failure("Slack API error: " + response.getError());
                }

                return SendResult.success(response.getTs());

            } catch (Exception e) {
                logger.error("Failed to send text: {}", e.getMessage());
                return SendResult.failure(e.getMessage());
            }
        });
    }

    @Override
    public CompletableFuture<SendResult> sendMarkdown(SlackChannelPlugin.SlackAccount account,
                                                      String target,
                                                      String markdown,
                                                      Optional<String> replyTo) {
        // Slack uses mrkdwn format
        return sendText(account, target, markdown, replyTo);
    }

    @Override
    public CompletableFuture<SendResult> sendMention(SlackChannelPlugin.SlackAccount account,
                                                     String target,
                                                     String userId,
                                                     String text) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String mention = "<@" + userId + ">";
                String fullMessage = mention + " " + text;

                ChatPostMessageRequest request = ChatPostMessageRequest.builder()
                        .channel(target)
                        .text(fullMessage)
                        .build();

                ChatPostMessageResponse response = methods.chatPostMessage(request);

                if (!response.isOk()) {
                    return SendResult.failure("Slack API error: " + response.getError());
                }

                return SendResult.success(response.getTs());

            } catch (Exception e) {
                logger.error("Failed to send mention: {}", e.getMessage());
                return SendResult.failure(e.getMessage());
            }
        });
    }

    /**
     * Send block kit message
     */
    public CompletableFuture<SendResult> sendBlocks(String channel, String blocks) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                ChatPostMessageRequest request = ChatPostMessageRequest.builder()
                        .channel(channel)
                        .blocksAsString(blocks)
                        .build();

                ChatPostMessageResponse response = methods.chatPostMessage(request);

                if (!response.isOk()) {
                    return SendResult.failure("Slack API error: " + response.getError());
                }

                return SendResult.success(response.getTs());

            } catch (Exception e) {
                logger.error("Failed to send blocks: {}", e.getMessage());
                return SendResult.failure(e.getMessage());
            }
        });
    }

    /**
     * Send ephemeral message
     */
    public CompletableFuture<SendResult> sendEphemeral(String channel, String user, String text) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                var response = methods.chatPostEphemeral(r -> r
                        .channel(channel)
                        .user(user)
                        .text(text)
                );

                if (!response.isOk()) {
                    return SendResult.failure("Slack API error: " + response.getError());
                }

                return SendResult.success(response.getMessageTs());

            } catch (Exception e) {
                logger.error("Failed to send ephemeral: {}", e.getMessage());
                return SendResult.failure(e.getMessage());
            }
        });
    }
}
