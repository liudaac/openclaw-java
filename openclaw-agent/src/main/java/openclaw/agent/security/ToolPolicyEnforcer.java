package openclaw.agent.security;

import openclaw.agent.session.SessionStateMachine.SessionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * Enforces tool policies for agent sessions.
 *
 * <p>Validates tool execution requests against configured policies,
 * with special handling for sub-agent sessions.</p>
 *
 * @author OpenClaw Team
 * @version 2026.3.30
 * @since 2026.3.30
 */
@Service
public class ToolPolicyEnforcer {

    private static final Logger logger = LoggerFactory.getLogger(ToolPolicyEnforcer.class);

    private final ToolPolicyConfiguration config;

    public ToolPolicyEnforcer(ToolPolicyConfiguration config) {
        this.config = config;
    }

    /**
     * Checks if a tool is allowed for execution in the given session context.
     *
     * <p>For sub-agent sessions, only tools in the allowed list can be used.
     * For main agent sessions, tools are allowed unless globally denied.</p>
     *
     * @param toolName the name of the tool to check
     * @param context the session context
     * @return true if the tool is allowed
     */
    public boolean isToolAllowed(String toolName, SessionContext context) {
        if (context == null) {
            logger.warn("Session context is null, denying tool execution: {}", toolName);
            return false;
        }

        // Check if this is a sub-agent session
        if (context.isSubagentSession()) {
            boolean allowed = config.getSubagentAllowedTools().contains(toolName);
            if (!allowed) {
                logger.warn("Tool '{}' denied for sub-agent session: {}", 
                    toolName, context.sessionKey());
            } else {
                logger.debug("Tool '{}' allowed for sub-agent session: {}", 
                    toolName, context.sessionKey());
            }
            return allowed;
        }

        // Main agent session: check global denylist
        if (config.getDeniedTools().contains(toolName)) {
            logger.warn("Tool '{}' is globally denied for session: {}", 
                toolName, context.sessionKey());
            return false;
        }

        return true;
    }

    /**
     * Validates tool execution and throws exception if not allowed.
     *
     * @param toolName the tool name
     * @param context the session context
     * @throws ToolPolicyViolationException if tool is not allowed
     */
    public void assertToolAllowed(String toolName, SessionContext context) 
            throws ToolPolicyViolationException {
        if (!isToolAllowed(toolName, context)) {
            String sessionType = context != null && context.isSubagentSession() 
                ? "sub-agent" : "main agent";
            throw new ToolPolicyViolationException(
                String.format("Tool '%s' is not allowed for %s session: %s", 
                    toolName, sessionType, 
                    context != null ? context.sessionKey() : "unknown"));
        }
    }

    /**
     * Gets the set of allowed tools for a given session context.
     *
     * @param context the session context
     * @return set of allowed tool names
     */
    public Set<String> getAllowedTools(SessionContext context) {
        if (context != null && context.isSubagentSession()) {
            return config.getSubagentAllowedTools();
        }
        // For main agents, return all tools (empty set means no restriction)
        return Set.of();
    }

    /**
     * Checks if the given session context represents a sub-agent session.
     *
     * @param context the session context
     * @return true if sub-agent session
     */
    public boolean isSubagentSession(SessionContext context) {
        return context != null && context.isSubagentSession();
    }

    /**
     * Exception thrown when a tool policy violation occurs.
     */
    public static class ToolPolicyViolationException extends RuntimeException {
        public ToolPolicyViolationException(String message) {
            super(message);
        }
    }
}
