package openclaw.channel.feishu;

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
 * Feishu outbound adapter for sending messages.
 *
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public class FeishuOutboundAdapter implements ChannelOutboundAdapter {

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public FeishuOutboundAdapter() {
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
                FeishuChannelPlugin.FeishuAccount feishuAccount = 
                        (FeishuChannelPlugin.FeishuAccount) account;
                
                String accessToken = getAccessToken(feishuAccount);
                String url = feishuAccount.apiUrl() + "/open-apis/im/v1/messages";
                
                Map<String, Object> body = Map.of(
                        "receive_id", to,
                        "msg_type", "text",
                        "content", Map.of("text", message)
                );
                
                return sendRequest(url, accessToken, body);
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
                FeishuChannelPlugin.FeishuAccount feishuAccount = 
                        (FeishuChannelPlugin.FeishuAccount) account;
                
                String accessToken = getAccessToken(feishuAccount);
                String url = feishuAccount.apiUrl() + "/open-apis/im/v1/messages";
                
                Map<String, Object> content = new java.util.HashMap<>();
                content.put("file_key", mediaUrl);
                message.ifPresent(m -> content.put("caption", m));
                
                Map<String, Object> body = Map.of(
                        "receive_id", to,
                        "msg_type", "image",
                        "content", content
                );
                
                return sendRequest(url, accessToken, body);
            } catch (Exception e) {
                return SendResult.failure(e.getMessage());
            }
        });
    }

    @Override
    public CompletableFuture<Void> sendTyping(Object account, String to) {
        // Feishu doesn't have a typing indicator API
        return CompletableFuture.completedFuture(null);
    }

    private String getAccessToken(FeishuChannelPlugin.FeishuAccount account) throws IOException {
        String url = account.apiUrl() + "/open-apis/auth/v3/app_access_token/internal";
        
        Map<String, Object> body = Map.of(
                "app_id", account.appId(),
                "app_secret", account.appSecret()
        );
        
        String jsonBody = objectMapper.writeValueAsString(body);
        
        Request request = new Request.Builder()
                .url(url)
                .header("Content-Type", "application/json")
                .post(RequestBody.create(jsonBody, MediaType.parse("application/json")))
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to get access token: " + response.code());
            }
            
            JsonNode root = objectMapper.readTree(response.body().string());
            return root.get("app_access_token").asText();
        }
    }

    private SendResult sendRequest(String url, String accessToken, Map<String, Object> body) throws IOException {
        String jsonBody = objectMapper.writeValueAsString(body);
        
        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .post(RequestBody.create(jsonBody, MediaType.parse("application/json")))
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                return SendResult.failure("HTTP " + response.code() + ": " + response.message());
            }
            
            JsonNode root = objectMapper.readTree(response.body().string());
            
            if (root.has("code") && root.get("code").asInt() != 0) {
                String error = root.get("msg").asText();
                return SendResult.failure(error);
            }
            
            String messageId = root.get("data").get("message_id").asText();
            return SendResult.success(messageId);
        }
    }
}
