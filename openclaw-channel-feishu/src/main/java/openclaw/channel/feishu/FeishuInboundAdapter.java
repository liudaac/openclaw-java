package openclaw.channel.feishu;

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
 * Feishu Inbound Adapter - Phase 2 Enhancement
 *
 * <p>Handles incoming messages from Feishu webhook.</p>
 * <p>Updated with policy-based mention handling based on groupPolicy.</p>
 */
public class FeishuInboundAdapter implements ChannelInboundAdapter {

    private static final Logger logger = LoggerFactory.getLogger(FeishuInboundAdapter.class);

    private final FeishuChannelPlugin.FeishuAccount account;
    private final FeishuOutboundAdapter outboundAdapter;
    private final FeishuPolicyResolver policyResolver;
    private final CopyOnWriteArrayList<Consumer<ChannelMessage>> messageHandlers;
    private FeishuGroupPolicy groupPolicy = FeishuGroupPolicy.ALLOWLIST;
    private Map<String, FeishuGroupConfig> groups = Map.of();
    private Optional<Boolean> globalRequireMention = Optional.empty();

    public FeishuInboundAdapter(FeishuChannelPlugin.FeishuAccount account,
                                FeishuOutboundAdapter outboundAdapter) {
        this.account = account;
        this.outboundAdapter = outboundAdapter;
        this.policyResolver = new FeishuPolicyResolver();
        this.messageHandlers = new CopyOnWriteArrayList<>();
    }

    /**
     * Set group policy configuration.
     *
     * @param policy the group policy
     */
    public void setGroupPolicy(FeishuGroupPolicy policy) {
        this.groupPolicy = policy != null ? policy : FeishuGroupPolicy.ALLOWLIST;
        logger.info("Set Feishu group policy to: {}", this.groupPolicy);
    }

    /**
     * Set groups configuration.
     *
     * @param groups map of group configs
     */
    public void setGroups(Map<String, FeishuGroupConfig> groups) {
        this.groups = groups != null ? groups : Map.of();
        logger.info("Set Feishu groups configuration, count: {}", this.groups.size());
    }

    /**
     * Set global require mention setting.
     *
     * @param requireMention optional boolean
     */
    public void setGlobalRequireMention(Optional<Boolean> requireMention) {
        this.globalRequireMention = requireMention;
    }

    @Override
    public CompletableFuture<ChannelInboundAdapter.ProcessResult> onMessage(ChannelMessage message) {
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
    public CompletableFuture<ChannelInboundAdapter.ProcessResult> onEvent(FeishuEvent event) {
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
                        return ChannelInboundAdapter.ProcessResult.success();
                }

            } catch (Exception e) {
                logger.error("Failed to process event: {}", e.getMessage());
                return ChannelInboundAdapter.ProcessResult.failure(e.getMessage());
            }
        });
    }

    private ChannelInboundAdapter.ProcessResult handleMessageEvent(FeishuEvent event) {
        // Extract message info from event
        String chatId = event.chatId();
        String senderId = event.sender();
        String content = event.content();
        String contentType = event.extra() != null ?
                event.extra().getOrDefault("contentType", "text").toString() : "text";

        // Determine if this is a direct message
        boolean isDirectMessage = chatId == null || chatId.isEmpty() ||
                (event.extra() != null && "p2p".equals(event.extra().get("chatType")));

        // Check if bot was mentioned
        boolean mentionedBot = event.extra() != null &&
                Boolean.TRUE.equals(event.extra().get("mentionedBot"));

        // Resolve group configuration
        Optional<FeishuGroupConfig> groupConfig = policyResolver.resolveGroupConfig(groups, chatId);

        // Resolve reply policy
        FeishuPolicyResolver.ReplyPolicy replyPolicy = policyResolver.resolveReplyPolicy(
                isDirectMessage,
                groupPolicy,
                groupConfig,
                globalRequireMention
        );

        // Check if message should be processed
        boolean shouldProcess = policyResolver.shouldProcessMessage(
                isDirectMessage,
                mentionedBot,
                contentType,
                replyPolicy
        );

        if (!shouldProcess) {
            logger.debug("Skipping message {} - not mentioned and requireMention is true", event.messageId());
            return ChannelInboundAdapter.ProcessResult.success();
        }

        // Convert to ChannelMessage
        ChannelMessage message = ChannelMessage.builder()
                .text(content)
                .from(senderId)
                .chatId(chatId)
                .messageId(event.messageId())
                .timestamp(System.currentTimeMillis())
                .metadata(Map.of(
                        "eventType", event.type(),
                        "account", account,
                        "isDirectMessage", isDirectMessage,
                        "mentionedBot", mentionedBot,
                        "contentType", contentType,
                        "requireMention", replyPolicy.requireMention()
                ))
                .build();

        return onMessage(message).join();
    }

    private ChannelInboundAdapter.ProcessResult handleMenuEvent(FeishuEvent event) {
        logger.info("Menu event from user: {}", event.sender());
        return ChannelInboundAdapter.ProcessResult.success();
    }

    private ChannelInboundAdapter.ProcessResult handleCardAction(FeishuEvent event) {
        logger.info("Card action from user: {}", event.sender());
        return ChannelInboundAdapter.ProcessResult.success();
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
