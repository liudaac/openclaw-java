package openclaw.security.guard;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Input validation guard.
 *
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public class InputValidator {

    // ReDoS-safe patterns
    private static final Pattern SAFE_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9\\s\\-_\\.]+$"
    );

    // Dangerous characters
    private static final String DANGEROUS_CHARS = "<>\"'&;|\\`$(){}[]";

    // SQL injection patterns
    private static final List<Pattern> SQL_INJECTION_PATTERNS = List.of(
            Pattern.compile("(?i)\\b(SELECT|INSERT|UPDATE|DELETE|DROP|CREATE|ALTER|EXEC|EXECUTE)\\b"),
            Pattern.compile("(?i)\\b(OR|AND)\\s+\\d+\\s*=\\s*\\d+"),
            Pattern.compile("'\\s*OR\\s*'"),
            Pattern.compile(";\\s*--"),
            Pattern.compile("/\\*"),
            Pattern.compile("\\*/")
    );

    // XSS patterns
    private static final List<Pattern> XSS_PATTERNS = List.of(
            Pattern.compile("(?i)<script[^>]*>"),
            Pattern.compile("(?i)</script>"),
            Pattern.compile("(?i)javascript:"),
            Pattern.compile("(?i)on\\w+\\s*="),
            Pattern.compile("(?i)<iframe"),
            Pattern.compile("(?i)<object"),
            Pattern.compile("(?i)<embed")
    );

    // Path traversal patterns
    private static final List<Pattern> PATH_TRAVERSAL_PATTERNS = List.of(
            Pattern.compile("\\.\\./"),
            Pattern.compile("\\.\\\\.\\\\"),
            Pattern.compile("%2e%2e%2f", Pattern.CASE_INSENSITIVE),
            Pattern.compile("%252e%252e%252f", Pattern.CASE_INSENSITIVE)
    );

    /**
     * Validates input for general safety.
     *
     * @param input the input
     * @return validation result
     */
    public ValidationResult validate(String input) {
        if (input == null || input.isEmpty()) {
            return ValidationResult.valid();
        }

        // Check for dangerous characters
        for (char c : DANGEROUS_CHARS.toCharArray()) {
            if (input.indexOf(c) >= 0) {
                return ValidationResult.invalid(
                        "Input contains dangerous character: " + c
                );
            }
        }

        // Check length
        if (input.length() > 10000) {
            return ValidationResult.invalid("Input too long");
        }

        return ValidationResult.valid();
    }

    /**
     * Validates input against SQL injection.
     *
     * @param input the input
     * @return validation result
     */
    public ValidationResult validateNoSqlInjection(String input) {
        if (input == null || input.isEmpty()) {
            return ValidationResult.valid();
        }

        for (Pattern pattern : SQL_INJECTION_PATTERNS) {
            if (pattern.matcher(input).find()) {
                return ValidationResult.invalid(
                        "Potential SQL injection detected"
                );
            }
        }

        return ValidationResult.valid();
    }

    /**
     * Validates input against XSS.
     *
     * @param input the input
     * @return validation result
     */
    public ValidationResult validateNoXss(String input) {
        if (input == null || input.isEmpty()) {
            return ValidationResult.valid();
        }

        for (Pattern pattern : XSS_PATTERNS) {
            if (pattern.matcher(input).find()) {
                return ValidationResult.invalid(
                        "Potential XSS detected"
                );
            }
        }

        return ValidationResult.valid();
    }

    /**
     * Validates input against path traversal.
     *
     * @param input the input
     * @return validation result
     */
    public ValidationResult validateNoPathTraversal(String input) {
        if (input == null || input.isEmpty()) {
            return ValidationResult.valid();
        }

        for (Pattern pattern : PATH_TRAVERSAL_PATTERNS) {
            if (pattern.matcher(input).find()) {
                return ValidationResult.invalid(
                        "Potential path traversal detected"
                );
            }
        }

        return ValidationResult.valid();
    }

    /**
     * Sanitizes input for safe display.
     *
     * @param input the input
     * @return sanitized input
     */
    public String sanitize(String input) {
        if (input == null) {
            return "";
        }

        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;")
                .replace("/", "&#x2F;");
    }

    /**
     * Validation result.
     *
     * @param valid whether valid
     * @param reason the reason if invalid
     */
    public record ValidationResult(
            boolean valid,
            Optional<String> reason
    ) {

        /**
         * Creates a valid result.
         *
         * @return the result
         */
        public static ValidationResult valid() {
            return new ValidationResult(true, Optional.empty());
        }

        /**
         * Creates an invalid result.
         *
         * @param reason the reason
         * @return the result
         */
        public static ValidationResult invalid(String reason) {
            return new ValidationResult(false, Optional.of(reason));
        }
    }
}
