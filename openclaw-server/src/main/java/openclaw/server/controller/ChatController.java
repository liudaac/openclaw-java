package openclaw.server.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Chat API
 * 
 * 提供聊天历史、发送消息、中止运行等功能
 * 对应 Node.js Control UI 的 chat.* 方法
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {
    
    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);
    
    @Autowired
    private ObjectMapper objectMapper;
    
    // 模拟会话存储
    private final Map<String, ChatSession> sessions = new ConcurrentHashMap<>();
    
    // 流式响应 Sink
    private final Map<String, Sinks.Many<String>> streamingSinks = new ConcurrentHashMap<>();
    
    /**
     * 获取聊天历史
     * 
     * GET /api/chat/history?sessionKey={sessionKey}
     */
    @GetMapping("/history")
    public Mono<ResponseEntity<ObjectNode>> getChatHistory(
            @RequestParam String sessionKey,
            @RequestParam(defaultValue = "100") int limit) {
        
        return Mono.fromCallable(() -> {
            ChatSession session = sessions.get(sessionKey);
            
            if (session == null) {
                // 返回空历史
                ObjectNode result = objectMapper.createObjectNode();
                result.set("messages", objectMapper.createArrayNode());
                result.put("count", 0);
                return ResponseEntity.ok(result);
            }
            
            ObjectNode result = objectMapper.createObjectNode();
            ArrayNode messages = objectMapper.createArrayNode();
            
            // 获取最近的消息
            int start = Math.max(0, session.getMessages().size() - limit);
            for (int i = start; i < session.getMessages().size(); i++) {
                messages.add(session.getMessages().get(i));
            }
            
            result.set("messages", messages);
            result.put("count", messages.size());
            result.put("sessionKey", sessionKey);
            
            return ResponseEntity.ok(result);
            
        }).onErrorResume(e -> {
            logger.error("Failed to get chat history", e);
            return Mono.just(ResponseEntity.internalServerError().build());
        });
    }
    
    /**
     * 发送消息
     * 
     * POST /api/chat/send
     */
    @PostMapping("/send")
    public Mono<ResponseEntity<ObjectNode>> sendMessage(@RequestBody JsonNode request) {
        return Mono.fromCallable(() -> {
            String sessionKey = request.path("sessionKey").asText();
            String message = request.path("message").asText();
            String idempotencyKey = request.path("idempotencyKey").asText();
            
            if (sessionKey == null || sessionKey.isEmpty()) {
                sessionKey = "session-" + UUID.randomUUID().toString().substring(0, 8);
            }
            
            // 获取或创建会话
            ChatSession session = sessions.computeIfAbsent(sessionKey, k -> new ChatSession(k));
            
            // 检查幂等性
            if (idempotencyKey != null && !idempotencyKey.isEmpty()) {
                if (session.hasInFlightRequest(idempotencyKey)) {
                    ObjectNode result = objectMapper.createObjectNode();
                    result.put("runId", idempotencyKey);
                    result.put("status", "in_flight");
                    return ResponseEntity.ok(result);
                }
            }
            
            String runId = idempotencyKey != null ? idempotencyKey : UUID.randomUUID().toString();
            
            // 添加用户消息
            ObjectNode userMessage = objectMapper.createObjectNode();
            userMessage.put("id", UUID.randomUUID().toString());
            userMessage.put("role", "user");
            userMessage.put("content", message);
            userMessage.put("timestamp", Instant.now().toString());
            session.addMessage(userMessage);
            
            // 标记为运行中
            session.markInFlight(runId);
            
            // 异步处理回复
            processAssistantResponse(session, runId);
            
            ObjectNode result = objectMapper.createObjectNode();
            result.put("runId", runId);
            result.put("status", "started");
            result.put("sessionKey", sessionKey);
            
            return ResponseEntity.ok(result);
            
        }).onErrorResume(e -> {
            logger.error("Failed to send message", e);
            return Mono.just(ResponseEntity.internalServerError().build());
        });
    }
    
    /**
     * 中止运行
     * 
     * POST /api/chat/abort
     */
    @PostMapping("/abort")
    public Mono<ResponseEntity<ObjectNode>> abortRun(@RequestBody JsonNode request) {
        return Mono.fromCallable(() -> {
            String sessionKey = request.path("sessionKey").asText();
            String runId = request.path("runId").asText();
            
            ChatSession session = sessions.get(sessionKey);
            if (session == null) {
                return ResponseEntity.badRequest()
                    .body(createErrorResponse("Session not found"));
            }
            
            // 中止运行
            session.abortRun(runId);
            
            // 添加中止消息
            ObjectNode abortMessage = objectMapper.createObjectNode();
            abortMessage.put("id", UUID.randomUUID().toString());
            abortMessage.put("role", "system");
            abortMessage.put("content", "[Run aborted by user]");
            abortMessage.put("timestamp", Instant.now().toString());
            abortMessage.put("aborted", true);
            abortMessage.put("runId", runId);
            session.addMessage(abortMessage);
            
            ObjectNode result = objectMapper.createObjectNode();
            result.put("success", true);
            result.put("message", "Run aborted");
            
            return ResponseEntity.ok(result);
            
        }).onErrorResume(e -> {
            logger.error("Failed to abort run", e);
            return Mono.just(ResponseEntity.internalServerError().body((ObjectNode) null));
        });
    }
    
    /**
     * 注入消息 (仅 UI 更新，不触发 Agent 运行)
     * 
     * POST /api/chat/inject
     */
    @PostMapping("/inject")
    public Mono<ResponseEntity<ObjectNode>> injectMessage(@RequestBody JsonNode request) {
        return Mono.fromCallable(() -> {
            String sessionKey = request.path("sessionKey").asText();
            String content = request.path("content").asText();
            String role = request.path("role").asText("assistant");
            
            ChatSession session = sessions.computeIfAbsent(sessionKey, k -> new ChatSession(k));
            
            ObjectNode message = objectMapper.createObjectNode();
            message.put("id", UUID.randomUUID().toString());
            message.put("role", role);
            message.put("content", content);
            message.put("timestamp", Instant.now().toString());
            message.put("injected", true);
            
            session.addMessage(message);
            
            ObjectNode result = objectMapper.createObjectNode();
            result.put("success", true);
            result.put("messageId", message.path("id").asText());
            
            return ResponseEntity.ok(result);
            
        }).onErrorResume(e -> {
            logger.error("Failed to inject message", e);
            return Mono.just(ResponseEntity.internalServerError().build());
        });
    }
    
    /**
     * 流式响应
     * 
     * GET /api/chat/stream?sessionKey={sessionKey}&runId={runId}
     */
    @GetMapping(value = "/stream", produces = "text/event-stream")
    public Flux<String> streamResponse(
            @RequestParam String sessionKey,
            @RequestParam String runId) {
        
        Sinks.Many<String> sink = Sinks.many().multicast().onBackpressureBuffer();
        streamingSinks.put(runId, sink);
        
        return sink.asFlux()
            .doOnCancel(() -> streamingSinks.remove(runId))
            .doOnComplete(() -> streamingSinks.remove(runId));
    }
    
    // Helper methods
    
    private void processAssistantResponse(ChatSession session, String runId) {
        new Thread(() -> {
            try {
                // 模拟流式响应
                String[] chunks = {
                    "Hello! ",
                    "I'm ",
                    "processing ",
                    "your ",
                    "message. ",
                    "Here's ",
                    "a ",
                    "response ",
                    "from ",
                    "the ",
                    "assistant."
                };
                
                StringBuilder fullResponse = new StringBuilder();
                
                for (String chunk : chunks) {
                    if (session.isAborted(runId)) {
                        break;
                    }
                    
                    fullResponse.append(chunk);
                    
                    // 发送流式数据
                    Sinks.Many<String> sink = streamingSinks.get(runId);
                    if (sink != null) {
                        ObjectNode event = objectMapper.createObjectNode();
                        event.put("type", "chunk");
                        event.put("content", chunk);
                        event.put("runId", runId);
                        sink.tryEmitNext("data: " + event.toString() + "\n\n");
                    }
                    
                    Thread.sleep(100);
                }
                
                // 添加完整回复到历史
                if (!session.isAborted(runId)) {
                    ObjectNode assistantMessage = objectMapper.createObjectNode();
                    assistantMessage.put("id", UUID.randomUUID().toString());
                    assistantMessage.put("role", "assistant");
                    assistantMessage.put("content", fullResponse.toString());
                    assistantMessage.put("timestamp", Instant.now().toString());
                    assistantMessage.put("runId", runId);
                    session.addMessage(assistantMessage);
                }
                
                // 完成流
                Sinks.Many<String> sink = streamingSinks.get(runId);
                if (sink != null) {
                    ObjectNode event = objectMapper.createObjectNode();
                    event.put("type", "done");
                    event.put("runId", runId);
                    sink.tryEmitNext("data: " + event.toString() + "\n\n");
                    sink.tryEmitComplete();
                }
                
                session.markCompleted(runId);
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }
    
    private JsonNode createErrorResponse(String error) {
        ObjectNode result = objectMapper.createObjectNode();
        result.put("success", false);
        result.put("error", error);
        return result;
    }
    
    // Inner class
    
    private static class ChatSession {
        private final String sessionKey;
        private final ArrayNode messages;
        private final Map<String, Boolean> inFlightRequests;
        private final Map<String, Boolean> abortedRuns;
        
        public ChatSession(String sessionKey) {
            this.sessionKey = sessionKey;
            this.messages = new ObjectMapper().createArrayNode();
            this.inFlightRequests = new ConcurrentHashMap<>();
            this.abortedRuns = new ConcurrentHashMap<>();
        }
        
        public String getSessionKey() {
            return sessionKey;
        }
        
        public ArrayNode getMessages() {
            return messages;
        }
        
        public void addMessage(JsonNode message) {
            messages.add(message);
        }
        
        public void markInFlight(String runId) {
            inFlightRequests.put(runId, true);
        }
        
        public void markCompleted(String runId) {
            inFlightRequests.remove(runId);
        }
        
        public boolean hasInFlightRequest(String runId) {
            return inFlightRequests.containsKey(runId);
        }
        
        public void abortRun(String runId) {
            abortedRuns.put(runId, true);
            inFlightRequests.remove(runId);
        }
        
        public boolean isAborted(String runId) {
            return abortedRuns.containsKey(runId);
        }
    }
}
