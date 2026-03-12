package openclaw.plugin.sdk.channel;

import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Channel Threading Adapter - 线程/话题管理
 * 
 * 功能:
 * - 创建线程/话题
 * - 在线程中发送消息
 * - 线程元数据管理
 * - 线程绑定到会话
 * 
 * 对应 Node.js: src/channels/threading.ts
 */
public interface ChannelThreadingAdapter extends ChannelAdapter {
    
    /**
     * 检查通道是否支持线程
     */
    boolean supportsThreading();
    
    /**
     * 创建线程
     * 
     * @param request 创建线程请求
     * @return Mono<ThreadInfo>
     */
    Mono<ThreadInfo> createThread(CreateThreadRequest request);
    
    /**
     * 在线程中发送消息
     * 
     * @param threadId 线程 ID
     * @param content 消息内容
     * @return Mono<String> 消息 ID
     */
    Mono<String> sendThreadMessage(String threadId, String content);
    
    /**
     * 获取线程信息
     * 
     * @param threadId 线程 ID
     * @return Mono<ThreadInfo>
     */
    Mono<ThreadInfo> getThreadInfo(String threadId);
    
    /**
     * 关闭线程
     * 
     * @param threadId 线程 ID
     * @param archive 是否归档
     * @return Mono<Void>
     */
    Mono<Void> closeThread(String threadId, boolean archive);
    
    /**
     * 列出聊天中的线程
     * 
     * @param chatId 聊天 ID
     * @return Mono<List<ThreadInfo>>
     */
    Mono<List<ThreadInfo>> listThreads(String chatId);
    
    /**
     * 绑定线程到会话
     * 
     * @param threadId 线程 ID
     * @param sessionKey 会话 Key
     * @return Mono<Void>
     */
    Mono<Void> bindThreadToSession(String threadId, String sessionKey);
    
    /**
     * 获取线程绑定的会话
     * 
     * @param threadId 线程 ID
     * @return Mono<Optional<String>> 会话 Key
     */
    Mono<Optional<String>> getThreadSessionBinding(String threadId);
    
    /**
     * 解除线程绑定
     * 
     * @param threadId 线程 ID
     * @return Mono<Void>
     */
    Mono<Void> unbindThread(String threadId);
    
    /**
     * 更新线程元数据
     * 
     * @param threadId 线程 ID
     * @param metadata 元数据
     * @return Mono<Void>
     */
    Mono<Void> updateThreadMetadata(String threadId, Map<String, String> metadata);
    
    /**
     * 创建线程请求
     */
    class CreateThreadRequest {
        private final String chatId;
        private final String parentMessageId;
        private final String name;
        private final String initialMessage;
        private final Map<String, Object> metadata;
        private final boolean autoArchive;
        
        private CreateThreadRequest(Builder builder) {
            this.chatId = builder.chatId;
            this.parentMessageId = builder.parentMessageId;
            this.name = builder.name;
            this.initialMessage = builder.initialMessage;
            this.metadata = builder.metadata;
            this.autoArchive = builder.autoArchive;
        }
        
        // Getters
        public String getChatId() { return chatId; }
        public String getParentMessageId() { return parentMessageId; }
        public String getName() { return name; }
        public String getInitialMessage() { return initialMessage; }
        public Map<String, Object> getMetadata() { return metadata; }
        public boolean isAutoArchive() { return autoArchive; }
        
        public static Builder builder() {
            return new Builder();
        }
        
        public static class Builder {
            private String chatId;
            private String parentMessageId;
            private String name;
            private String initialMessage;
            private Map<String, Object> metadata = Map.of();
            private boolean autoArchive = true;
            
            public Builder chatId(String chatId) {
                this.chatId = chatId;
                return this;
            }
            
            public Builder parentMessageId(String parentMessageId) {
                this.parentMessageId = parentMessageId;
                return this;
            }
            
            public Builder name(String name) {
                this.name = name;
                return this;
            }
            
            public Builder initialMessage(String initialMessage) {
                this.initialMessage = initialMessage;
                return this;
            }
            
            public Builder metadata(Map<String, Object> metadata) {
                this.metadata = metadata;
                return this;
            }
            
            public Builder autoArchive(boolean autoArchive) {
                this.autoArchive = autoArchive;
                return this;
            }
            
            public CreateThreadRequest build() {
                if (chatId == null || name == null) {
                    throw new IllegalArgumentException("chatId and name are required");
                }
                return new CreateThreadRequest(this);
            }
        }
    }
    
    /**
     * 线程信息
     */
    class ThreadInfo {
        private final String threadId;
        private final String chatId;
        private final String name;
        private final String parentMessageId;
        private final long createdAt;
        private final long lastActivityAt;
        private final int messageCount;
        private final boolean isArchived;
        private final Map<String, Object> metadata;
        
        public ThreadInfo(String threadId, String chatId, String name,
                         String parentMessageId, long createdAt,
                         long lastActivityAt, int messageCount,
                         boolean isArchived, Map<String, Object> metadata) {
            this.threadId = threadId;
            this.chatId = chatId;
            this.name = name;
            this.parentMessageId = parentMessageId;
            this.createdAt = createdAt;
            this.lastActivityAt = lastActivityAt;
            this.messageCount = messageCount;
            this.isArchived = isArchived;
            this.metadata = metadata;
        }
        
        // Getters
        public String getThreadId() { return threadId; }
        public String getChatId() { return chatId; }
        public String getName() { return name; }
        public String getParentMessageId() { return parentMessageId; }
        public long getCreatedAt() { return createdAt; }
        public long getLastActivityAt() { return lastActivityAt; }
        public int getMessageCount() { return messageCount; }
        public boolean isArchived() { return isArchived; }
        public Map<String, Object> getMetadata() { return metadata; }
    }
}
