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
        String version,
        Instant connectedAt,
        List<NodeInfo> nodes
) {
    public GatewayStatus {
        nodes = nodes != null ? List.copyOf(nodes) : List.of();
    }

    public GatewayStatus(boolean connected, String version) {
        this(connected, version, null, List.of());
    }

    public GatewayStatus(boolean connected, String version, Instant connectedAt) {
        this(connected, version, connectedAt, List.of());
    }
}
