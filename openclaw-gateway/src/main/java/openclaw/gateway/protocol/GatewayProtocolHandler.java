package openclaw.gateway.protocol;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gateway protocol handler for message framing and routing.
 *
 * @author OpenClaw Team
 * @version 2026.3.13
 */
public class GatewayProtocolHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GatewayProtocolHandler.class);
    
    private final ObjectMapper objectMapper;
    private final Map<String, PendingRequest> pendingRequests;
    private final ProtocolVersion protocolVersion;
    
    public GatewayProtocolHandler() {
        this.objectMapper = new ObjectMapper();
        this.pendingRequests = new ConcurrentHashMap<>();
        this.protocolVersion = ProtocolVersion.V3;
    }
    
    /**
     * Encode a request frame.
     */
    public String encodeRequest(String id, String method, Object params) {
        try {
            ObjectNode frame = objectMapper.createObjectNode();
            frame.put("type", "req");
            frame.put("id", id);
            frame.put("method", method);
            frame.put("version", protocolVersion.getVersion());
            frame.set("params", objectMapper.valueToTree(params));
            return objectMapper.writeValueAsString(frame);
        } catch (Exception e) {
            throw new RuntimeException("Failed to encode request", e);
        }
    }
    
    /**
     * Encode a response frame.
     */
    public String encodeResponse(String id, Object result, Object error) {
        try {
            ObjectNode frame = objectMapper.createObjectNode();
            frame.put("type", "resp");
            frame.put("id", id);
            frame.put("version", protocolVersion.getVersion());
            
            if (error != null) {
                frame.set("error", objectMapper.valueToTree(error));
            } else {
                frame.set("result", objectMapper.valueToTree(result));
            }
            
            return objectMapper.writeValueAsString(frame);
        } catch (Exception e) {
            throw new RuntimeException("Failed to encode response", e);
        }
    }
    
    /**
     * Encode an event frame.
     */
    public String encodeEvent(String event, Object data) {
        try {
            ObjectNode frame = objectMapper.createObjectNode();
            frame.put("type", "event");
            frame.put("event", event);
            frame.put("version", protocolVersion.getVersion());
            frame.set("data", objectMapper.valueToTree(data));
            return objectMapper.writeValueAsString(frame);
        } catch (Exception e) {
            throw new RuntimeException("Failed to encode event", e);
        }
    }
    
    /**
     * Decode a frame.
     */
    public GatewayFrame decodeFrame(String json) throws IOException {
        JsonNode node = objectMapper.readTree(json);
        
        String type = node.get("type").asText();
        String id = node.has("id") ? node.get("id").asText() : null;
        int version = node.has("version") ? node.get("version").asInt() : 1;
        
        return new GatewayFrame(type, id, version, node);
    }
    
    /**
     * Register a pending request.
     */
    public void registerRequest(String id, CompletableFuture<Object> future) {
        pendingRequests.put(id, new PendingRequest(id, future, System.currentTimeMillis()));
    }
    
    /**
     * Complete a pending request.
     */
    public boolean completeRequest(String id, Object result) {
        PendingRequest request = pendingRequests.remove(id);
        if (request != null) {
            request.future().complete(result);
            return true;
        }
        return false;
    }
    
    /**
     * Complete a pending request with error.
     */
    public boolean completeRequestWithError(String id, Throwable error) {
        PendingRequest request = pendingRequests.remove(id);
        if (request != null) {
            request.future().completeExceptionally(error);
            return true;
        }
        return false;
    }
    
    /**
     * Clean up expired pending requests.
     */
    public int cleanupExpiredRequests(long timeoutMs) {
        long now = System.currentTimeMillis();
        var expired = pendingRequests.entrySet().stream()
            .filter(e -> now - e.getValue().timestamp() > timeoutMs)
            .map(Map.Entry::getKey)
            .toList();
        
        expired.forEach(id -> {
            PendingRequest request = pendingRequests.remove(id);
            if (request != null) {
                request.future().completeExceptionally(
                    new RuntimeException("Request timeout")
                );
            }
        });
        
        return expired.size();
    }
    
    /**
     * Get protocol version.
     */
    public ProtocolVersion getProtocolVersion() {
        return protocolVersion;
    }
    
    /**
     * Check if version is compatible.
     */
    public boolean isVersionCompatible(int clientVersion) {
        return clientVersion <= protocolVersion.getVersion();
    }
    
    // Records
    
    public record GatewayFrame(
        String type,
        String id,
        int version,
        JsonNode data
    ) {}
    
    private record PendingRequest(
        String id,
        CompletableFuture<Object> future,
        long timestamp
    ) {}
    
    /**
     * Protocol version.
     */
    public enum ProtocolVersion {
        V1(1),
        V2(2),
        V3(3);
        
        private final int version;
        
        ProtocolVersion(int version) {
            this.version = version;
        }
        
        public int getVersion() {
            return version;
        }
    }
}
