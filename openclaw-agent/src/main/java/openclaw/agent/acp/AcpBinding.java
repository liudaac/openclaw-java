package openclaw.agent.acp;

import openclaw.agent.tool.ToolCall;
import openclaw.agent.tool.ToolResult;
import openclaw.channel.core.ChannelMessage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * ACP (Agent Communication Protocol) Binding - 完整协议绑定
 * 
 * 功能:
 * - 会话生命周期管理
 * - 上下文钩子
 * - 记忆刷新
 * - 补丁应用
 * - 子代理协调
 * 
 * 对应 Node.js: src/agents/acp/binding.ts
 */
public interface AcpBinding {
    
    /**
     * 初始化 ACP 绑定
     * 
     * @param config ACP 配置
     * @return CompletableFuture<Void>
     */
    CompletableFuture<Void> initialize(AcpBindingConfig config);
    
    /**
     * 绑定会话到通道
     * 
     * @param sessionKey 会话 Key
     * @param channelMessage 通道消息
     * @return CompletableFuture<BindingResult>
     */
    CompletableFuture<BindingResult> bindSession(String sessionKey, 
                                                  ChannelMessage channelMessage);
    
    /**
     * 解绑会话
     * 
     * @param sessionKey 会话 Key
     * @return CompletableFuture<Void>
     */
    CompletableFuture<Void> unbindSession(String sessionKey);
    
    /**
     * 获取绑定的通道消息
     * 
     * @param sessionKey 会话 Key
     * @return CompletableFuture<ChannelMessage>
     */
    CompletableFuture<ChannelMessage> getBoundChannelMessage(String sessionKey);
    
    /**
     * 应用上下文补丁
     * 
     * @param sessionKey 会话 Key
     * @param patch 补丁
     * @return CompletableFuture<ApplyPatchResult>
     */
    CompletableFuture<ApplyPatchResult> applyPatch(String sessionKey, 
                                                    ContextPatch patch);
    
    /**
     * 执行记忆刷新
     * 
     * @param sessionKey 会话 Key
     * @param options 刷新选项
     * @return CompletableFuture<MemoryFlushResult>
     */
    CompletableFuture<MemoryFlushResult> flushMemory(String sessionKey,
                                                      MemoryFlushOptions options);
    
    /**
     * 注册上下文钩子
     * 
     * @param hookType 钩子类型
     * @param hook 钩子函数
     * @return CompletableFuture<Void>
     */
    CompletableFuture<Void> registerContextHook(ContextHookType hookType,
                                                 ContextHook hook);
    
    /**
     * 触发上下文钩子
     * 
     * @param hookType 钩子类型
     * @param context 上下文
     * @return CompletableFuture<HookResult>
     */
    CompletableFuture<HookResult> triggerContextHook(ContextHookType hookType,
                                                      HookContext context);
    
    /**
     * 发送消息到会话
     * 
     * @param sessionKey 会话 Key
     * @param message 消息
     * @return CompletableFuture<Void>
     */
    CompletableFuture<Void> sendToSession(String sessionKey, AgentMessage message);
    
    /**
     * 发送流式消息到会话
     * 
     * @param sessionKey 会话 Key
     * @param messageStream 消息流
     * @return Flux<String>
     */
    Flux<String> sendStreamingToSession(String sessionKey, 
                                        Flux<String> messageStream);
    
    /**
     * 获取会话状态
     * 
     * @param sessionKey 会话 Key
     * @return CompletableFuture<SessionState>
     */
    CompletableFuture<SessionState> getSessionState(String sessionKey);
    
    /**
     * 更新会话状态
     * 
     * @param sessionKey 会话 Key
     * @param state 新状态
     * @return CompletableFuture<Void>
     */
    CompletableFuture<Void> updateSessionState(String sessionKey, SessionState state);
    
    /**
     * 获取会话指标
     * 
     * @param sessionKey 会话 Key
     * @return CompletableFuture<SessionMetrics>
     */
    CompletableFuture<SessionMetrics> getSessionMetrics(String sessionKey);
    
    /**
     * 关闭 ACP 绑定
     * 
     * @return CompletableFuture<Void>
     */
    CompletableFuture<Void> shutdown();
    
    // Configuration
    
    class AcpBindingConfig {
        private final String bindingId;
        private final String channelType;
        private final boolean enableStreaming;
        private final boolean enableMemoryFlush;
        private final Duration memoryFlushInterval;
        private final int maxConcurrentSessions;
        private final Map<String, Object> metadata;
        
        public AcpBindingConfig(String bindingId, String channelType) {
            this(bindingId, channelType, true, true, 
                 Duration.ofMinutes(5), 100, Map.of());
        }
        
        public AcpBindingConfig(String bindingId, String channelType,
                                boolean enableStreaming, boolean enableMemoryFlush,
                                Duration memoryFlushInterval, int maxConcurrentSessions,
                                Map<String, Object> metadata) {
            this.bindingId = bindingId;
            this.channelType = channelType;
            this.enableStreaming = enableStreaming;
            this.enableMemoryFlush = enableMemoryFlush;
            this.memoryFlushInterval = memoryFlushInterval;
            this.maxConcurrentSessions = maxConcurrentSessions;
            this.metadata = metadata;
        }
        
        // Getters
        public String getBindingId() { return bindingId; }
        public String getChannelType() { return channelType; }
        public boolean isEnableStreaming() { return enableStreaming; }
        public boolean isEnableMemoryFlush() { return enableMemoryFlush; }
        public Duration getMemoryFlushInterval() { return memoryFlushInterval; }
        public int getMaxConcurrentSessions() { return maxConcurrentSessions; }
        public Map<String, Object> getMetadata() { return metadata; }
    }
    
