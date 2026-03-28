package openclaw.gateway.config;

import java.util.Map;
import java.util.Set;

/**
 * Protected configuration paths that cannot be modified through gateway tools.
 *
 * <p>This class defines configuration paths that are protected from modification
 * to prevent security risks. Any attempt to modify these paths through
 * config.apply or config.patch operations will be rejected.</p>
 *
 * @author OpenClaw Team
 * @version 2026.3.27
 * @since 2026.3.27
 */
public class ProtectedConfigPaths {

    /**
     * Set of protected configuration paths.
     */
    public static final Set<String> PROTECTED_PATHS = Set.of(
        "tools.exec.ask",
        "tools.exec.security"
    );

    /**
     * Checks if a path is protected.
     *
     * @param path the configuration path
     * @return true if protected
     */
    public static boolean isProtected(String path) {
        return PROTECTED_PATHS.contains(path);
    }

    /**
     * Gets the value at a nested path in a configuration map.
     *
     * @param config the configuration map
     * @param path the dot-separated path
     * @return the value, or null if not found
     */
    @SuppressWarnings("unchecked")
    public static Object getValueAtPath(Map<String, Object> config, String path) {
        if (config == null || path == null) {
            return null;
        }

        String[] parts = path.split("\\.");
        Object current = config;

        for (String part : parts) {
            if (!(current instanceof Map)) {
                return null;
            }
            current = ((Map<String, Object>) current).get(part);
            if (current == null) {
                return null;
            }
        }

        return current;
    }

    /**
     * Checks if any protected path would be modified.
     *
     * @param currentConfig the current configuration
     * @param newConfig the new configuration
     * @return set of protected paths that would be modified
     */
    public static Set<String> getModifiedProtectedPaths(
            Map<String, Object> currentConfig,
            Map<String, Object> newConfig) {

        Set<String> modified = new java.util.HashSet<>();

        for (String protectedPath : PROTECTED_PATHS) {
            Object currentValue = getValueAtPath(currentConfig, protectedPath);
            Object newValue = getValueAtPath(newConfig, protectedPath);

            if (!java.util.Objects.equals(currentValue, newValue)) {
                modified.add(protectedPath);
            }
        }

        return modified;
    }

    /**
     * Asserts that a configuration mutation is allowed.
     *
     * @param action the action being performed (config.apply or config.patch)
     * @param currentConfig the current configuration
     * @param raw the raw configuration mutation
     * @throws ProtectedConfigException if mutation is not allowed
     */
    public static void assertMutationAllowed(
            String action,
            Map<String, Object> currentConfig,
            Map<String, Object> raw) throws ProtectedConfigException {

        Set<String> modifiedPaths = getModifiedProtectedPaths(currentConfig, raw);

        if (!modifiedPaths.isEmpty()) {
            throw new ProtectedConfigException(
                "gateway " + action + " cannot change protected config paths: " +
                String.join(", ", modifiedPaths)
            );
        }
    }

    /**
     * Exception thrown when attempting to modify protected configuration.
     */
    public static class ProtectedConfigException extends Exception {
        public ProtectedConfigException(String message) {
            super(message);
        }
    }
}
