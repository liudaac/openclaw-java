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

    /**
     * Telegram message text limit (4096 characters).
     * Messages longer than this need to be split.
     */
    public static final int TELEGRAM_MESSAGE_LIMIT = 4096;

    /**
     * Safety margin for message splitting (leave room for formatting).
     */
    public static final int MESSAGE_SPLIT_MARGIN = 100;

    /**
     * Effective maximum length for a single message chunk.
     */
    public static final int MAX_MESSAGE_LENGTH = TELEGRAM_MESSAGE_LIMIT - MESSAGE_SPLIT_MARGIN;

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

                // Split long messages into chunks
                java.util.List<String> chunks = splitMessage(message);

                if (chunks.size() == 1) {
                    // Single message - send normally
                    return sendSingleMessage(telegramAccount, to, chunks.get(0), options);
                } else {
                    // Multiple chunks - send sequentially
                    return sendMessageChunks(telegramAccount, to, chunks, options);
                }
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

    /**
     * Split a long message into chunks that fit within Telegram's message limit.
     *
     * @param message the message to split
     * @return list of message chunks
     */
    private java.util.List<String> splitMessage(String message) {
        if (message == null || message.length() <= MAX_MESSAGE_LENGTH) {
            return java.util.List.of(message);
        }

        java.util.List<String> chunks = new java.util.ArrayList<>();
        int start = 0;

        while (start < message.length()) {
            int end = Math.min(start + MAX_MESSAGE_LENGTH, message.length());

            // Try to split at a newline if possible (prefer splitting at paragraph boundaries)
            if (end < message.length()) {
                int lastNewline = message.lastIndexOf('\n', end);
                if (lastNewline > start && lastNewline > end - 200) {
                    // Found a newline within reasonable distance, split there
                    end = lastNewline + 1; // Include the newline
                }
            }

            chunks.add(message.substring(start, end));
            start = end;
        }

        return chunks;
    }

    /**
     * Send a single message.
     */
    private SendResult sendSingleMessage(
            TelegramChannelPlugin.TelegramAccount account,
            String to,
            String message,
            Optional<SendOptions> options) throws IOException {

        String url = account.apiUrl() + "/bot" + account.botToken() + "/sendMessage";

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
    }

    /**
     * Send multiple message chunks sequentially.
     * Only the first message preserves reply options.
     */
    private SendResult sendMessageChunks(
            TelegramChannelPlugin.TelegramAccount account,
            String to,
            java.util.List<String> chunks,
            Optional<SendOptions> options) throws IOException {

        String lastMessageId = null;
        StringBuilder errors = new StringBuilder();

        for (int i = 0; i < chunks.size(); i++) {
            String chunk = chunks.get(i);

            // Only first chunk gets reply/thread options
            Optional<SendOptions> chunkOptions = (i == 0) ? options : Optional.empty();

            SendResult result = sendSingleMessage(account, to, chunk, chunkOptions);

            if (result.success()) {
                lastMessageId = result.messageId().orElse(null);
            } else {
                errors.append("Chunk ").append(i + 1).append("/").append(chunks.size())
                      .append(" failed: ").append(result.error().orElse("Unknown error"))
                      .append("\n");
            }
        }

        if (errors.length() > 0) {
            return SendResult.failure(errors.toString().trim());
        }

        return SendResult.success(lastMessageId);
    }
}
