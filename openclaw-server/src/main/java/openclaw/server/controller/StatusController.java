package openclaw.server.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import openclaw.server.websocket.GatewayWebSocketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.RuntimeMXBean;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 状态监控 API
 * 
 * 提供系统状态、健康检查、指标数据
 * 对应 Node.js Control UI 的 status, health, models.list 等方法
 */
@RestController
@RequestMapping("/api")
public class StatusController {
    
    private static final Logger logger = LoggerFactory.getLogger(StatusController.class);
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private GatewayWebSocketHandler webSocketHandler;
    
    private final Instant startTime = Instant.now();
    
    /**
     * 获取状态快照
     * 
     * GET /api/status
     */
    @GetMapping("/status")
    public Mono<ResponseEntity<ObjectNode>> getStatus() {
        return Mono.fromCallable(() -> {
            ObjectNode status = objectMapper.createObjectNode();
            
            // 基本信息
            status.put("version", "2026.3.9");
            status.put("status", "running");
            status.put("uptime", getUptime());
            status.put("uptimeText", formatDuration(getUptime()));
            
            // 连接信息
            ObjectNode connections = objectMapper.createObjectNode();
            connections.put("websocket", webSocketHandler.getConnectionCount());
            connections.put("maxConnections", 100);
            status.set("connections", connections);
            
            // 内存信息
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
            MemoryUsage nonHeapUsage = memoryBean.getNonHeapMemoryUsage();
            
            ObjectNode memory = objectMapper.createObjectNode();
            memory.put("heapUsed", heapUsage.getUsed());
            memory.put("heapCommitted", heapUsage.getCommitted());
            memory.put("heapMax", heapUsage.getMax());
            memory.put("nonHeapUsed", nonHeapUsage.getUsed());
            memory.put("nonHeapCommitted", nonHeapUsage.getCommitted());
            status.set("memory", memory);
            
            // 运行时信息
            RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
            ObjectNode runtime = objectMapper.createObjectNode();
            runtime.put("pid", runtimeBean.getPid());
            runtime.put("javaVersion", System.getProperty("java.version"));
            runtime.put("javaVendor", System.getProperty("java.vendor"));
            runtime.put("osName", System.getProperty("os.name"));
            runtime.put("osVersion", System.getProperty("os.version"));
            runtime.put("osArch", System.getProperty("os.arch"));
            runtime.put("availableProcessors", Runtime.getRuntime().availableProcessors());
            status.set("runtime", runtime);
            
            return ResponseEntity.ok(status);
            
        }).onErrorResume(e -> {
            logger.error("Failed to get status", e);
            return Mono.just(ResponseEntity.internalServerError().build());
        });
    }
    
    /**
     * 健康检查
     * 
     * GET /api/health
     */
    @GetMapping("/health")
    public Mono<ResponseEntity<ObjectNode>> getHealth() {
        return Mono.fromCallable(() -> {
            ObjectNode health = objectMapper.createObjectNode();
            
            // 整体健康状态
            boolean healthy = true;
            
            // WebSocket 健康
            ObjectNode websocketHealth = objectMapper.createObjectNode();
            websocketHealth.put("status", "healthy");
            websocketHealth.put("connections", webSocketHandler.getConnectionCount());
            health.set("websocket", websocketHealth);
            
            // 内存健康
            ObjectNode memoryHealth = objectMapper.createObjectNode();
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
            double heapUsagePercent = (double) heapUsage.getUsed() / heapUsage.getMax() * 100;
            
            memoryHealth.put("status", heapUsagePercent < 90 ? "healthy" : "warning");
            memoryHealth.put("usagePercent", heapUsagePercent);
            if (heapUsagePercent >= 90) {
                healthy = false;
            }
            health.set("memory", memoryHealth);
            
            // 整体状态
            health.put("status", healthy ? "healthy" : "degraded");
            health.put("timestamp", Instant.now().toString());
            
            return ResponseEntity.ok(health);
            
        }).onErrorResume(e -> {
            logger.error("Health check failed", e);
            ObjectNode error = objectMapper.createObjectNode();
            error.put("status", "unhealthy");
            error.put("error", e.getMessage());
            return Mono.just(ResponseEntity.internalServerError().body(error));
        });
    }
    
