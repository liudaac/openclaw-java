package openclaw.desktop.model;

import java.time.Instant;

/**
 * Node Information Model.
 */
public record NodeInfo(
    String id,
    String name,
    String status,
    String version,
    Instant lastSeen
) {
    /**
     * Check if node is active.
     */
    public boolean isActive() {
        return "active".equals(status);
    }

    /**
     * Check if node is idle.
     */
    public boolean isIdle() {
        return "idle".equals(status);
    }

    /**
     * Check if node is offline.
     */
    public boolean isOffline() {
        return "offline".equals(status);
    }
}
