package openclaw.channel.feishu;

import openclaw.plugin.sdk.channel.ChannelStreamingAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Feishu channel streaming adapter implementation.
 *
 * @author OpenClaw Team
 * @version 2026.3.13
 */
@Component
public class FeishuStreamingAdapter implements ChannelStreamingAdapter {
    
    private static final Logger logger = LoggerFactory.getLogger(FeishuStreamingAdapter.class);
    
    private final FeishuApiClient apiClient;
    private final Map<String, String> messageIdMap; // internal -> feishu message id
    private final StreamingConfig config;
    
    public FeishuStreamingAdapter(FeishuApiClient apiClient) {
        this.apiClient = apiClient;
        this.messageIdMap = new ConcurrentHashMap<>();
        this.config = new StreamingConfig(
            500,           // chunk size
            Duration.ofMillis(200), // chunk interval
            true,          // enable typing
            Duration.ofSeconds(3),  // typing interval
            10,            // max concurrent
            Duration.ofMinutes(5)   // timeout
        );
    }
    
    @Override
    public boolean supportsStreaming() {
        return true;
    }
    
    @Override
    public Flux<StreamChunk> sendStreamingMessage(StreamingMessageRequest request) {
        return Flux.defer(() -> {
            String internalId = UUID.randomUUID().toString();
            
            // Send initial message to Feishu
            return apiClient.sendMessage(request.getChatId(), "...")
                .flatMapMany(feishuMessageId -> {
                    messageIdMap.put(internalId, feishuMessageId);
                    
                    return request.getContentStream()
                        .map(content -> {
                            long seq = System.currentTimeMillis();
                            return StreamChunk.intermediate(internalId, content, seq);
                        });
                });
        });
    }
    
    @Override
    public Mono<Void> sendTypingIndicator(String chatId, Duration duration) {
        return Mono.defer(() -> {
            logger.debug("Sending typing indicator to Feishu chat: {} for {}", chatId, duration);
            // Feishu doesn't have native typing indicator, but we can simulate
            // by sending a temporary message or using card update
            return Mono.empty();
        });
    }
    
    @Override
    public Mono<Void> updateStreamingMessage(String messageId, String content) {
        return Mono.defer(() -> {
            String feishuMessageId = messageIdMap.get(messageId);
            if (feishuMessageId == null) {
                logger.warn("Message not found for update: {}", messageId);
                return Mono.empty();
            }
            
            logger.debug("Updating Feishu message: {} with content length: {}", 
                feishuMessageId, content.length());
            
            // Update message via Feishu API
            return apiClient.updateMessage(feishuMessageId, content);
        });
    }
    
    @Override
    public Mono<Void> finalizeStreamingMessage(String messageId, String finalContent) {
        return Mono.defer(() -> {
            String feishuMessageId = messageIdMap.get(messageId);
            if (feishuMessageId == null) {
                logger.warn("Message not found for finalize: {}", messageId);
                return Mono.empty();
            }
            
            logger.debug("Finalizing Feishu message: {}", feishuMessageId);
            
            // Final update
            return apiClient.updateMessage(feishuMessageId, finalContent)
                .doOnSuccess(v -> messageIdMap.remove(messageId));
        });
    }
    
    @Override
    public Mono<Void> cancelStreamingMessage(String messageId, String reason) {
        return Mono.defer(() -> {
            String feishuMessageId = messageIdMap.get(messageId);
            if (feishuMessageId == null) {
                return Mono.empty();
            }
            
            logger.info("Cancelling Feishu message: {} - {}", feishuMessageId, reason);
            
            // Update with cancellation notice
            return apiClient.updateMessage(feishuMessageId, "[Cancelled: " + reason + "]")
                .doOnSuccess(v -> messageIdMap.remove(messageId));
        });
    }
    
    @Override
    public StreamingConfig getStreamingConfig() {
        return config;
    }
    
    /**
     * Feishu API client interface.
     */
    public interface FeishuApiClient {
        Mono<String> sendMessage(String chatId, String content);
        Mono<Void> updateMessage(String messageId, String content);
    }
}
