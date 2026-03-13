package openclaw.gateway.client;

import openclaw.gateway.auth.DeviceAuthV3;
import openclaw.gateway.protocol.GatewayProtocolHandler;
import openclaw.gateway.reconnect.ExponentialBackoff;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Gateway WebSocket client with auto-reconnect.
 *
 * @author OpenClaw Team
 * @version 2026.3.13
 */
public class GatewayClient {
    
    private static final Logger logger = LoggerFactory.getLogger(GatewayClient.class);
    
    private final URI serverUri;
    private final DeviceAuthV3.AuthRequest authRequest;
    private final GatewayProtocolHandler protocolHandler;
    private final ExponentialBackoff reconnectBackoff;
    private final Map<String, CompletableFuture<Object>> pendingRequests;
    
    private final ReactorNettyWebSocketClient wsClient;
    private final AtomicReference<WebSocketSession> sessionRef;
    private final AtomicBoolean connected;
    private final AtomicBoolean shouldReconnect;
    
    public GatewayClient(String serverUrl, DeviceAuthV3.AuthRequest authRequest) {
        this.serverUri = URI.create(serverUrl);
        this.authRequest = authRequest;
        this.protocolHandler = new GatewayProtocolHandler();
        this.reconnectBackoff = new ExponentialBackoff(1000, 30000, 2.0, 0.1);
        this.pendingRequests = new ConcurrentHashMap<>();
        
        this.wsClient = new ReactorNettyWebSocketClient();
        this.sessionRef = new AtomicReference<>();
        this.connected = new AtomicBoolean(false);
        this.shouldReconnect = new AtomicBoolean(true);
    }
    
    /**
     * Start the client with auto-reconnect.
     */
    public CompletableFuture<Void> start() {
        return CompletableFuture.runAsync(() -> {
            shouldReconnect.set(true);
            connect();
        });
    }
    
    /**
     * Stop the client.
     */
    public CompletableFuture<Void> stop() {
        return CompletableFuture.runAsync(() -> {
            shouldReconnect.set(false);
            disconnect();
        });
    }
    
    /**
     * Send a request and wait for response.
     */
    public CompletableFuture<Object> request(String method, Object params) {
        String id = UUID.randomUUID().toString();
        String frame = protocolHandler.encodeRequest(id, method, params);
        
        CompletableFuture<Object> future = new CompletableFuture<>();
        pendingRequests.put(id, future);
        protocolHandler.registerRequest(id, future);
        
        WebSocketSession session = sessionRef.get();
        if (session != null && connected.get()) {
            session.send(Mono.just(session.textMessage(frame)))
                .doOnError(e -> {
                    pendingRequests.remove(id);
                    future.completeExceptionally(e);
                })
                .subscribe();
        } else {
            pendingRequests.remove(id);
            future.completeExceptionally(new IllegalStateException("Not connected"));
        }
        
        return future;
    }
    
    /**
     * Send a notification (no response expected).
     */
    public void notify(String method, Object params) {
        String id = UUID.randomUUID().toString();
        String frame = protocolHandler.encodeRequest(id, method, params);
        
        WebSocketSession session = sessionRef.get();
        if (session != null && connected.get()) {
            session.send(Mono.just(session.textMessage(frame))).subscribe();
        }
    }
    
    /**
     * Check if connected.
     */
    public boolean isConnected() {
        return connected.get();
    }
    
    // Private methods
    
    private void connect() {
        if (!shouldReconnect.get()) {
            return;
        }
        
        logger.info("Connecting to gateway: {}", serverUri);
        
        wsClient.execute(serverUri, session -> {
            sessionRef.set(session);
            connected.set(true);
            reconnectBackoff.reset();
            
            logger.info("Connected to gateway");
            
            // Send authentication
            String authFrame = protocolHandler.encodeRequest(
                UUID.randomUUID().toString(),
                "auth",
                authRequest
            );
            
            return session.send(Mono.just(session.textMessage(authFrame)))
                .thenMany(session.receive()
                    .map(msg -> msg.getPayloadAsText())
                    .flatMap(this::handleMessage)
                )
                .then();
        }).doOnError(e -> {
            logger.error("Connection error", e);
            handleDisconnect();
        }).subscribe();
    }
    
    private void disconnect() {
        WebSocketSession session = sessionRef.getAndSet(null);
        if (session != null) {
            connected.set(false);
            session.close().subscribe();
            logger.info("Disconnected from gateway");
        }
    }
    
    private void handleDisconnect() {
        connected.set(false);
        sessionRef.set(null);
        
        if (shouldReconnect.get()) {
            Duration delay = reconnectBackoff.nextDelay();
            logger.info("Reconnecting in {}ms", delay.toMillis());
            
            try {
                Thread.sleep(delay.toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            connect();
        }
    }
    
    private Mono<Void> handleMessage(String payload) {
        try {
            GatewayProtocolHandler.GatewayFrame frame = protocolHandler.decodeFrame(payload);
            
            switch (frame.type()) {
                case "resp":
                    handleResponse(frame);
                    break;
                case "event":
                    handleEvent(frame);
                    break;
                default:
                    logger.debug("Unknown frame type: {}", frame.type());
            }
        } catch (Exception e) {
            logger.error("Failed to handle message", e);
        }
        
        return Mono.empty();
    }
    
    private void handleResponse(GatewayProtocolHandler.GatewayFrame frame) {
        String id = frame.id();
        if (id == null) return;
        
        CompletableFuture<Object> future = pendingRequests.remove(id);
        if (future != null) {
            // Complete the future
            future.complete(frame.data());
        }
    }
    
    private void handleEvent(GatewayProtocolHandler.GatewayFrame frame) {
        // Handle events
        logger.debug("Received event: {}", frame.data());
    }
}