    /**
     * 获取模型列表
     * 
     * GET /api/models
     */
    @GetMapping("/models")
    public Mono<ResponseEntity<ObjectNode>> getModels() {
        return Mono.fromCallable(() -> {
            ObjectNode result = objectMapper.createObjectNode();
            
            ArrayNode models = objectMapper.createArrayNode();
            
            // OpenAI 模型
            ObjectNode gpt4 = objectMapper.createObjectNode();
            gpt4.put("id", "gpt-4");
            gpt4.put("name", "GPT-4");
            gpt4.put("provider", "openai");
            gpt4.put("contextWindow", 8192);
            models.add(gpt4);
            
            ObjectNode gpt4Turbo = objectMapper.createObjectNode();
            gpt4Turbo.put("id", "gpt-4-turbo");
            gpt4Turbo.put("name", "GPT-4 Turbo");
            gpt4Turbo.put("provider", "openai");
            gpt4Turbo.put("contextWindow", 128000);
            models.add(gpt4Turbo);
            
            ObjectNode gpt35 = objectMapper.createObjectNode();
            gpt35.put("id", "gpt-3.5-turbo");
            gpt35.put("name", "GPT-3.5 Turbo");
            gpt35.put("provider", "openai");
            gpt35.put("contextWindow", 16385);
            models.add(gpt35);
            
            // Anthropic 模型
            ObjectNode claude3 = objectMapper.createObjectNode();
            claude3.put("id", "claude-3-opus");
            claude3.put("name", "Claude 3 Opus");
            claude3.put("provider", "anthropic");
            claude3.put("contextWindow", 200000);
            models.add(claude3);
            
            ObjectNode claude35 = objectMapper.createObjectNode();
            claude35.put("id", "claude-3.5-sonnet");
            claude35.put("name", "Claude 3.5 Sonnet");
            claude35.put("provider", "anthropic");
            claude35.put("contextWindow", 200000);
            models.add(claude35);
            
            // Ollama 模型
            ObjectNode llama3 = objectMapper.createObjectNode();
            llama3.put("id", "llama3");
            llama3.put("name", "Llama 3");
            llama3.put("provider", "ollama");
            llama3.put("contextWindow", 8192);
            models.add(llama3);
            
            result.set("models", models);
            result.put("count", models.size());
            
            return ResponseEntity.ok(result);
            
        }).onErrorResume(e -> {
            logger.error("Failed to get models", e);
            return Mono.just(ResponseEntity.internalServerError().build());
        });
    }
    
    /**
     * 获取通道状态
     * 
     * GET /api/channels/status
     */
    @GetMapping("/channels/status")
    public Mono<ResponseEntity<ObjectNode>> getChannelsStatus() {
        return Mono.fromCallable(() -> {
            ObjectNode result = objectMapper.createObjectNode();
            
            ArrayNode channels = objectMapper.createArrayNode();
            
            // Telegram
            ObjectNode telegram = objectMapper.createObjectNode();
            telegram.put("id", "telegram");
            telegram.put("name", "Telegram");
            telegram.put("status", "connected");
            telegram.put("type", "messaging");
            channels.add(telegram);
            
            // Feishu
            ObjectNode feishu = objectMapper.createObjectNode();
            feishu.put("id", "feishu");
            feishu.put("name", "Feishu");
            feishu.put("status", "connected");
            feishu.put("type", "messaging");
            channels.add(feishu);
            
            // Discord
            ObjectNode discord = objectMapper.createObjectNode();
            discord.put("id", "discord");
            discord.put("name", "Discord");
            discord.put("status", "connected");
            discord.put("type", "messaging");
            channels.add(discord);
            
            // Slack
            ObjectNode slack = objectMapper.createObjectNode();
            slack.put("id", "slack");
            slack.put("name", "Slack");
            slack.put("status", "connected");
            slack.put("type", "messaging");
            channels.add(slack);
            
            result.set("channels", channels);
            result.put("count", channels.size());
            result.put("connected", channels.size());
            
            return ResponseEntity.ok(result);
            
        }).onErrorResume(e -> {
            logger.error("Failed to get channels status", e);
            return Mono.just(ResponseEntity.internalServerError().build());
        });
    }
    
