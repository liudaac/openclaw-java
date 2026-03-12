package openclaw.sdk.core;

import java.util.Map;
import java.util.Optional;

/**
 * OpenClaw configuration interface.
 *
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public interface OpenClawConfig {

    /**
     * Gets a configuration value by path.
     *
     * @param path the dot-separated path (e.g., "gateway.port")
     * @return the value if present
     */
    Optional<Object> get(String path);

    /**
     * Gets a string configuration value.
     *
     * @param path the path
     * @return the string value if present
     */
    default Optional<String> getString(String path) {
        return get(path).map(Object::toString);
    }

    /**
     * Gets an integer configuration value.
     *
     * @param path the path
     * @return the integer value if present
     */
    default Optional<Integer> getInt(String path) {
        return get(path)
                .filter(v -> v instanceof Number)
                .map(v -> ((Number) v).intValue());
    }

    /**
     * Gets a boolean configuration value.
     *
     * @param path the path
     * @return the boolean value if present
     */
    default Optional<Boolean> getBoolean(String path) {
        return get(path)
                .filter(v -> v instanceof Boolean)
                .map(v -> (Boolean) v);
    }

    /**
     * Gets a nested configuration object.
     *
     * @param path the path
     * @return the nested config if present
     */
    Optional<Map<String, Object>> getObject(String path);

    /**
     * Checks if a path exists in the configuration.
     *
     * @param path the path
     * @return true if exists
     */
    boolean has(String path);

    /**
     * Gets the raw configuration map.
     *
     * @return the raw config
     */
    Map<String, Object> toMap();
}
