package openclaw.gateway.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Pre-authentication WebSocket connection budget manager.
 *
 * <p>Limits the number of concurrent pre-authentication WebSocket upgrades
 * per client IP to prevent resource exhaustion attacks.</p>
 *
 * <p>Mirrors the TypeScript implementation in src/gateway/server/preauth-connection-budget.ts</p>
 *
 * @author OpenClaw Team
 * @version 2026.3.28
 * @since 2026.3.28
 */
public class PreauthConnectionBudget {

    private static final Logger logger = LoggerFactory.getLogger(PreauthConnectionBudget.class);

    /**
     * Default maximum pre-authentication connections per IP.
     */
    public static final int DEFAULT_MAX_PREAUTH_CONNECTIONS_PER_IP = 32;

    /**
     * Budget key for unknown client IPs.
     */
    public static final String UNKNOWN_CLIENT_IP_BUDGET_KEY = "__openclaw_unknown_client_ip__";

    /**
     * Environment variable for configuring the limit.
     */
    public static final String ENV_MAX_PREAUTH_CONNECTIONS = "OPENCLAW_MAX_PREAUTH_CONNECTIONS_PER_IP";

    private final int limit;
    private final Map<String, Integer> counts = new ConcurrentHashMap<>();

    /**
     * Creates a new pre-authentication connection budget with default limit.
     */
    public PreauthConnectionBudget() {
        this(getLimitFromEnv());
    }

    /**
     * Creates a new pre-authentication connection budget with specified limit.
     *
     * @param limit the maximum connections per IP
     */
    public PreauthConnectionBudget(int limit) {
        this.limit = Math.max(1, limit);
        logger.info("PreauthConnectionBudget initialized with limit: {}", this.limit);
    }

    /**
     * Gets the limit from environment variable or uses default.
     *
     * @return the configured limit
     */
    public static int getLimitFromEnv() {
        String configured = System.getenv(ENV_MAX_PREAUTH_CONNECTIONS);
        if (configured == null || configured.isEmpty()) {
            return DEFAULT_MAX_PREAUTH_CONNECTIONS_PER_IP;
        }

        try {
            int parsed = Integer.parseInt(configured);
            if (!Double.isFinite(parsed) || parsed < 1) {
                return DEFAULT_MAX_PREAUTH_CONNECTIONS_PER_IP;
            }
            return Math.max(1, (int) Math.floor(parsed));
        } catch (NumberFormatException e) {
            return DEFAULT_MAX_PREAUTH_CONNECTIONS_PER_IP;
        }
    }

    /**
     * Normalizes the budget key for a client IP.
     *
     * @param clientIp the client IP address
     * @return the normalized budget key
     */
    private String normalizeBudgetKey(String clientIp) {
        // Trusted-proxy mode can intentionally leave client IP unresolved when
        // forwarded headers are missing or invalid; keep those upgrades capped
        // under a shared fallback bucket instead of failing open.
        String ip = clientIp != null ? clientIp.trim() : null;
        return (ip != null && !ip.isEmpty()) ? ip : UNKNOWN_CLIENT_IP_BUDGET_KEY;
    }

    /**
     * Attempts to acquire a connection slot for the given client IP.
     *
     * @param clientIp the client IP address
     * @return true if the connection is allowed, false if the budget is exceeded
     */
    public boolean acquire(String clientIp) {
        String ip = normalizeBudgetKey(clientIp);

        Integer current = counts.get(ip);
        int next = (current != null ? current : 0) + 1;

        if (next > limit) {
            logger.warn("Pre-auth connection budget exceeded for IP: {} (limit: {})", ip, limit);
            return false;
        }

        counts.put(ip, next);
        logger.debug("Acquired pre-auth connection slot for IP: {} (count: {})", ip, next);
        return true;
    }

    /**
     * Releases a connection slot for the given client IP.
     *
     * @param clientIp the client IP address
     */
    public void release(String clientIp) {
        String ip = normalizeBudgetKey(clientIp);

        Integer current = counts.get(ip);
        if (current == null) {
            return;
        }

        if (current <= 1) {
            counts.remove(ip);
            logger.debug("Released pre-auth connection slot for IP: {} (count: 0)", ip);
        } else {
            counts.put(ip, current - 1);
            logger.debug("Released pre-auth connection slot for IP: {} (count: {})", ip, current - 1);
        }
    }

    /**
     * Gets the current connection count for a specific IP.
     *
     * @param clientIp the client IP address
     * @return the current connection count
     */
    public int getConnectionCount(String clientIp) {
        String ip = normalizeBudgetKey(clientIp);
        return counts.getOrDefault(ip, 0);
    }

    /**
     * Gets the total connection count across all IPs.
     *
     * @return the total connection count
     */
    public int getTotalConnectionCount() {
        return counts.values().stream().mapToInt(Integer::intValue).sum();
    }

    /**
     * Gets the configured limit.
     *
     * @return the limit
     */
    public int getLimit() {
        return limit;
    }
}
