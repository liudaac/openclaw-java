package openclaw.security.config;

import java.util.*;

/**
 * Validator for security-related configuration.
 *
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public class SecurityConfigValidator {

    // Dangerous configuration flags
    private static final Set<String> DANGEROUS_FLAGS = Set.of(
            "security.ssrf.disabled",
            "security.cors.allowAll",
            "security.csrf.disabled",
            "security.xss.disabled",
            "security.tls.verifyDisabled",
            "security.auth.disabled",
            "security.audit.disabled",
            "security.rateLimit.disabled",
            "security.inputValidation.disabled",
            "security.fileUpload.unrestricted",
            "security.exec.unrestricted",
            "security.network.unrestricted"
    );

    // Security best practices
    private static final Map<String, SecurityCheck> SECURITY_CHECKS = Map.of(
            "security.password.minLength",
            new SecurityCheck("Password minimum length", v -> v instanceof Integer && (Integer) v >= 8),

            "security.session.timeoutMinutes",
            new SecurityCheck("Session timeout", v -> v instanceof Integer && (Integer) v <= 1440),

            "security.rateLimit.requestsPerMinute",
            new SecurityCheck("Rate limit", v -> v instanceof Integer && (Integer) v > 0),

            "security.tls.minVersion",
            new SecurityCheck("TLS version", v -> v instanceof String && 
                    Set.of("1.2", "1.3").contains(v))
    );

    /**
     * Validates configuration for security issues.
     *
     * @param config the configuration
     * @return validation result
     */
    public ValidationResult validate(Map<String, Object> config) {
        List<String> warnings = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        // Check for dangerous flags
        for (String flag : DANGEROUS_FLAGS) {
            Object value = getNestedValue(config, flag);
            if (value != null) {
                if (Boolean.TRUE.equals(value) || "true".equals(value)) {
                    errors.add("Dangerous security flag enabled: " + flag);
                }
            }
        }

        // Run security checks
        for (Map.Entry<String, SecurityCheck> entry : SECURITY_CHECKS.entrySet()) {
            String key = entry.getKey();
            SecurityCheck check = entry.getValue();
            Object value = getNestedValue(config, key);

            if (value != null) {
                if (!check.validator().test(value)) {
                    warnings.add("Security check failed for " + key + ": " + check.description());
                }
            }
        }

        // Check for missing security settings
        Set<String> missingKeys = new HashSet<>(SECURITY_CHECKS.keySet());
        missingKeys.removeAll(config.keySet());

        for (String key : missingKeys) {
            if (key.startsWith("security.")) {
                warnings.add("Missing security configuration: " + key);
            }
        }

        return new ValidationResult(errors.isEmpty(), warnings, errors);
    }

    /**
     * Gets a nested value from the config.
     *
     * @param config the config
     * @param path the dot-separated path
     * @return the value or null
     */
    private Object getNestedValue(Map<String, Object> config, String path) {
        String[] parts = path.split("\\.");
        Object current = config;

        for (String part : parts) {
            if (current instanceof Map) {
                current = ((Map<?, ?>) current).get(part);
                if (current == null) {
                    return null;
                }
            } else {
                return null;
            }
        }

        return current;
    }

    /**
     * Validation result.
     *
     * @param valid whether the config is valid
     * @param warnings list of warnings
     * @param errors list of errors
     */
    public record ValidationResult(
            boolean valid,
            List<String> warnings,
            List<String> errors
    ) {

        /**
         * Checks if there are any issues.
         *
         * @return true if there are warnings or errors
         */
        public boolean hasIssues() {
            return !warnings.isEmpty() || !errors.isEmpty();
        }

        /**
         * Gets all issues.
         *
         * @return list of all issues
         */
        public List<String> allIssues() {
            List<String> all = new ArrayList<>();
            all.addAll(errors);
            all.addAll(warnings);
            return all;
        }
    }

    /**
     * Security check.
     *
     * @param description the description
     * @param validator the validator
     */
    private record SecurityCheck(
            String description,
            java.util.function.Predicate<Object> validator
    ) {
    }
}
