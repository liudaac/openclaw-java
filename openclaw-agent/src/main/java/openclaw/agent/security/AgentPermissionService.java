package openclaw.agent.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Service for managing agent permissions and access control.
 *
 * <p>Provides security checks for agent operations, including:
 * <ul>
 *   <li>Gateway configuration write protection</li>
 *   <li>Protected path validation</li>
 *   <li>Agent action authorization</li>
 * </ul>
 * </p>
 *
 * @author OpenClaw Team
 * @version 2026.3.28
 * @since 2026.3.28
 */
@Service
public class AgentPermissionService {

    private static final Logger logger = LoggerFactory.getLogger(AgentPermissionService.class);

    /**
     * Protected gateway configuration paths that agents cannot modify.
     */
    public static final Set<String> PROTECTED_GATEWAY_CONFIG_PATHS = Set.of(
        "tools.exec.ask",
        "tools.exec.security"
    );

    /**
     * Protected agent configuration paths.
     */
    public static final Set<String> PROTECTED_AGENT_CONFIG_PATHS = Set.of(
        "permissions",
        "security"
    );

    /**
     * Checks if a gateway config mutation is allowed.
     *
     * @param action the action (config.apply or config.patch)
     * @param currentConfig the current configuration
     * @param mutation the mutation being applied
     * @throws ConfigMutationException if mutation is not allowed
     */
    public void assertGatewayConfigMutationAllowed(
            String action,
            Map<String, Object> currentConfig,
            Map<String, Object> mutation) throws ConfigMutationException {

        // Get the effective new config
        Map<String, Object> newConfig;
        if ("config.apply".equals(action)) {
            newConfig = mutation;
        } else if ("config.patch".equals(action)) {
            newConfig = applyPatch(currentConfig, mutation);
        } else {
            throw new ConfigMutationException("Unknown action: " + action);
        }

        // Check protected paths
        List<String> changedProtectedPaths = PROTECTED_GATEWAY_CONFIG_PATHS.stream()
            .filter(path -> isPathChanged(currentConfig, newConfig, path))
            .toList();

        if (!changedProtectedPaths.isEmpty()) {
            String errorMsg = String.format(
                "gateway %s cannot change protected config paths: %s",
                action,
                String.join(", ", changedProtectedPaths)
            );
            logger.warn("Blocked agent config mutation: {}", errorMsg);
            throw new ConfigMutationException(errorMsg);
        }
    }

    /**
     * Checks if an agent config mutation is allowed.
     *
     * @param agentId the agent ID
     * @param currentConfig the current agent configuration
     * @param mutation the mutation being applied
     * @throws ConfigMutationException if mutation is not allowed
     */
    public void assertAgentConfigMutationAllowed(
            String agentId,
            Map<String, Object> currentConfig,
            Map<String, Object> mutation) throws ConfigMutationException {

        // Check protected paths at top level
        for (String protectedPath : PROTECTED_AGENT_CONFIG_PATHS) {
            if (mutation.containsKey(protectedPath)) {
                String errorMsg = String.format(
                    "Agent %s cannot modify protected config path: %s",
                    agentId,
                    protectedPath
                );
                logger.warn("Blocked agent config mutation: {}", errorMsg);
                throw new ConfigMutationException(errorMsg);
            }
        }
    }

    /**
     * Checks if an agent is allowed to execute a tool.
     *
     * @param agentId the agent ID
     * @param toolName the tool name
     * @param params the tool parameters
     * @return true if allowed
     */
    public boolean isToolExecutionAllowed(String agentId, String toolName, Map<String, Object> params) {
        // Check for protected tools
        if ("gateway".equals(toolName)) {
            String action = (String) params.get("action");
            if ("config.apply".equals(action) || "config.patch".equals(action)) {
                // Additional checks for config changes
                String raw = (String) params.get("raw");
                if (raw != null && containsProtectedPath(raw)) {
                    logger.warn("Agent {} attempted to modify protected config via gateway tool", agentId);
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Applies a patch to a configuration.
     *
     * @param current the current configuration
     * @param patch the patch to apply
     * @return the patched configuration
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> applyPatch(
            Map<String, Object> current,
            Map<String, Object> patch) {

        Map<String, Object> result = new java.util.HashMap<>(current);

        for (Map.Entry<String, Object> entry : patch.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (value instanceof Map && result.get(key) instanceof Map) {
                // Recursive merge for nested objects
                Map<String, Object> currentNested = (Map<String, Object>) result.get(key);
                Map<String, Object> patchNested = (Map<String, Object>) value;
                result.put(key, applyPatch(currentNested, patchNested));
            } else {
                // Simple replace
                result.put(key, value);
            }
        }

        return result;
    }

    /**
     * Checks if a path has changed between two configurations.
     *
     * @param current the current configuration
     * @param next the new configuration
     * @param path the dot-separated path
     * @return true if changed
     */
    private boolean isPathChanged(
            Map<String, Object> current,
            Map<String, Object> next,
            String path) {

        Object currentValue = getValueAtPath(current, path);
        Object nextValue = getValueAtPath(next, path);

        return !java.util.Objects.equals(currentValue, nextValue);
    }

    /**
     * Gets the value at a path in a configuration.
     *
     * @param config the configuration
     * @param path the dot-separated path
     * @return the value, or null if not found
     */
    @SuppressWarnings("unchecked")
    private Object getValueAtPath(Map<String, Object> config, String path) {
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
     * Checks if a raw config string contains protected paths.
     *
     * @param raw the raw config string
     * @return true if contains protected paths
     */
    private boolean containsProtectedPath(String raw) {
        if (raw == null) {
            return false;
        }

        for (String protectedPath : PROTECTED_GATEWAY_CONFIG_PATHS) {
            if (raw.contains(protectedPath)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Exception thrown when a config mutation is not allowed.
     */
    public static class ConfigMutationException extends