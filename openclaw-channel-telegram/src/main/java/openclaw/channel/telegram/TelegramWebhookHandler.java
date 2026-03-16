package openclaw.channel.telegram;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import openclaw.sdk.channel.ChannelMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Telegram Webhook Handler - 处理 Telegram Bot Webhook
 * 
 * 功能:
 * - 接收 webhook 请求
 * - 验证请求签名
 * - 解析消息
 * - 传递给 InboundAdapter
 * 
 * 对应 Node.js: src/channels/telegram/webhook.ts
 */
@RestController
@RequestMapping("/webhook/telegram")
public class TelegramWebhookHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(TelegramWebhookHandler.class);
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private TelegramInboundAdapter inboundAdapter;
    
    @Autowired
    private TelegramConfig config;
    
    // 用于幂等性检查
    private final Map<Long, Long> processedUpdates = new ConcurrentHashMap<>();
    private static final long DEDUPE_WINDOW_MS = 60000; // 1分钟
    
    /**
     * 处理 Telegram Webhook 请求
     */
    @PostMapping
    public Mono<ResponseEntity<String>> handleWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "X-Telegram-Bot-Api-Secret-Token", required = false) String secretToken) {
        
        return Mono.fromCallable(() -> {
            // 1. 验证 secret token (如果配置了)
            if (config.getWebhookSecret() != null && 
                !config.getWebhookSecret().equals(secretToken)) {
                logger.warn("Invalid webhook secret token");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Unauthorized");
            }
            
            // 2. 解析 JSON
            JsonNode update = objectMapper.readTree(payload);
            long updateId = update.path("update_id").asLong();
            
            // 3. 幂等性检查
            if (isDuplicate(updateId)) {
                logger.debug("Duplicate update ignored: {}", updateId);
                return ResponseEntity.ok("OK");
            }
            
            // 4. 记录已处理
            processedUpdates.put(updateId, System.currentTimeMillis());
            
            // 5. 解析消息
            ChannelMessage message = parseUpdate(update);
            if (message == null) {
                return ResponseEntity.ok("OK");
            }
            
            // 6. 传递给 InboundAdapter
            inboundAdapter.onMessage(message).thenAccept(
                result -> logger.debug("Message processed: {}", updateId)
            ).exceptionally(error -> {
                logger.error("Error processing message: {}", updateId, error);
                return null;
            });
            
            return ResponseEntity.ok("OK");
            
        }).onErrorResume(e -> {
            logger.error("Error handling webhook", e);
            return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error"));
        });
    }
    
    /**
     * 设置 webhook
     */
    @PostMapping("/setup")
    public Mono<ResponseEntity<String>> setupWebhook(
            @RequestParam String url,
            @RequestParam(required = false) String secretToken) {
        
        return inboundAdapter.setupWebhook(url, secretToken)
            .map(success -> {
                if (success) {
                    return ResponseEntity.ok("Webhook setup successfully");
                } else {
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Failed to setup webhook");
                }
            })
            .onErrorResume(e -> {
                logger.error("Error setting up webhook", e);
                return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error: " + e.getMessage()));
            });
    }
    
    /**
     * 删除 webhook
     */
    @DeleteMapping("/setup")
    public Mono<ResponseEntity<String>> deleteWebhook() {
        
        return inboundAdapter.deleteWebhook()
            .map(success -> {
                if (success) {
                    return ResponseEntity.ok("Webhook deleted successfully");
                } else {
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Failed to delete webhook");
                }
            })
            .onErrorResume(e -> {
                logger.error("Error deleting webhook", e);
                return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error: " + e.getMessage()));
            });
    }
    
    /**
     * 获取 webhook 信息
     */
    @GetMapping("/info")
    public Mono<ResponseEntity<String>> getWebhookInfo() {
        
        return inboundAdapter.getWebhookInfo()
            .map(info -> ResponseEntity.ok(info))
            .onErrorResume(e -> {
                logger.error("Error getting webhook info", e);
                return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error: " + e.getMessage()));
            });
    }
    
    /**
     * 解析 Telegram Update
     */
    private ChannelMessage parseUpdate(JsonNode update) {
        try {
            // 处理消息
            if (update.has("message")) {
                return parseMessage(update.path("message"));
            }
            
            // 处理编辑的消息
            if (update.has("edited_message")) {
                return parseMessage(update.path("edited_message"));
            }
            
            // 处理回调查询
            if (update.has("callback_query")) {
                return parseCallbackQuery(update.path("callback_query"));
            }
            
            // 处理频道消息
            if (update.has("channel_post")) {
                return parseMessage(update.path("channel_post"));
            }
            
            logger.debug("Unhandled update type: {}", update);
            return null;
            
        } catch (Exception e) {
            logger.error("Error parsing update", e);
            return null;
        }
    }
    
    /**
     * 解析消息
     */
    private ChannelMessage parseMessage(JsonNode message) {
        String messageId = message.path("message_id").asText();
        long date = message.path("date").asLong() * 1000; // 转换为毫秒
        
        JsonNode from = message.path("from");
        String userId = from.path("id").asText();
        String username = from.path("username").asText();
        String firstName = from.path("first_name").asText();
        
        JsonNode chat = message.path("chat");
        String chatId = chat.path("id").asText();
        String chatType = chat.path("type").asText();
        String chatTitle = chat.path("title").asText();
        
        // 获取文本内容
        String text = null;
        if (message.has("text")) {
            text = message.path("text").asText();
        } else if (message.has("caption")) {
            text = message.path("caption").asText();
        }
        
        // 获取回复的消息 ID
        String replyToMessageId = null;
        if (message.has("reply_to_message")) {
            replyToMessageId = message.path("reply_to_message").path("message_id").asText();
        }
        
        // 获取话题 ID (线程)
        String threadId = null;
        if (message.has("message_thread_id")) {
            threadId = message.path("message_thread_id").asText();
        }
        
        return ChannelMessage.builder()
            .messageId(messageId)
            .text(text)
            .from(userId)
            .fromName(firstName != null ? firstName : username)
            .chatId(chatId)
            .timestamp(date)
            .metadata(Map.of(
                "username", username != null ? username : "",
                "isBot", from.path("is_bot").asBoolean(false),
                "chatType", chatType != null ? chatType : "",
                "chatTitle", chatTitle != null ? chatTitle : "",
                "replyToMessageId", replyToMessageId != null ? replyToMessageId : "",
                "threadId", threadId != null ? threadId : ""
            ))
            .build();
    }
    
    /**
     * 解析回调查询
     */
    private ChannelMessage parseCallbackQuery(JsonNode callbackQuery) {
        String queryId = callbackQuery.path("id").asText();
        String data = callbackQuery.path("data").asText();
        
        JsonNode from = callbackQuery.path("from");
        String userId = from.path("id").asText();
        String username = from.path("username").asText();
        String firstName = from.path("first_name").asText();
        
        // 获取关联的消息
        JsonNode message = callbackQuery.path("message");
        String chatId = message.path("chat").path("id").asText();
        String messageId = message.path("message_id").asText();
        
        return ChannelMessage.builder()
            .messageId(queryId)
            .text(data)
            .from(userId)
            .fromName(firstName != null ? firstName : username)
            .chatId(chatId)
            .timestamp(System.currentTimeMillis())
            .metadata(Map.of(
                "type", "callback_query",
                "queryId", queryId,
                "originalMessageId", messageId,
                "username", username != null ? username : ""
            ))
            .build();
    }
    
    /**
     * 检查是否重复
     */
    private boolean isDuplicate(long updateId) {
        // 清理旧记录
        long now = System.currentTimeMillis();
        processedUpdates.entrySet().removeIf(
            entry -> now - entry.getValue() > DEDUPE_WINDOW_MS
        );
        
        return processedUpdates.containsKey(updateId);
    }
}
