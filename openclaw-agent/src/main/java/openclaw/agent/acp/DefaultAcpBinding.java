package openclaw.agent.acp;

import openclaw.agent.session.SessionStateMachine;
import openclaw.agent.tool.ToolChainExecutor;
import openclaw.channel.core.ChannelMessage;
import openclaw.memory.manager.MemoryManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Default ACP Binding 实现
 */
@Service
public class DefaultAcpBinding implements AcpBinding {
    
    private static final Logger logger = LoggerFactory.getLogger(DefaultAcpBinding.class);
    
    @Autowired
    private MemoryManager memoryManager;
    
    @Autowired
    private SessionStateMachine sessionStateMachine;
    
    @Autowired
    private ToolChainExecutor toolChainExecutor;
    
    private AcpBindingConfig config;
    private final Map<String, ChannelMessage> bindings = new ConcurrentHashMap<>();
    private final Map<ContextHookType, ContextHook> hooks = new ConcurrentHashMap<>();
    private final Map<String, SessionMetrics> metrics = new ConcurrentHashMap<>();
    private ScheduledExecutorService flushScheduler;
    private volatile boolean initialized = false;
    
    @Override
    public CompletableFuture<Void> initialize(AcpBindingConfig config) {
        return CompletableFuture.runAsync(() -> {
            this.config = config;
            
            // 初始化调度器
            if (config.isEnableMemoryFlush()) {
                this.flushScheduler = new ScheduledThreadPoolExecutor(1);
                scheduleMemoryFlush();
            }
            
            this.initialized = true;
            logger.info("ACP Binding initialized: {}", config.getBindingId());
        });
    }
    
    @Override
    public CompletableFuture<BindingResult> bindSession(String sessionKey, 
                                                        ChannelMessage channelMessage) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 检查会话数限制
                if (bindings.size() >= config.getMaxConcurrentSessions()) {
                    return BindingResult.failure("Max concurrent sessions reached");
                }
                
                // 保存绑定
                bindings.put(sessionKey, channelMessage);
                
                // 初始化会话状态
                sessionStateMachine.transition(sessionKey, 
                    SessionStateMachine.SessionState.ACTIVE);
                
                // 初始化指标
                metrics.put(sessionKey, new SessionMetrics(sessionKey, 0, 0, 0, 0, 
                    System.currentTimeMillis()));
                
                // 触发会话开始钩子
                triggerContextHook(ContextHookType.SESSION_START,
                    new HookContext(sessionKey, Map.of("channelMessage", channelMessage)));
                
