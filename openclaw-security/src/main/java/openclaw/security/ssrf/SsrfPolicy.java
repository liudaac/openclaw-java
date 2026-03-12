package openclaw.security.ssrf;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * SSRF (Server-Side Request Forgery) protection policy.
 *
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public interface SsrfPolicy {

    /**
     * Validates a URL against SSRF policies.
     *
     * @param url the URL to validate
     * @return validation result
     */
    SsrfValidationResult validate(String url);

    /**
     * Validates a URI against SSRF policies.
     *
     * @param uri the URI to validate
     * @return validation result
     */
    SsrfValidationResult validate(URI uri);

    /**
     * Checks if an IP address is blocked.
     *
     * @param ip the IP address
     * @return true if blocked
     */
    boolean isIpBlocked(String ip);

    /**
     * Checks if a hostname is blocked.
     *
     * @param hostname the hostname
     * @return true if blocked
     */
    boolean isHostnameBlocked(String hostname);

    /**
     * SSRF validation result.
     *
     * @param allowed whether the URL is allowed
     * @param reason the reason if blocked
     * @param riskLevel the risk level
     */
    record SsrfValidationResult(
            boolean allowed,
            Optional<String> reason,
            RiskLevel riskLevel
    ) {

        /**
         * Creates an allowed result.
         *
         * @return the result
         */
        public static SsrfValidationResult allowed() {
            return new SsrfValidationResult(true, Optional.empty(), RiskLevel.LOW);
        }

        /**
         * Creates a blocked result.
         *
         * @param reason the reason
         * @param riskLevel the risk level
         * @return the result
         */
        public static SsrfValidationResult blocked(String reason, RiskLevel riskLevel) {
            return new SsrfValidationResult(false, Optional.of(reason), riskLevel);
        }

        /**
         * Creates a blocked result with high risk.
         *
         * @param reason the reason
         * @return the result
         */
        public static SsrfValidationResult blocked(String reason) {
            return blocked(reason, RiskLevel.HIGH);
        }
    }

    /**
     * Risk level.
     */
    enum RiskLevel {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }
}
