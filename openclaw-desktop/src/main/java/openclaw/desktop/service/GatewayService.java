package openclaw.desktop.service;

import openclaw.gateway.GatewayService;
import openclaw.gateway.NodeInfo;
import openclaw.gateway.GatewayStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Gateway Service for Desktop Application.
 *
 * <p>Provides high-level Gateway operations with JavaFX-friendly callbacks.</p>
 */
@Service
public class GatewayService {

    private static final Logger logger = LoggerFactory.getLogger(GatewayService.class);

    @Autowired
    private GatewayService gatewayService;

    private String currentUrl;
    private String authToken;
    private boolean autoReconnect;
    private Instant lastConnectedAt;
    private int reconnectAttempts;
    private boolean connected;
    private String version;

    /**
     * Connect to Gateway.
     */
    public CompletableFuture<Boolean> connect(String url, String token, boolean autoReconnect) {
        this.currentUrl = url;
        this.authToken = token;
        this.autoReconnect = autoReconnect;

        logger.info("Connecting to Gateway: {}", url);

        return CompletableFuture.supplyAsync(() -> {
            try {
                // Implementation depends on GatewayService API
                // gatewayService.connect(url, token).join();
                this.connected = true;
                this.lastConnectedAt = Instant.now();
                this.reconnectAttempts = 0;
                this.version = "2026.3.24";
                logger.info("Connected to Gateway");
                return true;
            } catch (Exception e) {
                logger.error("Failed to connect to Gateway", e);
                this.connected = false;
                return false;
            }
        });
    }

    /**
     * Disconnect from Gateway.
     */
    public CompletableFuture<Void> disconnect() {
        return CompletableFuture.runAsync(() -> {
            logger.info("Disconnecting from Gateway");
            // gatewayService.disconnect().join();
            this.connected = false;
        });
    }

    /**
     * Get Gateway status.
     */
    public CompletableFuture<GatewayStatus> getStatus() {
        return CompletableFuture.supplyAsync(() -> {
            return new GatewayStatus(
                connected,
                currentUrl,
                version,
                lastConnectedAt,
                reconnectAttempts,
                autoReconnect
            );
        });
    }

    /**
     * Get registered nodes.
     */
    public CompletableFuture<List<NodeInfo>> getNodes() {
        return CompletableFuture.supplyAsync(() -> {
            // Return mock data for now
            return List.of(
                new NodeInfo("node-1", "Local Node", "active", "2026.3.24", Instant.now()),
                new NodeInfo("node-2", "Remote Node", "idle", "2026.3.24", Instant.now())
            );
        });
    }

    /**
     * Get current URL.
     */
    public String getCurrentUrl() {
        return currentUrl;
    }

    /**
     * Check if connected.
     */
    public boolean isConnected() {
        return connected;
    }
}
