package openclaw.agent.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Configuration for tool policies, including sub-agent restrictions.
 *
 * <p>Defines which tools are allowed or denied for different session types.</p>
 *
 * @author OpenClaw Team
 * @version 2026.3.30
 * @since 2026.3.30
 */
@Component
@ConfigurationProperties(prefix = "openclaw.agent.tool-policy")
public class ToolPolicyConfiguration {

    /**
     * Tools allowed for sub-agent sessions (read-only).
     */
    private Set<String> subagentAllowedTools = Set.of(
            "memory_search",
            "memory_get",
            "read",
            "web_search",
            "web_fetch"
    );

    /**
     * Tools denied for sub-agent sessions.
     */
    private Set<String> subagentDeniedTools = Set.of(
            "write",
            "edit",
            "memory_create",
            "memory_update",
            "memory_delete"
    );

    /**
     * Tools denied for all sessions (global denylist).
     */
    private Set<String> deniedTools = Set.of();

    /**
     * Gets the set of tools allowed for sub-agent sessions.
     *
     * @return set of allowed tool names
     */
    public Set<String> getSubagentAllowedTools() {
        return subagentAllowedTools;
    }

    /**
     * Sets the tools allowed for sub-agent sessions.
     *
     * @param subagentAllowedTools the allowed tools
     */
    public void setSubagentAllowedTools(Set<String> subagentAllowedTools) {
        this.subagentAllowedTools = subagentAllowedTools;
    }

    /**
     * Gets the set of tools denied for sub-agent sessions.
     *
     * @return set of denied tool names
     */
    public Set<String> getSubagentDeniedTools() {
        return subagentDeniedTools;
    }

    /**
     * Sets the tools denied for sub-agent sessions.
     *
     * @param subagentDeniedTools the denied tools
     */
    public void setSubagentDeniedTools(Set<String> subagentDeniedTools) {
        this.subagentDeniedTools = subagentDeniedTools;
    }

    /**
     * Gets the global set of denied tools.
     *
     * @return set of globally denied tool names
     */
    public Set<String> getDeniedTools() {
        return deniedTools;
    }

    /**
     * Sets the global denied tools.
     *
     * @param deniedTools the globally denied tools
     */
    public void setDeniedTools(Set<String> deniedTools) {
        this.deniedTools = deniedTools;
    }

    /**
     * Checks if a tool is in the sub-agent allowed list.
     *
     * @param toolName the tool name
     * @return true if allowed for sub-agents
     */
    public boolean isSubagentAllowed(String toolName) {
        return subagentAllowedTools.contains(toolName);
    }

    /**
     * Checks if a tool is in the sub-agent denied list.
     *
     * @param toolName the tool name
     * @return true if denied for sub-agents
     */
    public boolean isSubagentDenied(String toolName) {
        return subagentDeniedTools.contains(toolName);
    }

    /**
     * Checks if a tool is globally denied.
     *
     * @param toolName the tool name
     * @return true if globally denied
     */
    public boolean isGloballyDenied(String toolName) {
        return deniedTools.contains(toolName);
    }
}
