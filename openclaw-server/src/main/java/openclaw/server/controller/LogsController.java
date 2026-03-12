package openclaw.server.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

/**
 * 日志 API
 * 
 * 提供日志查看和实时流式日志
 * 对应 Node.js Control UI 的 logs.tail 方法
 */
@RestController
@RequestMapping("/api/logs")
public class LogsController {
    
    private static final Logger logger = LoggerFactory.getLogger(LogsController.class);
    
    @Autowired
    private ObjectMapper objectMapper;
    
    // 日志队列 (用于流式传输)
    private final BlockingQueue<LogEntry> logQueue = new LinkedBlockingQueue<>(1000);
    
    // 最近的日志缓存
    private final List<LogEntry> recentLogs = new ArrayList<>();
    private static final int MAX_RECENT_LOGS = 1000;
    
    /**
     * 获取日志列表
     * 
     * GET /api/logs?limit={limit}&level={level}
     */
    @GetMapping
    public Mono<ResponseEntity<JsonNode>> getLogs(
            @RequestParam(defaultValue = "100") int limit,
            @RequestParam(defaultValue = "INFO") String level) {
        
        return Mono.fromCallable(() -> {
            // 过滤日志
            List<LogEntry> filtered = recentLogs.stream()
                .filter(log -> matchesLevel(log.getLevel(), level))
                .limit(limit)
                .collect(Collectors.toList());
            
            ObjectNode result = objectMapper.createObjectNode();
            ArrayNode logs = objectMapper.createArrayNode();
            
            for (LogEntry entry : filtered) {
                ObjectNode logNode = objectMapper.createObjectNode();
                logNode.put("timestamp", entry.getTimestamp().toString());
                logNode.put("level", entry.getLevel());
                logNode.put("logger", entry.getLogger());
                logNode.put("message", entry.getMessage());
                logs.add(logNode);
            }
            
            result.set("logs", logs);
            result.put("count", logs.size());
            result.put("total", recentLogs.size());
            
            return ResponseEntity.ok(result);
            
        }).onErrorResume(e -> {
            logger.error("Failed to get logs", e);
            return Mono.just(ResponseEntity.internalServerError().build());
        });
    }
    
    /**
     * 实时日志流
     * 
     * GET /api/logs/tail?level={level}
     */
    @GetMapping(value = "/tail", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> tailLogs(@RequestParam(defaultValue = "INFO") String level) {
        return Flux.<String>create(sink -> {
            new Thread(() -> {
                try {
                    while (!sink.isCancelled()) {
                        LogEntry entry = logQueue.take();
                        
                        if (matchesLevel(entry.getLevel(), level)) {
                            ObjectNode logNode = objectMapper.createObjectNode();
                            logNode.put("timestamp", entry.getTimestamp().toString());
                            logNode.put("level", entry.getLevel());
                            logNode.put("logger", entry.getLogger());
                            logNode.put("message", entry.getMessage());
                            
                            sink.next("data: " + logNode.toString() + "\n\n");
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    sink.complete();
                }
            }).start();
        });
    }
    
    /**
     * 搜索日志
     * 
     * GET /api/logs/search?query={query}&limit={limit}
     */
    @GetMapping("/search")
    public Mono<ResponseEntity<JsonNode>> searchLogs(
            @RequestParam String query,
            @RequestParam(defaultValue = "100") int limit) {
        
        return Mono.fromCallable(() -> {
            // 搜索日志
            List<LogEntry> matched = recentLogs.stream()
                .filter(log -> log.getMessage().toLowerCase().contains(query.toLowerCase()))
                .limit(limit)
                .collect(Collectors.toList());
            
            ObjectNode result = objectMapper.createObjectNode();
            ArrayNode logs = objectMapper.createArrayNode();
            
            for (LogEntry entry : matched) {
                ObjectNode logNode = objectMapper.createObjectNode();
                logNode.put("timestamp", entry.getTimestamp().toString());
                logNode.put("level", entry.getLevel());
                logNode.put("logger", entry.getLogger());
                logNode.put("message", entry.getMessage());
                logs.add(logNode);
            }
            
            result.set("logs", logs);
            result.put("count", logs.size());
            result.put("query", query);
            
            return ResponseEntity.ok(result);
            
        }).onErrorResume(e -> {
            logger.error("Failed to search logs", e);
            return Mono.just(ResponseEntity.internalServerError().build());
        });
    }
    
    /**
     * 导出日志
     * 
     * GET /api/logs/export
     */
    @GetMapping("/export")
    public Mono<ResponseEntity<String>> exportLogs(
            @RequestParam(defaultValue = "1000") int limit) {
        
        return Mono.fromCallable(() -> {
            StringBuilder sb = new StringBuilder();
            
            // 添加 CSV 头
            sb.append("timestamp,level,logger,message\n");
            
            // 添加日志
            List<LogEntry> toExport = recentLogs.stream()
                .limit(limit)
                .collect(Collectors.toList());
            
            for (LogEntry entry : toExport) {
                sb.append(String.format("%s,%s,\"%s\",\"%s\"\n",
                    entry.getTimestamp(),
                    entry.getLevel(),
                    escapeCsv(entry.getLogger()),
                    escapeCsv(entry.getMessage())));
            }
            
            return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=logs.csv")
                .contentType(MediaType.TEXT_PLAIN)
                .body(sb.toString());
            
        }).onErrorResume(e -> {
            logger.error("Failed to export logs", e);
            return Mono.just(ResponseEntity.internalServerError().body("Error exporting logs"));
        });
    }
    
    /**
     * 添加日志条目 (供内部使用)
     */
    public void addLog(String level, String loggerName, String message) {
        LogEntry entry = new LogEntry(Instant.now(), level, loggerName, message);
        
        // 添加到队列
        if (!logQueue.offer(entry)) {
            logQueue.poll(); // 移除最旧的
            logQueue.offer(entry);
        }
        
        // 添加到缓存
        synchronized (recentLogs) {
            recentLogs.add(0, entry);
            if (recentLogs.size() > MAX_RECENT_LOGS) {
                recentLogs.remove(recentLogs.size() - 1);
            }
        }
    }
    
    // Helper methods
    
    private boolean matchesLevel(String logLevel, String filterLevel) {
        int logPriority = getLevelPriority(logLevel);
        int filterPriority = getLevelPriority(filterLevel);
        return logPriority >= filterPriority;
    }
    
    private int getLevelPriority(String level) {
        return switch (level.toUpperCase()) {
            case "TRACE" -> 0;
            case "DEBUG" -> 1;
            case "INFO" -> 2;
            case "WARN" -> 3;
            case "ERROR" -> 4;
            default -> 2;
        };
    }
    
    private String escapeCsv(String value) {
        if (value == null) return "";
        return value.replace("\"", "\"\"").replace("\n", " ").replace("\r", "");
    }
    
    // Inner class
    
    private static class LogEntry {
        private final Instant timestamp;
        private final String level;
        private final String logger;
        private final String message;
        
        public LogEntry(Instant timestamp, String level, String logger, String message) {
            this.timestamp = timestamp;
            this.level = level;
            this.logger = logger;
            this.message = message;
        }
        
        public Instant getTimestamp() { return timestamp; }
        public String getLevel() { return level; }
        public String getLogger() { return logger; }
        public String getMessage() { return message; }
    }
}