    /**
     * 获取会话列表
     * 
     * GET /api/sessions
     */
    @GetMapping("/sessions")
    public Mono<ResponseEntity<ObjectNode>> getSessions() {
        return Mono.fromCallable(() -> {
            ObjectNode result = objectMapper.createObjectNode();
            
            ArrayNode sessions = objectMapper.createArrayNode();
            
            // 示例会话数据
            ObjectNode session1 = objectMapper.createObjectNode();
            session1.put("id", "session-001");
            session1.put("channel", "telegram");
            session1.put("status", "active");
            session1.put("messageCount", 42);
            session1.put("lastActivity", Instant.now().minusSeconds(300).toString());
            sessions.add(session1);
            
            ObjectNode session2 = objectMapper.createObjectNode();
            session2.put("id", "session-002");
            session2.put("channel", "feishu");
            session2.put("status", "active");
            session2.put("messageCount", 15);
            session2.put("lastActivity", Instant.now().minusSeconds(600).toString());
            sessions.add(session2);
            
            result.set("sessions", sessions);
            result.put("count", sessions.size());
            result.put("active", sessions.size());
            
            return ResponseEntity.ok(result);
            
        }).onErrorResume(e -> {
            logger.error("Failed to get sessions", e);
            return Mono.just(ResponseEntity.internalServerError().build());
        });
    }
    
    /**
     * 获取系统信息
     * 
     * GET /api/system/info
     */
    @GetMapping("/system/info")
    public Mono<ResponseEntity<ObjectNode>> getSystemInfo() {
        return Mono.fromCallable(() -> {
            ObjectNode info = objectMapper.createObjectNode();
            
            // 版本信息
            info.put("version", "2026.3.9");
            info.put("buildTime", "2026-03-12");
            info.put("commitHash", "abc123");
            
            // Java 信息
            info.put("javaVersion", System.getProperty("java.version"));
            info.put("javaVendor", System.getProperty("java.vendor"));
            info.put("javaHome", System.getProperty("java.home"));
            
            // 操作系统信息
            info.put("osName", System.getProperty("os.name"));
            info.put("osVersion", System.getProperty("os.version"));
            info.put("osArch", System.getProperty("os.arch"));
            
            // 运行时信息
            Runtime runtime = Runtime.getRuntime();
            info.put("availableProcessors", runtime.availableProcessors());
            info.put("maxMemory", runtime.maxMemory());
            info.put("totalMemory", runtime.totalMemory());
            info.put("freeMemory", runtime.freeMemory());
            
            return ResponseEntity.ok(info);
            
        }).onErrorResume(e -> {
            logger.error("Failed to get system info", e);
            return Mono.just(ResponseEntity.internalServerError().build());
        });
    }
    
    // Helper methods
    
    private long getUptime() {
        return Duration.between(startTime, Instant.now()).getSeconds();
    }
    
    private String formatDuration(long seconds) {
        Duration duration = Duration.ofSeconds(seconds);
        long days = duration.toDays();
        long hours = duration.toHoursPart();
        long minutes = duration.toMinutesPart();
        long secs = duration.toSecondsPart();
        
        if (days > 0) {
            return String.format("%dd %dh %dm %ds", days, hours, minutes, secs);
        } else if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes, secs);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, secs);
        } else {
            return String.format("%ds", secs);
        }
    }
}
