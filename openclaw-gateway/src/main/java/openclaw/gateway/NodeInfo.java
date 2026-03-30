package openclaw.gateway;

import java.time.Instant;

/**
 * Gateway node information.
 *
 * @author OpenClaw Team
 * @version 2026.3.30
 */
public record NodeInfo(
        String id,
        String name,
        String status,
        String version,
        Instant lastSeen
) {
    public NodeInfo(String id, String name, String status) {
        this(id, name, status, null, null);
    }

    public NodeInfo(String id, String name, String status, String version) {
        this(id, name, status, version, null);
    }
}
