package openclaw.gateway.auth;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Operator scope definitions and authorization utilities.
 *
 * <p>Defines the scope-based access control system for Gateway operations.
 * Mirrors the TypeScript implementation in src/gateway/method-scopes.ts</p>
 *
 * @author OpenClaw Team
 * @version 2026.3.28
 * @since 2026.3.28
 */
public final class OperatorScopes {

    // Scope constants
    public static final String ADMIN_SCOPE = "operator.admin";
    public static final String READ_SCOPE = "operator.read";
    public static final String WRITE_SCOPE = "operator.write";
    public static final String APPROVALS_SCOPE = "operator.approvals";
    public static final String PAIRING_SCOPE = "operator.pairing";

    // Default CLI scopes
    public static final List<String> CLI_DEFAULT_SCOPES = Arrays.asList(
        ADMIN_SCOPE,
        READ_SCOPE,
        WRITE_SCOPE,
        APPROVALS_SCOPE,
        PAIRING_SCOPE
    );

    // Method scope mappings
    private static final Map<String, String> METHOD_SCOPE_MAP = new HashMap<>();
    private static final Set<String> NODE_ROLE_METHODS = new HashSet<>();
    private static final List<String> ADMIN_METHOD_PREFIXES = Arrays.asList(
        "exec.approvals.",
        "config.",
        "wizard.",
        "update."
    );

    static {
        // Initialize method scope mappings
        initMethodScopes();
    }

