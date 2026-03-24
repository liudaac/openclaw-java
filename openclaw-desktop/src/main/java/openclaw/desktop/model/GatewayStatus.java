package openclaw.desktop.model;

import java.time.Instant;

/**
 * Gateway Status Model.
 */
public record GatewayStatus(
    boolean connected,
    String url,
    String version,
    Instant lastConnectedAt,
    int reconnectAttempts,
    boolean autoReconnect
) {
    /**
     * Check if connected.
     */
    public boolean isConnected() {
        return connected;
    }

    /**
     * Get URL.
     */
    public String getUrl() {
        return url;
    }

    /**
     * Get version.
     */
    public String getVersion() {
        return version;
    }

    /**
     * Get last connected time.
     */
    public Instant getLastConnectedAt() {
        return lastConnectedAt;
    }

    /**
     * Get reconnect attempts.
     */
    public int getReconnectAttempts() {
        return reconnectAttempts;
    }

    /**
     * Check if auto reconnect enabled.
     */
    public boolean isAutoReconnect() {
        return autoReconnect;
    }
}
