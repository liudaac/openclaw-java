package openclaw.sdk.core;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Core runtime providing basic services to plugins.
 *
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public interface CoreRuntime {

    /**
     * Gets the plugin logger.
     *
     * @return the logger
     */
    PluginLogger logger();

    /**
     * Gets the runtime environment.
     *
     * @return the runtime environment
     */
    RuntimeEnv environment();

    /**
     * Resolves a path relative to the workspace.
     *
     * @param relativePath the relative path
     * @return the resolved path
     */
    Path resolveWorkspacePath(String relativePath);

    /**
     * Gets the temporary directory for this plugin.
     *
     * @return the temp directory path
     */
    Path getTempDirectory();

    /**
     * Gets the OpenClaw configuration.
     *
     * @return the configuration
     */
    CompletableFuture<OpenClawConfig> getConfig();

    /**
     * Gets the OpenClaw version.
     *
     * @return the version string
     */
    String getVersion();

    /**
     * Checks if running in development mode.
     *
     * @return true if in dev mode
     */
    boolean isDevMode();
}