    private static void initMethodScopes() {
        // Approvals scope methods
        METHOD_SCOPE_MAP.put("exec.approval.request", APPROVALS_SCOPE);
        METHOD_SCOPE_MAP.put("exec.approval.waitDecision", APPROVALS_SCOPE);
        METHOD_SCOPE_MAP.put("exec.approval.resolve", APPROVALS_SCOPE);

        // Pairing scope methods
        METHOD_SCOPE_MAP.put("node.pair.request", PAIRING_SCOPE);
        METHOD_SCOPE_MAP.put("node.pair.list", PAIRING_SCOPE);
        METHOD_SCOPE_MAP.put("node.pair.approve", PAIRING_SCOPE);
        METHOD_SCOPE_MAP.put("node.pair.reject", PAIRING_SCOPE);
        METHOD_SCOPE_MAP.put("node.pair.verify", PAIRING_SCOPE);
        METHOD_SCOPE_MAP.put("device.pair.list", PAIRING_SCOPE);
        METHOD_SCOPE_MAP.put("device.pair.approve", PAIRING_SCOPE);
        METHOD_SCOPE_MAP.put("device.pair.reject", PAIRING_SCOPE);
        METHOD_SCOPE_MAP.put("device.pair.remove", PAIRING_SCOPE);
        METHOD_SCOPE_MAP.put("device.token.rotate", PAIRING_SCOPE);
        METHOD_SCOPE_MAP.put("device.token.revoke", PAIRING_SCOPE);
        METHOD_SCOPE_MAP.put("node.rename", PAIRING_SCOPE);

        // Read scope methods
        METHOD_SCOPE_MAP.put("health", READ_SCOPE);
        METHOD_SCOPE_MAP.put("doctor.memory.status", READ_SCOPE);
        METHOD_SCOPE_MAP.put("logs.tail", READ_SCOPE);
        METHOD_SCOPE_MAP.put("channels.status", READ_SCOPE);
        METHOD_SCOPE_MAP.put("status", READ_SCOPE);
        METHOD_SCOPE_MAP.put("usage.status", READ_SCOPE);
        METHOD_SCOPE_MAP.put("usage.cost", READ_SCOPE);
        METHOD_SCOPE_MAP.put("tts.status", READ_SCOPE);
        METHOD_SCOPE_MAP.put("tts.providers", READ_SCOPE);
        METHOD_SCOPE_MAP.put("models.list", READ_SCOPE);
        METHOD_SCOPE_MAP.put("tools.catalog", READ_SCOPE);
        METHOD_SCOPE_MAP.put("tools.effective", READ_SCOPE);
        METHOD_SCOPE_MAP.put("agents.list", READ_SCOPE);
        METHOD_SCOPE_MAP.put("agent.identity.get", READ_SCOPE);
        METHOD_SCOPE_MAP.put("skills.status", READ_SCOPE);
        METHOD_SCOPE_MAP.put("voicewake.get", READ_SCOPE);
        METHOD_SCOPE_MAP.put("sessions.list", READ_SCOPE);
        METHOD_SCOPE_MAP.put("sessions.get", READ_SCOPE);
        METHOD_SCOPE_MAP.put("sessions.preview", READ_SCOPE);
        METHOD_SCOPE_MAP.put("sessions.resolve", READ_SCOPE);
        METHOD_SCOPE_MAP.put("sessions.subscribe", READ_SCOPE);
        METHOD_SCOPE_MAP.put("sessions.unsubscribe", READ_SCOPE);
        METHOD_SCOPE_MAP.put("sessions.messages.subscribe", READ_SCOPE);
        METHOD_SCOPE_MAP.put("sessions.messages.unsubscribe", READ_SCOPE);
        METHOD_SCOPE_MAP.put("sessions.usage", READ_SCOPE);
        METHOD_SCOPE_MAP.put("sessions.usage.timeseries", READ_SCOPE);
        METHOD_SCOPE_MAP.put("sessions.usage.logs", READ_SCOPE);
        METHOD_SCOPE_MAP.put("cron.list", READ_SCOPE);
        METHOD_SCOPE_MAP.put("cron.status", READ_SCOPE);
        METHOD_SCOPE_MAP.put("cron.runs", READ_SCOPE);
        METHOD_SCOPE_MAP.put("gateway.identity.get", READ_SCOPE);
        METHOD_SCOPE_MAP.put("system-presence", READ_SCOPE);
        METHOD_SCOPE_MAP.put("last-heartbeat", READ_SCOPE);
        METHOD_SCOPE_MAP.put("node.list", READ_SCOPE);
        METHOD_SCOPE_MAP.put("node.describe", READ_SCOPE);
        METHOD_SCOPE_MAP.put("chat.history", READ_SCOPE);
        METHOD_SCOPE_MAP.put("config.get", READ_SCOPE);
        METHOD_SCOPE_MAP.put("config.schema.lookup", READ_SCOPE);
        METHOD_SCOPE_MAP.put("talk.config", READ_SCOPE);
        METHOD_SCOPE_MAP.put("agents.files.list", READ_SCOPE);
        METHOD_SCOPE_MAP.put("agents.files.get", READ_SCOPE);

        // Write scope methods
        METHOD_SCOPE_MAP.put("send", WRITE_SCOPE);
        METHOD_SCOPE_MAP.put("poll", WRITE_SCOPE);
        METHOD_SCOPE_MAP.put("agent", WRITE_SCOPE);
        METHOD_SCOPE_MAP.put("agent.wait", WRITE_SCOPE);
        METHOD_SCOPE_MAP.put("wake", WRITE_SCOPE);
        METHOD_SCOPE_MAP.put("talk.mode", WRITE_SCOPE);
        METHOD_SCOPE_MAP.put("talk.speak", WRITE_SCOPE);
        METHOD_SCOPE_MAP.put("tts.enable", WRITE_SCOPE);
        METHOD_SCOPE_MAP.put("tts.disable", WRITE_SCOPE);
        METHOD_SCOPE_MAP.put("tts.convert", WRITE_SCOPE);
        METHOD_SCOPE_MAP.put("tts.setProvider", WRITE_SCOPE);
        METHOD_SCOPE_MAP.put("voicewake.set", WRITE_SCOPE);
        METHOD_SCOPE_MAP.put("node.invoke", WRITE_SCOPE);
        METHOD_SCOPE_MAP.put("chat.send", WRITE_SCOPE);
        METHOD_SCOPE_MAP.put("chat.abort", WRITE_SCOPE);
        METHOD_SCOPE_MAP.put("sessions.create", WRITE_SCOPE);
        METHOD_SCOPE_MAP.put("sessions.send", WRITE_SCOPE);
        METHOD_SCOPE_MAP.put("sessions.steer", WRITE_SCOPE);
        METHOD_SCOPE_MAP.put("sessions.abort", WRITE_SCOPE);
        METHOD_SCOPE_MAP.put("push.test", WRITE_SCOPE);
        METHOD_SCOPE_MAP.put("node.pending.enqueue", WRITE_SCOPE);

        // Admin scope methods
        METHOD_SCOPE_MAP.put("channels.logout", ADMIN_SCOPE);
        METHOD_SCOPE_MAP.put("agents.create", ADMIN_SCOPE);
        METHOD_SCOPE_MAP.put("agents.update", ADMIN_SCOPE);
        METHOD_SCOPE_MAP.put("agents.delete", ADMIN_SCOPE);
        METHOD_SCOPE_MAP.put("skills.install", ADMIN_SCOPE);
        METHOD_SCOPE_MAP.put("skills.update", ADMIN_SCOPE);
        METHOD_SCOPE_MAP.put("secrets.reload", ADMIN_SCOPE);
        METHOD_SCOPE_MAP.put("secrets.resolve", ADMIN_SCOPE);
        METHOD_SCOPE_MAP.put("cron.add", ADMIN_SCOPE);
        METHOD_SCOPE_MAP.put("cron.update", ADMIN_SCOPE);
        METHOD_SCOPE_MAP.put("cron.remove", ADMIN_SCOPE);
        METHOD_SCOPE_MAP.put("cron.run", ADMIN_SCOPE);
        METHOD_SCOPE_MAP.put("sessions.patch", ADMIN_SCOPE);
        METHOD_SCOPE_MAP.put("sessions.reset", ADMIN_SCOPE);
        METHOD_SCOPE_MAP.put("sessions.delete", ADMIN_SCOPE);
        METHOD_SCOPE_MAP.put("sessions.compact", ADMIN_SCOPE);
        METHOD_SCOPE_MAP.put("connect", ADMIN_SCOPE);
        METHOD_SCOPE_MAP.put("chat.inject", ADMIN_SCOPE);
        METHOD_SCOPE_MAP.put("web.login.start", ADMIN_SCOPE);
        METHOD_SCOPE_MAP.put("web.login.wait", ADMIN_SCOPE);
        METHOD_SCOPE_MAP.put("web.login.complete", ADMIN_SCOPE);
        METHOD_SCOPE_MAP.put("set-heartbeats", ADMIN_SCOPE);
        METHOD_SCOPE_MAP.put("system-event", ADMIN_SCOPE);
        METHOD_SCOPE_MAP.put("agents.files.set", ADMIN_SCOPE);

        // Node role methods
        NODE_ROLE_METHODS.add("node.invoke.result");
        NODE_ROLE_METHODS.add("node.event");
        NODE_ROLE_METHODS.add("node.pending.drain");
        NODE_ROLE_METHODS.add("node.canvas.capability.refresh");
        NODE_ROLE_METHODS.add("node.pending.pull");
        NODE_ROLE_METHODS.add("node.pending.ack");
        NODE_ROLE_METHODS.add("skills.bins");
    }

