package openclaw.channel.telegram.webhook;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Telegram Webhook Types - 定义 webhook 相关的数据类型
 */
public class TelegramWebhookTypes {

    /**
     * Webhook 响应
     */
    public record WebhookResponse(
            boolean success,
            String message,
            String error
    ) {
        public static WebhookResponse success(String message) {
            return new WebhookResponse(true, message, null);
        }

        public static WebhookResponse failure(String error) {
            return new WebhookResponse(false, null, error);
        }
    }

    /**
     * Telegram 消息信息
     */
    public record TelegramMessageInfo(
            long messageId,
            String text,
            String userId,
            String username,
            String firstName,
            String chatId,
            String chatType,
            long timestamp
    ) {
    }

    /**
     * Telegram 回调查询信息
     */
    public record TelegramCallbackInfo(
            String callbackId,
            String data,
            String userId,
            String chatId,
            long timestamp
    ) {
    }

    /**
     * Telegram Update 处理器接口
     */
    public interface TelegramUpdateHandler {
        void onMessage(TelegramMessageInfo message);
        void onCallbackQuery(TelegramCallbackInfo callback);
    }

    /**
     * 从 JSON 解析消息信息
     */
    public static TelegramMessageInfo parseMessageInfo(JsonNode message) {
        long messageId = message.path("message_id").asLong();
        long date = message.path("date").asLong() * 1000;

        JsonNode from = message.path("from");
        String userId = from.path("id").asText();
        String username = from.path("username").asText();
        String firstName = from.path("first_name").asText();

        JsonNode chat = message.path("chat");
        String chatId = chat.path("id").asText();
        String chatType = chat.path("type").asText();

        String text = null;
        if (message.has("text")) {
            text = message.path("text").asText();
        } else if (message.has("caption")) {
            text = message.path("caption").asText();
        }

        return new TelegramMessageInfo(
                messageId,
                text,
                userId,
                username,
                firstName,
                chatId,
                chatType,
                date
        );
    }

    /**
     * 从 JSON 解析回调查询信息
     */
    public static TelegramCallbackInfo parseCallbackInfo(JsonNode callbackQuery) {
        String queryId = callbackQuery.path("id").asText();
        String data = callbackQuery.path("data").asText();

        JsonNode from = callbackQuery.path("from");
        String userId = from.path("id").asText();

        JsonNode message = callbackQuery.path("message");
        String chatId = message.path("chat").path("id").asText();

        return new TelegramCallbackInfo(
                queryId,
                data,
                userId,
                chatId,
                System.currentTimeMillis()
        );
    }
}
