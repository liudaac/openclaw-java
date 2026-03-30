package openclaw.gateway;

/**
 * Gateway node information.
 *
 * @author OpenClaw Team
 * @version 2026.3.30
 */
public record NodeInfo(
        String name,
        String status,
        String version
) {
}