    private OperatorScopes() {
        // Utility class
    }

    /**
     * Resolves the required scope for a method.
     *
     * @param method the method name
     * @return the required scope, or null if not classified
     */
    public static String resolveRequiredScope(String method) {
        String scope = METHOD_SCOPE_MAP.get(method);
        if (scope != null) {
            return scope;
        }

        // Check admin prefixes
        for (String prefix : ADMIN_METHOD_PREFIXES) {
            if (method.startsWith(prefix)) {
                return ADMIN_SCOPE;
            }
        }

        return null;
    }

    /**
     * Checks if a method is an approval method.
     *
     * @param method the method name
     * @return true if approval method
     */
    public static boolean isApprovalMethod(String method) {
        return APPROVALS_SCOPE.equals(resolveRequiredScope(method));
    }

    /**
     * Checks if a method is a pairing method.
     *
     * @param method the method name
     * @return true if pairing method
     */
    public static boolean isPairingMethod(String method) {
        return PAIRING_SCOPE.equals(resolveRequiredScope(method));
    }

    /**
     * Checks if a method is a read method.
     *
     * @param method the method name
     * @return true if read method
     */
    public static boolean isReadMethod(String method) {
        return READ_SCOPE.equals(resolveRequiredScope(method));
    }

    /**
     * Checks if a method is a write method.
     *
     * @param method the method name
     * @return true if write method
     */
    public static boolean isWriteMethod(String method) {
        return WRITE_SCOPE.equals(resolveRequiredScope(method));
    }

    /**
     * Checks if a method is a node role method.
     *
     * @param method the method name
     * @return true if node role method
     */
    public static boolean isNodeRoleMethod(String method) {
        return NODE_ROLE_METHODS.contains(method);
    }

    /**
     * Checks if a method is admin only.
     *
     * @param method the method name
     * @return true if admin only
     */
    public static boolean isAdminOnlyMethod(String method) {
        return ADMIN_SCOPE.equals(resolveRequiredScope(method));
    }

    /**
     * Checks if a method is classified.
     *
     * @param method the method name
     * @return true if classified
     */
    public static boolean isClassified(String method) {
        if (isNodeRoleMethod(method)) {
            return true;
        }
        return resolveRequiredScope(method) != null;
    }

    /**
     * Authorizes a method call with the given scopes.
     *
     * @param method the method name
     * @param scopes the granted scopes
     * @return authorization result
     */
    public static AuthResult authorize(String method, List<String> scopes) {
        // Admin scope grants all access
        if (scopes.contains(ADMIN_SCOPE)) {
            return AuthResult.allowed();
        }

        String requiredScope = resolveRequiredScope(method);
        if (requiredScope == null) {
            // Default deny for unclassified methods
            requiredScope = ADMIN_SCOPE;
        }

        // Read scope allows read and write methods
        if (READ_SCOPE.equals(requiredScope)) {
            if (scopes.contains(READ_SCOPE) || scopes.contains(WRITE_SCOPE)) {
                return AuthResult.allowed();
            }
            return AuthResult.denied(READ_SCOPE);
        }

        // Other scopes require exact match
        if (scopes.contains(requiredScope)) {
            return AuthResult.allowed();
        }

        return AuthResult.denied(requiredScope);
    }

    /**
     * Checks if the given scopes can inject system provenance.
     *
     * @param scopes the scopes to check
     * @return true if can inject system provenance
     */
    public static boolean canInjectSystemProvenance(List<String> scopes) {
        return scopes != null && scopes.contains(ADMIN_SCOPE);
    }

    /**
     * Authorization result.
     */
    public static class AuthResult {
        private final boolean allowed;
        private final String missingScope;

        private AuthResult(boolean allowed, String missingScope) {
            this.allowed = allowed;
            this.missingScope = missingScope;
        }

        public static AuthResult allowed() {
            return new AuthResult(true, null);
        }

        public static AuthResult denied(String missingScope) {
            return new AuthResult(false, missingScope);
        }

        public boolean isAllowed() {
            return allowed;
        }

        public String getMissingScope() {
            return missingScope;
        }
    }
}