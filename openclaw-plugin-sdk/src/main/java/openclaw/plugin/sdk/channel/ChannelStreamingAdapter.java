package openclaw.plugin.sdk.channel;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

/**
 * Channel Streaming Adapter - 支持实时流式消息
 * 
 * 功能:
 * - 流式消息发送 (SSE/WebSocket)
 * - 背压控制
 * - 流取消机制
 * - 实时打字指示器
 * 
 * 对应 Node.js: src/channels/streaming.ts
 */
public interface ChannelStreamingAdapter extends ChannelAdapter {
    
    /**
     * 检查通道是否支持流式消息
     */
    boolean supportsStreaming();
    
    /**
     * 发送流式消息
     * 
     * @param request 流式请求
     * @return 流式响应 Flux
     */
    Flux<StreamChunk> sendStreamingMessage(StreamingMessageRequest request);
    
    /**
     * 发送打字指示器
     * 
     * @param chatId 聊天 ID
     * @param duration 显示时长
     * @return Mono<Void>
     */
    Mono<Void> sendTypingIndicator(String chatId, Duration duration);
    
    /**
     * 更新流式消息
     * 
     * @param messageId 消息 ID
     * @param content 新内容
     * @return Mono<Void>
     */
    Mono<Void> updateStreamingMessage(String messageId, String content);
    
    /**
     * 完成流式消息
     * 
     * @param messageId 消息 ID
     * @param finalContent 最终内容
     * @return Mono<Void>
     */
    Mono<Void> finalizeStreamingMessage(String messageId, String finalContent);
    
    /**
     * 取消流式消息
     * 
     * @param messageId 消息 ID
     * @param reason 取消原因
     * @return Mono<Void>
     */
    Mono<Void> cancelStreamingMessage(String messageId, String reason);
    
    /**
     * 获取流式配置
     */
    StreamingConfig getStreamingConfig();
    
    /**
     * 流式消息请求
     */
    class StreamingMessageRequest {
        private final String chatId;
        private final String replyToMessageId;
        private final Flux<String> contentStream;
        private final Map<String, Object> metadata;
        private final Duration timeout;
        
        public StreamingMessageRequest(String chatId, Flux<String> contentStream) {
            this(chatId, null, contentStream, Map.of(), Duration.ofMinutes(5));
        }
        
        public StreamingMessageRequest(String chatId, String replyToMessageId,
                                       Flux<String> contentStream,
                                       Map<String, Object> metadata,
                                       Duration timeout) {
            this.chatId = chatId;
            this.replyToMessageId = replyToMessageId;
            this.contentStream = contentStream;
            this.metadata = metadata;
            this.timeout = timeout;
        }
        
        // Getters
        public String getChatId() { return chatId; }
        public String getReplyToMessageId() { return replyToMessageId; }
        public Flux<String> getContentStream() { return contentStream; }
        public Map<String, Object> getMetadata() { return metadata; }
        public Duration getTimeout() { return timeout; }
    }
    
    /**
     * 流式块
     */
    class StreamChunk {
        private final String messageId;
        private final String content;
        private final boolean isFinal;
        private final long sequence;
        private final Map<String, Object> metadata;
        
        public StreamChunk(String messageId, String content, boolean isFinal,
                          long sequence, Map<String, Object> metadata) {
            this.messageId = messageId;
            this.content = content;
            this.isFinal = isFinal;
            this.sequence = sequence;
            this.metadata = metadata;
        }
        
        // Getters
        public String getMessageId() { return messageId; }
        public String getContent() { return content; }
        public boolean isFinal() { return isFinal; }
        public long getSequence() { return sequence; }
        public Map<String, Object> getMetadata() { return metadata; }
        
        /**
         * 创建中间块
         */
        public static StreamChunk intermediate(String messageId, String content,
                                               long sequence) {
            return new StreamChunk(messageId, content, false, sequence, Map.of());
        }
        
        /**
         * 创建最终块
         */
        public static StreamChunk finalChunk(String messageId, String content,
                                             long sequence) {
            return new StreamChunk(messageId, content, true, sequence, Map.of());
        }
    }
    
    /**
     * 流式配置
     */
    class StreamingConfig {
        private final int chunkSize;
        private final Duration chunkInterval;
        private final boolean enableTypingIndicator;
        private final Duration typingIndicatorInterval;
        private final int maxConcurrentStreams;
        private final Duration defaultTimeout;
        
        public StreamingConfig() {
            this(100, Duration.ofMillis(100), true, Duration.ofSeconds(5),
                 10, Duration.ofMinutes(5));
        }
        
        public StreamingConfig(int chunkSize, Duration chunkInterval,
                              boolean enableTypingIndicator,
                              Duration typingIndicatorInterval,
                              int maxConcurrentStreams,
                              Duration defaultTimeout) {
            this.chunkSize = chunkSize;
            this.chunkInterval = chunkInterval;
            this.enableTypingIndicator = enableTypingIndicator;
            this.typingIndicatorInterval = typingIndicatorInterval;
            this.maxConcurrentStreams = maxConcurrentStreams;
            this.defaultTimeout = defaultTimeout;
        }
        
        // Getters
        public int getChunkSize() { return chunkSize; }
        public Duration getChunkInterval() { return chunkInterval; }
        public boolean isEnableTypingIndicator() { return enableTypingIndicator; }
        public Duration getTypingIndicatorInterval() { return typingIndicatorInterval; }
        public int getMaxConcurrentStreams() { return maxConcurrentStreams; }
        public Duration getDefaultTimeout() { return defaultTimeout; }
    }
}
