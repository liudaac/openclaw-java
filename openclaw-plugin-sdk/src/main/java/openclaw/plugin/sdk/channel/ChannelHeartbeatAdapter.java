package openclaw.plugin.sdk.channel;

import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Channel Heartbeat Adapter - 心跳检测适配器
 * 
 * 功能:
 * - 发送心跳消息
 * - 检测连接健康状态
 * - 自动重连机制
 * - 连接状态监控
 * 
 * 对应 Node.js: src/channels/heartbeat.ts
 */
public interface ChannelHeartbeatAdapter extends ChannelAdapter {
    
    /**
     * 检查通道是否支持心跳
     */
    boolean supportsHeartbeat();
    
    /**
     * 发送心跳
     * 
     * @return Mono<Boolean> 是否成功
     */
    Mono<Boolean> sendHeartbeat();
    
    /**
     * 检查连接健康状态
     * 
     * @return Mono<HealthStatus>
     */
    Mono<HealthStatus> checkHealth();
    
    /**
     * 获取最后心跳时间
     * 
     * @return Instant
     */
    Instant getLastHeartbeatTime();
    
    /**
     * 获取心跳统计
     * 
     * @return HeartbeatStats
     */
    HeartbeatStats getHeartbeatStats();
    
    /**
     * 配置心跳
     * 
     * @param config 心跳配置
     */
    void configureHeartbeat(HeartbeatConfig config);
    
    /**
     * 启动心跳
     * 
     * @return Mono<Void>
     */
    Mono<Void> startHeartbeat();
    
    /**
     * 停止心跳
     * 
     * @return Mono<Void>
     */
    Mono<Void> stopHeartbeat();
    
    /**
     * 触发重连
     * 
     * @return Mono<Boolean> 是否成功
     */
    Mono<Boolean> reconnect();
    
    /**
     * 获取连接状态
     * 
     * @return ConnectionStatus
     */
    ConnectionStatus getConnectionStatus();
    
    /**
     * 注册连接状态监听器
     * 
     * @param listener 监听器
     */
    void registerStatusListener(ConnectionStatusListener listener);
    
    /**
     * 移除连接状态监听器
     * 
     * @param listener 监听器
     */
    void removeStatusListener(ConnectionStatusListener listener);
    
    /**
     * 健康状态
     */
    class HealthStatus {
        private final boolean healthy;
        private final String status;
        private final long latencyMs;
        private final String message;
        private final Instant checkedAt;
        
        public HealthStatus(boolean healthy, String status, long latencyMs, 
                           String message, Instant checkedAt) {
            this.healthy = healthy;
            this.status = status;
            this.latencyMs = latencyMs;
            this.message = message;
            this.checkedAt = checkedAt;
        }
        
        // Getters
        public boolean isHealthy() { return healthy; }
        public String getStatus() { return status; }
        public long getLatencyMs() { return latencyMs; }
        public String getMessage() { return message; }
        public Instant getCheckedAt() { return checkedAt; }
        
        // Factory methods
        public static HealthStatus healthy(long latencyMs) {
            return new HealthStatus(true, "HEALTHY", latencyMs, 
                "Connection is healthy", Instant.now());
        }
        
        public static HealthStatus unhealthy(String message) {
            return new HealthStatus(false, "UNHEALTHY", -1, 
                message, Instant.now());
        }
        
        public static HealthStatus degraded(String message, long latencyMs) {
            return new HealthStatus(true, "DEGRADED", latencyMs, 
                message, Instant.now());
        }
    }
    
    /**
     * 心跳统计
     */
    class HeartbeatStats {
        private final long totalHeartbeats;
        private final long successfulHeartbeats;
        private final long failedHeartbeats;
        private final long consecutiveFailures;
        private final double successRate;
        private final Duration averageLatency;
        private final Instant startedAt;
        private final Instant lastSuccessAt;
        private final Instant lastFailureAt;
        
        public HeartbeatStats(long totalHeartbeats, long successfulHeartbeats,
                             long failedHeartbeats, long consecutiveFailures,
                             double successRate, Duration averageLatency,
                             Instant startedAt, Instant lastSuccessAt, 
                             Instant lastFailureAt) {
            this.totalHeartbeats = totalHeartbeats;
            this.successfulHeartbeats = successfulHeartbeats;
            this.failedHeartbeats = failedHeartbeats;
            this.consecutiveFailures = consecutiveFailures;
            this.successRate = successRate;
            this.averageLatency = averageLatency;
            this.startedAt = startedAt;
            this.lastSuccessAt = lastSuccessAt;
            this.lastFailureAt = lastFailureAt;
        }
        
        // Getters
        public long getTotalHeartbeats() { return totalHeartbeats; }
        public long getSuccessfulHeartbeats() { return successfulHeartbeats; }
        public long getFailedHeartbeats() { return failedHeartbeats; }
        public long getConsecutiveFailures() { return consecutiveFailures; }
        public double getSuccessRate() { return successRate; }
        public Duration getAverageLatency() { return averageLatency; }
        public Instant getStartedAt() { return startedAt; }
        public Instant getLastSuccessAt() { return lastSuccessAt; }
        public Instant getLastFailureAt() { return lastFailureAt; }
        
        public boolean needsReconnect(int maxConsecutiveFailures) {
            return consecutiveFailures >= maxConsecutiveFailures;
        }
    }
    
