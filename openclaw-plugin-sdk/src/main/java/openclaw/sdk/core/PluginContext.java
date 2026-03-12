package openclaw.sdk.core;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

/**
 * Context provided to plugins during initialization.
 *
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public interface PluginContext {

    /**
     * Gets the plugin ID.
     *
     * @return the plugin ID
     */
    String getPluginId();

    /**
     * Gets the plugin configuration.
     *
     * @return the configuration
     */
    Map<String, Object> getConfig();

    /**
     * Gets the workspace directory.
     *
     * @return the workspace path
     */
    Path getWorkspaceDir();

    /**
     * Gets the plugin's data directory.
     *
     * @return the data directory path
     */
    Path getDataDir();

    /**
     * Gets an environment variable.
     *
     * @param name the variable name
     * @return the value if present
     */
    Optional<String> getEnv(String name);

    /**
     * Gets the agent ID if running in an agent context.
     *
     * @return the agent ID if present
     */
    Optional<String> getAgentId();

    /**
     * Gets the session key if in a session context.
     *
     * @return the session key if present
     */
    Optional<String> getSessionKey();
}
