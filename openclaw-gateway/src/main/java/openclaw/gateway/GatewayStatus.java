package openclaw.gateway;

import java.time.Instant;
import java.util.List;

/**
 * Gateway status information.
 *
 * @author OpenClaw Team
 * @version 2026.3.30
 */
public record GatewayStatus(
        boolean connected,
        String url,
        String version,
        Instant lastConnectedAt,
        int reconnectAttempts,
        List<NodeInfo> nodes
) {
    public GatewayStatus {
        nodes = nodes != null ? List.copyOf(nodes) : List.of();
    }

    public GatewayStatus(boolean connected, String url, String version) {
        this(connected, url, version, null, 0, List.of());
    }

    public GatewayStatus(boolean connected, String url, String version, Instant lastConnectedAt) {
        this(connected, url, version, lastConnectedAt, 0, List.of());
    }

    public boolean isConnected() {
        return connected;
    }

    public String getUrl() {
        return url;
    }

    public String getVersion() {
        return version;
    }

    public Instant getLastConnectedAt() {
        return lastConnectedAt;
    }

    public int getReconnectAttempts() {
        return reconnectAttempts;
    }

    public List<NodeInfo> getNodes() {
        return nodes;
    }
}
