package openclaw.channel.feishu.webhook;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import openclaw.channel.feishu.FeishuChannelPlugin;
import openclaw.channel.feishu.FeishuInboundAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Feishu Webhook Controller - Phase 2 Enhancement
 *
 * <p>Enhanced webhook handling with signature verification.</p>
 */
public class FeishuWebhookController {

    private static final Logger logger = LoggerFactory.getLogger(FeishuWebhookController.class);

    private final ObjectMapper objectMapper;
    private final FeishuChannelPlugin.FeishuAccount account;
    private final FeishuInboundAdapter inboundAdapter;

    public FeishuWebhookController(FeishuChannelPlugin.FeishuAccount account,
                                   FeishuInboundAdapter inboundAdapter) {
        this.objectMapper = new ObjectMapper();
        this.account = account;
        this.inboundAdapter = inboundAdapter;
    }

    /**
     * Process webhook payload with signature verification
     */
    public CompletableFuture<WebhookResponse> processWebhook(String payload, String signature, String timestamp) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Verify signature
                if (!verifySignature(payload, signature, timestamp)) {
                    logger.warn("Invalid webhook signature");
                    return WebhookResponse.error("Invalid signature");
                }

                JsonNode root = objectMapper.readTree(payload);

                // Handle URL verification
                if (root.has("challenge")) {
                    String challenge = root.get("challenge").asText();
                    return handleUrlVerification(challenge);
                }

                // Handle event
                if (root.has("event")) {
                    JsonNode event = root.get("event");
                    return handleEvent(event);
                }

                return WebhookResponse.ok();

            } catch (Exception e) {
                logger.error("Failed to process webhook: {}", e.getMessage());
                return WebhookResponse.error("Processing failed: " + e.getMessage());
            }
        });
    }

    /**
     * Handle URL verification (required for Feishu webhook setup)
     */
    private WebhookResponse handleUrlVerification(String challenge) {
        logger.info("Handling Feishu URL verification");
        return new WebhookResponse(true, Optional.empty(), Optional.of(challenge));
    }

    /**
     * Handle event
     */
    private WebhookResponse handleEvent(JsonNode event) {
        try {
            String eventType = event.get("type").asText();
            logger.info("Handling Feishu event: {}", eventType);

            // Extract event data
            FeishuInboundAdapter.FeishuEvent feishuEvent = extractEvent(event, eventType);

            // Process via inbound adapter
            inboundAdapter.onEvent(feishuEvent)
                    .thenAccept(result -> {
                        if (!result.success()) {
                            logger.error("Failed to process event: {}", result.error());
                        }
                    });

            return WebhookResponse.ok();

        } catch (Exception e) {
            logger.error("Failed to handle event: {}", e.getMessage());
            return WebhookResponse.error("Event handling failed: " + e.getMessage());
        }
    }

    /**
     * Extract event from JSON
     */
    private FeishuInboundAdapter.FeishuEvent extractEvent(JsonNode event, String eventType) {
        String sender = "";
        String chatId = "";
        String messageId = "";
        String content = "";

        // Extract sender
        if (event.has("sender")) {
            JsonNode senderNode = event.get("sender");
            if (senderNode.has("sender_id")) {
                JsonNode senderId = senderNode.get("sender_id");
                if (senderId.has("open_id")) {
                    sender = senderId.get("open_id").asText();
                }
            }
        }

        // Extract message info
        if (event.has("message")) {
            JsonNode message = event.get("message");
            if (message.has("message_id")) {
                messageId = message.get("message_id").asText();
            }
            if (message.has("chat_id")) {
                chatId = message.get("chat_id").asText();
            }
            if (message.has("content")) {
                content = message.get("content").asText();
            }
        }

        return new FeishuInboundAdapter.FeishuEvent(
                eventType,
                sender,
                chatId,
                messageId,
                content,
                Map.of()
        );
    }

    /**
     * Verify webhook signature
     */
    public boolean verifySignature(String payload, String signature, String timestamp) {
        try {
            if (account.encryptKey().isEmpty()) {
                // No encryption key configured, skip verification
                return true;
            }

            String encryptKey = account.encryptKey().get();
            String signString = timestamp + "\n" + encryptKey + "\n" + payload;

            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(encryptKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);

            byte[] hash = mac.doFinal(signString.getBytes(StandardCharsets.UTF_8));
            String expectedSignature = Base64.getEncoder().encodeToString(hash);

            return expectedSignature.equals(signature);

        } catch (Exception e) {
            logger.error("Signature verification failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Webhook response
     */
    public record WebhookResponse(
            boolean success,
            Optional<String> error,
            Optional<String> challenge
    ) {
        public static WebhookResponse ok() {
            return new WebhookResponse(true, Optional.empty(), Optional.empty());
        }
        public static WebhookResponse error(String message) {
            return new WebhookResponse(false, Optional.of(message), Optional.empty());
        }
    }
}
