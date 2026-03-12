package openclaw.sdk.core;

import java.util.Map;
import java.util.Optional;

/**
 * Runtime environment information.
 *
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public interface RuntimeEnv {

    /**
     * Gets the Node.js version (for compatibility).
     *
     * @return the Node version
     */
    String getNodeVersion();

    /**
     * Gets the OpenClaw version.
     *
     * @return the version
     */
    String getOpenClawVersion();

    /**
     * Gets the platform (e.g., "linux", "darwin", "win32").
     *
     * @return the platform
     */
    String getPlatform();

    /**
     * Gets the architecture (e.g., "x64", "arm64").
     *
     * @return the architecture
     */
    String getArch();

    /**
     * Gets all environment variables.
     *
     * @return the environment map
     */
    Map<String, String> getEnv();

    /**
     * Gets a specific environment variable.
     *
     * @param name the variable name
     * @return the value if present
     */
    Optional<String> getEnv(String name);

    /**
     * Checks if running in CI environment.
     *
     * @return true if in CI
     */
    boolean isCI();

    /**
     * Checks if running on macOS.
     *
     * @return true if on macOS
     */
    default boolean isMacOS() {
        return "darwin".equalsIgnoreCase(getPlatform());
    }

    /**
     * Checks if running on Linux.
     *
     * @return true if on Linux
     */
    default boolean isLinux() {
        return "linux".equalsIgnoreCase(getPlatform());
    }

    /**
     * Checks if running on Windows.
     *
     * @return true if on Windows
     */
    default boolean isWindows() {
        return "win32".equalsIgnoreCase(getPlatform());
    }
}
