package openclaw.desktop.model;

import java.util.List;

/**
 * Agent Information Model.
 */
public record AgentInfo(
    String id,
    String name,
    String description,
    List<String> skills,
    String status
) {
    public boolean isActive() {
        return "active".equals(status);
    }

    public boolean isPaused() {
        return "paused".equals(status);
    }
}