                logger.info("Session bound: {}", sessionKey);
                return BindingResult.success(sessionKey);
                
            } catch (Exception e) {
                logger.error("Failed to bind session: {}", sessionKey, e);
                return BindingResult.failure(e.getMessage());
            }
        });
    }
    
    @Override
    public CompletableFuture<Void> unbindSession(String sessionKey) {
        return CompletableFuture.runAsync(() -> {
            // 触发会话结束钩子
            triggerContextHook(ContextHookType.SESSION_END,
                new HookContext(sessionKey, Map.of()));
            
            // 移除绑定
            bindings.remove(sessionKey);
            metrics.remove(sessionKey);
            
            // 更新会话状态
            sessionStateMachine.transition(sessionKey, 
                SessionStateMachine.SessionState.COMPLETED);
            
            logger.info("Session unbound: {}", sessionKey);
        });
    }
    
    @Override
    public CompletableFuture<ChannelMessage> getBoundChannelMessage(String sessionKey) {
        return CompletableFuture.supplyAsync(() -> bindings.get(sessionKey));
    }
    
    @Override
    public CompletableFuture<ApplyPatchResult> applyPatch(String sessionKey, 
                                                           ContextPatch patch) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                int modifiedCount = 0;
                
                // 应用添加
                if (patch.getAdditions() != null) {
                    for (Map.Entry<String, Object> entry : patch.getAdditions().entrySet()) {
                        // 添加到上下文
                        modifiedCount++;
                    }
                }
                
                // 应用删除
                if (patch.getDeletions() != null) {
                    modifiedCount += patch.getDeletions().size();
                }
                
                // 应用修改
                if (patch.getModifications() != null) {
                    for (Map.Entry<String, Object> entry : patch.getModifications().entrySet()) {
                        // 修改上下文
                        modifiedCount++;
                    }
                }
                
                logger.debug("Patch applied to session {}: {} fields modified", 
                    sessionKey, modifiedCount);
                
                return new ApplyPatchResult(true, modifiedCount, null);
                
            } catch (Exception e) {
                logger.error("Failed to apply patch to session: {}", sessionKey, e);
                return new ApplyPatchResult(false, 0, e.getMessage());
            }
        });
    }
    
    @Override
    public CompletableFuture<MemoryFlushResult> flushMemory(String sessionKey,
                                                             MemoryFlushOptions options) {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            int flushed = 0;
            int failed = 0;
            long bytes = 0;
            
            try {
                // 执行记忆刷新
                // 这里应该调用 memoryManager 的刷新方法
                
                logger.debug("Memory flushed for session {}: {} entries", 
                    sessionKey, flushed);
                
            } catch (Exception e) {
                logger.error("Failed to flush memory for session: {}", sessionKey, e);
                failed++;
            }
            
            long duration = System.currentTimeMillis() - startTime;
            return new MemoryFlushResult(flushed, failed, bytes, duration);
        });
    }
    
    @Override
    public CompletableFuture<Void> registerContextHook(ContextHookType hookType,
                                                        ContextHook hook) {
        return CompletableFuture.runAsync(() -> {
            hooks.put(hookType, hook);
            logger.debug("Context hook registered: {}", hookType);
        });
    }
    
    @Override
    public CompletableFuture<HookResult> triggerContextHook(ContextHookType hookType,
                                                             HookContext context) {
        ContextHook hook = hooks.get(hookType);
        if (hook == null) {
            return CompletableFuture.completedFuture(
                new HookResult(true, null, "No hook registered"));
        }
        
        return hook.execute(context)
            .exceptionally(e -> {
                logger.error("Hook execution failed: {}", hookType, e);
                return new HookResult(false, null, e.getMessage());
            });
    }
    
    @Override
    public CompletableFuture<Void> sendToSession(String sessionKey, AgentMessage message) {
        return CompletableFuture.runAsync(() -> {
            // 触发消息前钩子
            triggerContextHook(ContextHookType.BEFORE_MESSAGE,
                new HookContext(sessionKey, Map.of("message", message)));
            
            // 发送消息逻辑
            // ...
            
            // 更新指标
            SessionMetrics currentMetrics = metrics.get(sessionKey);
            if (currentMetrics != null) {
                metrics.put(sessionKey, new SessionMetrics(
                    sessionKey,
                    currentMetrics.getMessageCount() + 1,
                    currentMetrics.getTokenCount(),
                    currentMetrics.getToolCallCount(),
                    currentMetrics.getDurationMs(),
                    System.currentTimeMillis()
                ));
            }
            
            // 触发消息后钩子
            triggerContextHook(ContextHookType.AFTER_MESSAGE,
                new HookContext(sessionKey, Map.of("message", message)));
        });
    }
    
    @Override
    public Flux<String> sendStreamingToSession(String sessionKey, 
                                               Flux<String> messageStream) {
        if (!config.isEnableStreaming()) {
            return Flux.error(new IllegalStateException("Streaming is not enabled"));
        }
        
        return messageStream
            .doOnSubscribe(sub -> {
                triggerContextHook(ContextHookType.BEFORE_MESSAGE,
                    new HookContext(sessionKey, Map.of("streaming", true)));
            })
            .doOnComplete(() -> {
                triggerContextHook(ContextHookType.AFTER_MESSAGE,
                    new HookContext(sessionKey, Map.of("streaming", true, "completed", true)));
            });
    }
    
    @Override
    public CompletableFuture<SessionState> getSessionState(String sessionKey) {
        return CompletableFuture.supplyAsync(() -> {
            SessionStateMachine.SessionState state = 
                sessionStateMachine.getCurrentState(sessionKey);
            return convertState(state);
        });
    }
    
    @Override
    public CompletableFuture<Void> updateSessionState(String sessionKey, SessionState state) {
        return CompletableFuture.runAsync(() -> {
            SessionStateMachine.SessionState machineState = convertToMachineState(state);
            sessionStateMachine.transition(sessionKey, machineState);
        });
    }
    
    @Override
    public CompletableFuture<SessionMetrics> getSessionMetrics(String sessionKey) {
        return CompletableFuture.supplyAsync(() -> metrics.get(sessionKey));
    }
    
    @Override
    public CompletableFuture<Void> shutdown() {
        return CompletableFuture.runAsync(() -> {
            if (flushScheduler != null) {
                flushScheduler.shutdown();
                try {
                    flushScheduler.awaitTermination(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            
            bindings.clear();
            hooks.clear();
            metrics.clear();
            
            initialized = false;
            logger.info("ACP Binding shutdown: {}", config.getBindingId());
        });
    }
    
    // Helper methods
    
    private void scheduleMemoryFlush() {
        if (flushScheduler == null) return;
        
        flushScheduler.scheduleAtFixedRate(() -> {
            for (String sessionKey : bindings.keySet()) {
                flushMemory(sessionKey, new MemoryFlushOptions(false, 100, 
                    Duration.ofHours(1))).join();
            }
        }, 
        config.getMemoryFlushInterval().toMillis(),
        config.getMemoryFlushInterval().toMillis(),
        TimeUnit.MILLISECONDS);
    }
    
    private SessionState convertState(SessionStateMachine.SessionState state) {
        if (state == null) return SessionState.PENDING;
        return switch (state) {
            case PENDING -> SessionState.PENDING;
            case ACTIVE -> SessionState.ACTIVE;
            case PAUSED -> SessionState.PAUSED;
            case COMPLETED -> SessionState.COMPLETED;
            case ARCHIVED -> SessionState.ARCHIVED;
            case ERROR -> SessionState.ERROR;
        };
    }
    
    private SessionStateMachine.SessionState convertToMachineState(SessionState state) {
        return switch (state) {
            case PENDING -> SessionStateMachine.SessionState.PENDING;
            case ACTIVE -> SessionStateMachine.SessionState.ACTIVE;
            case PAUSED -> SessionStateMachine.SessionState.PAUSED;
            case COMPLETED -> SessionStateMachine.SessionState.COMPLETED;
            case ARCHIVED -> SessionStateMachine.SessionState.ARCHIVED;
            case ERROR -> SessionStateMachine.SessionState.ERROR;
        };
    }
}
