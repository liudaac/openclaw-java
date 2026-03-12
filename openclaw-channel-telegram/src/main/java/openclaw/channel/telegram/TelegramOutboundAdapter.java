package openclaw.channel.telegram;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import openclaw.sdk.channel.ChannelOutboundAdapter;
import openclaw.sdk.channel.SendResult;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Telegram outbound adapter for sending messages.
 *
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public class TelegramOutboundAdapter implements ChannelOutboundAdapter {

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public TelegramOutboundAdapter() {
        this.httpClient = new OkHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public CompletableFuture<SendResult> sendText(
            Object account,
            String to,
            String message,
            Optional<SendOptions> options) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                TelegramChannelPlugin.TelegramAccount telegramAccount = 
                        (TelegramChannelPlugin.TelegramAccount) account;
                
                String url = telegramAccount.apiUrl() + "/bot" + telegramAccount.botToken() + "/sendMessage";
                
                Map<String, Object> body = new java.util.HashMap<>();
                body.put("chat_id", to);
                body.put("text", message);
                body.put("parse_mode", "Markdown");
                
                options.ifPresent(opt -> {
                    opt.replyTo().ifPresent(replyTo -> body.put("reply_to_message_id", replyTo));
                    opt.threadId().ifPresent(threadId -> body.put("message_thread_id", threadId));
                    if (opt.silent()) {
                        body.put("disable_notification", true);
                    }
                });
                
                return sendRequest(url, body);
            } catch (Exception e) {
                return SendResult.failure(e.getMessage());
            }
        });
    }

    @Override
    public CompletableFuture<SendResult> sendMedia(
            Object account,
            String to,
            Optional<String> message,
            String mediaUrl,
            Optional<SendOptions> options) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                TelegramChannelPlugin.TelegramAccount telegramAccount = 
                        (TelegramChannelPlugin.TelegramAccount) account;
                
                String url = telegramAccount.apiUrl() + "/bot" + telegramAccount.botToken() + "/sendPhoto";
                
                Map<String, Object> body = new java.util.HashMap<>();
                body.put("chat_id", to);
                body.put("photo", mediaUrl);
                message.ifPresent(m -> body.put("caption", m));
                
                return sendRequest(url, body);
            } catch (Exception e) {
                return SendResult.failure(e.getMessage());
            }
        });
    }

    @Override
    public CompletableFuture<Void> sendTyping(Object account, String to) {
        return CompletableFuture.runAsync(() -> {
            try {
                TelegramChannelPlugin.TelegramAccount telegramAccount = 
                        (TelegramChannelPlugin.TelegramAccount) account;
                
                String url = telegramAccount.apiUrl() + "/bot" + telegramAccount.botToken() + "/sendChatAction";
                
                Map<String, Object> body = Map.of(
                        "chat_id", to,
                        "action", "typing"
                );
                
                sendRequest(url, body);
            } catch (Exception e) {
                // Ignore typing errors
            }
        });
    }

    private SendResult sendRequest(String url, Map<String, Object> body) throws IOException {
        String jsonBody = objectMapper.writeValueAsString(body);
        
        Request request = new Request.Builder()
                .url(url)
                .header("Content-Type", "application/json")
                .post(RequestBody.create(jsonBody, MediaType.parse("application/json")))
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                return SendResult.failure("HTTP " + response.code() + ": " + response.message());
            }
            
            JsonNode root = objectMapper.readTree(response.body().string());
            
            if (!root.get("ok").asBoolean()) {
                String error = root.get("description").asText();
                return SendResult.failure(error);
            }
            
            String messageId = root.get("result").get("message_id").asText();
            return SendResult.success(messageId);
        }
    }
}