    /**
     * 心跳配置
     */
    class HeartbeatConfig {
        private final Duration interval;
        private final Duration timeout;
        private final int maxConsecutiveFailures;
        private final boolean autoReconnect;
        private final Duration reconnectDelay;
        private final int maxReconnectAttempts;
        private final boolean exponentialBackoff;
        
        public HeartbeatConfig() {
            this(Duration.ofSeconds(30), Duration.ofSeconds(10), 
                 3, true, Duration.ofSeconds(5), 5, true);
        }
        
        public HeartbeatConfig(Duration interval, Duration timeout,
                              int maxConsecutiveFailures, boolean autoReconnect,
                              Duration reconnectDelay, int maxReconnectAttempts,
                              boolean exponentialBackoff) {
            this.interval = interval;
            this.timeout = timeout;
            this.maxConsecutiveFailures = maxConsecutiveFailures;
            this.autoReconnect = autoReconnect;
            this.reconnectDelay = reconnectDelay;
            this.maxReconnectAttempts = maxReconnectAttempts;
            this.exponentialBackoff = exponentialBackoff;
        }
        
        // Getters
        public Duration getInterval() { return interval; }
        public Duration getTimeout() { return timeout; }
        public int getMaxConsecutiveFailures() { return maxConsecutiveFailures; }
        public boolean isAutoReconnect() { return autoReconnect; }
        public Duration getReconnectDelay() { return reconnectDelay; }
        public int getMaxReconnectAttempts() { return maxReconnectAttempts; }
        public boolean isExponentialBackoff() { return exponentialBackoff; }
        
        /**
         * 计算重连延迟
         */
        public Duration calculateReconnectDelay(int attempt) {
            if (!exponentialBackoff) {
                return reconnectDelay;
            }
            long delayMs = reconnectDelay.toMillis() * (long) Math.pow(2, attempt - 1);
            return Duration.ofMillis(Math.min(delayMs, 60000)); // 最大 60 秒
        }
    }
    
    /**
     * 连接状态
     */
    enum ConnectionStatus {
        DISCONNECTED("Disconnected"),
        CONNECTING("Connecting"),
        CONNECTED("Connected"),
        RECONNECTING("Reconnecting"),
        DEGRADED("Degraded"),
        ERROR("Error");
        
        private final String displayName;
        
        ConnectionStatus(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() { return displayName; }
        
        public boolean isConnected() {
            return this == CONNECTED || this == DEGRADED;
        }
        
        public boolean isReconnecting() {
            return this == RECONNECTING || this == CONNECTING;
        }
    }
    
    /**
     * 连接状态监听器
     */
    @FunctionalInterface
    interface ConnectionStatusListener {
        void onStatusChanged(ConnectionStatus oldStatus, ConnectionStatus newStatus, 
                            String message);
    }
    
    /**
     * 默认心跳适配器实现基类
     */
    abstract class AbstractHeartbeatAdapter implements ChannelHeartbeatAdapter {
        
        protected HeartbeatConfig config;
        protected volatile ConnectionStatus status = ConnectionStatus.DISCONNECTED;
        protected volatile Instant lastHeartbeatTime;
        protected final Map<String, ConnectionStatusListener> listeners = new ConcurrentHashMap<>();
        
        protected long totalHeartbeats = 0;
        protected long successfulHeartbeats = 0;
        protected long failedHeartbeats = 0;
        protected long consecutiveFailures = 0;
        protected long totalLatencyMs = 0;
        protected Instant startedAt;
        protected Instant lastSuccessAt;
        protected Instant lastFailureAt;
        
        @Override
        public void configureHeartbeat(HeartbeatConfig config) {
            this.config = config;
        }
        
        @Override
        public Instant getLastHeartbeatTime() {
            return lastHeartbeatTime;
        }
        
        @Override
        public HeartbeatStats getHeartbeatStats() {
            double successRate = totalHeartbeats > 0 
                ? (double) successfulHeartbeats / totalHeartbeats * 100 
                : 0;
            Duration avgLatency = successfulHeartbeats > 0 
                ? Duration.ofMillis(totalLatencyMs / successfulHeartbeats) 
                : Duration.ZERO;
            
            return new HeartbeatStats(
                totalHeartbeats, successfulHeartbeats, failedHeartbeats,
                consecutiveFailures, successRate, avgLatency,
                startedAt, lastSuccessAt, lastFailureAt
            );
        }
        
        @Override
        public ConnectionStatus getConnectionStatus() {
            return status;
        }
        
        @Override
        public void registerStatusListener(ConnectionStatusListener listener) {
            listeners.put(listener.toString(), listener);
        }
        
        @Override
        public void removeStatusListener(ConnectionStatusListener listener) {
            listeners.remove(listener.toString());
        }
        
        protected void updateStatus(ConnectionStatus newStatus, String message) {
            ConnectionStatus oldStatus = this.status;
            this.status = newStatus;
            
            // 通知监听器
            for (ConnectionStatusListener listener : listeners.values()) {
                try {
                    listener.onStatusChanged(oldStatus, newStatus, message);
                } catch (Exception e) {
                    // 忽略监听器错误
                }
            }
        }
        
        protected void recordSuccess(long latencyMs) {
            totalHeartbeats++;
            successfulHeartbeats++;
            consecutiveFailures = 0;
            totalLatencyMs += latencyMs;
            lastSuccessAt = Instant.now();
            lastHeartbeatTime = Instant.now();
        }
        
        protected void recordFailure() {
            totalHeartbeats++;
            failedHeartbeats++;
            consecutiveFailures++;
            lastFailureAt = Instant.now();
            lastHeartbeatTime = Instant.now();
        }
    }
}