    // Result types
    
    class BindingResult {
        private final boolean success;
        private final String sessionKey;
        private final String errorMessage;
        
        public BindingResult(boolean success, String sessionKey, String errorMessage) {
            this.success = success;
            this.sessionKey = sessionKey;
            this.errorMessage = errorMessage;
        }
        
        public boolean isSuccess() { return success; }
        public String getSessionKey() { return sessionKey; }
        public String getErrorMessage() { return errorMessage; }
        
        public static BindingResult success(String sessionKey) {
            return new BindingResult(true, sessionKey, null);
        }
        
        public static BindingResult failure(String errorMessage) {
            return new BindingResult(false, null, errorMessage);
        }
    }
    
    class ApplyPatchResult {
        private final boolean success;
        private final int modifiedFields;
        private final String errorMessage;
        
        public ApplyPatchResult(boolean success, int modifiedFields, String errorMessage) {
            this.success = success;
            this.modifiedFields = modifiedFields;
            this.errorMessage = errorMessage;
        }
        
        public boolean isSuccess() { return success; }
        public int getModifiedFields() { return modifiedFields; }
        public String getErrorMessage() { return errorMessage; }
    }
    
    class MemoryFlushResult {
        private final int entriesFlushed;
        private final int entriesFailed;
        private final long bytesFlushed;
        private final long durationMs;
        
        public MemoryFlushResult(int entriesFlushed, int entriesFailed,
                                long bytesFlushed, long durationMs) {
            this.entriesFlushed = entriesFlushed;
            this.entriesFailed = entriesFailed;
            this.bytesFlushed = bytesFlushed;
            this.durationMs = durationMs;
        }
        
        public int getEntriesFlushed() { return entriesFlushed; }
        public int getEntriesFailed() { return entriesFailed; }
        public long getBytesFlushed() { return bytesFlushed; }
        public long getDurationMs() { return durationMs; }
    }
    
    class HookResult {
        private final boolean success;
        private final Object result;
        private final String errorMessage;
        
        public HookResult(boolean success, Object result, String errorMessage) {
            this.success = success;
            this.result = result;
            this.errorMessage = errorMessage;
        }
        
        public boolean isSuccess() { return success; }
        public Object getResult() { return result; }
        public String getErrorMessage() { return errorMessage; }
    }
    
    // Data types
    
    class ContextPatch {
        private final String sessionKey;
        private final Map<String, Object> additions;
        private final List<String> deletions;
        private final Map<String, Object> modifications;
        
        public ContextPatch(String sessionKey, Map<String, Object> additions,
                           List<String> deletions, Map<String, Object> modifications) {
            this.sessionKey = sessionKey;
            this.additions = additions;
            this.deletions = deletions;
            this.modifications = modifications;
        }
        
        public String getSessionKey() { return sessionKey; }
        public Map<String, Object> getAdditions() { return additions; }
        public List<String> getDeletions() { return deletions; }
        public Map<String, Object> getModifications() { return modifications; }
    }
    
    class MemoryFlushOptions {
        private final boolean force;
        private final int maxEntries;
        private final Duration maxAge;
        
        public MemoryFlushOptions(boolean force, int maxEntries, Duration maxAge) {
            this.force = force;
            this.maxEntries = maxEntries;
            this.maxAge = maxAge;
        }
        
        public boolean isForce() { return force; }
        public int getMaxEntries() { return maxEntries; }
        public Duration getMaxAge() { return maxAge; }
    }
    
    class HookContext {
        private final String sessionKey;
        private final Map<String, Object> data;
        private final long timestamp;
        
        public HookContext(String sessionKey, Map<String, Object> data) {
            this.sessionKey = sessionKey;
            this.data = data;
            this.timestamp = System.currentTimeMillis();
        }
        
        public String getSessionKey() { return sessionKey; }
        public Map<String, Object> getData() { return data; }
        public long getTimestamp() { return timestamp; }
    }
    
    class SessionMetrics {
        private final String sessionKey;
        private final long messageCount;
        private final long tokenCount;
        private final long toolCallCount;
        private final long durationMs;
        private final long lastActivityAt;
        
        public SessionMetrics(String sessionKey, long messageCount, long tokenCount,
                             long toolCallCount, long durationMs, long lastActivityAt) {
            this.sessionKey = sessionKey;
            this.messageCount = messageCount;
            this.tokenCount = tokenCount;
            this.toolCallCount = toolCallCount;
            this.durationMs = durationMs;
            this.lastActivityAt = lastActivityAt;
        }
        
        public String getSessionKey() { return sessionKey; }
        public long getMessageCount() { return messageCount; }
        public long getTokenCount() { return tokenCount; }
        public long getToolCallCount() { return toolCallCount; }
        public long getDurationMs() { return durationMs; }
        public long getLastActivityAt() { return lastActivityAt; }
    }
    
    // Enums and interfaces
    
    enum ContextHookType {
        BEFORE_MESSAGE,
        AFTER_MESSAGE,
        BEFORE_TOOL_CALL,
        AFTER_TOOL_CALL,
        BEFORE_COMPACT,
        AFTER_COMPACT,
        SESSION_START,
        SESSION_END
    }
    
    enum SessionState {
        PENDING,
        ACTIVE,
        PAUSED,
        COMPLETED,
        ARCHIVED,
        ERROR
    }
    
    @FunctionalInterface
    interface ContextHook {
        CompletableFuture<HookResult> execute(HookContext context);
    }
}
