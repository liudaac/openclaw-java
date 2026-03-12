package openclaw.security;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * Safe regex utilities to prevent ReDoS attacks.
 *
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public class SafeRegex {

    // Maximum regex execution time in milliseconds
    private static final long DEFAULT_TIMEOUT_MS = 1000;

    // Dangerous patterns that can cause catastrophic backtracking
    private static final Set<String> DANGEROUS_PATTERNS = Set.of(
            "(a+)+",
            "([a-zA-Z]+)*",
            "(a|aa)+",
            "(a|a?)+",
            "(.*a){x}",
            "(\\w+\\s?)*\\w*",
            "(\\S+\\s?)*\\S*"
    );

    /**
     * Checks if a regex pattern is safe (won't cause ReDoS).
     *
     * @param pattern the regex pattern
     * @return true if safe
     */
    public static boolean isSafe(String pattern) {
        if (pattern == null || pattern.isEmpty()) {
            return true;
        }

        // Check for dangerous patterns
        for (String dangerous : DANGEROUS_PATTERNS) {
            if (pattern.contains(dangerous)) {
                return false;
            }
        }

        // Check for nested quantifiers
        if (hasNestedQuantifiers(pattern)) {
            return false;
        }

        // Check for excessive alternation
        if (hasExcessiveAlternation(pattern)) {
            return false;
        }

        return true;
    }

    /**
     * Validates a pattern and throws if unsafe.
     *
     * @param pattern the pattern
     * @throws UnsafeRegexException if pattern is unsafe
     */
    public static void validate(String pattern) {
        if (!isSafe(pattern)) {
            throw new UnsafeRegexException("Potentially unsafe regex pattern: " + pattern);
        }
    }

    /**
     * Matches input against pattern with timeout protection.
     *
     * @param input the input
     * @param pattern the pattern
     * @return true if matches
     * @throws RegexTimeoutException if execution times out
     */
    public static boolean matches(String input, String pattern) {
        return matches(input, pattern, DEFAULT_TIMEOUT_MS);
    }

    /**
     * Matches input against pattern with custom timeout.
     *
     * @param input the input
     * @param pattern the pattern
     * @param timeoutMs timeout in milliseconds
     * @return true if matches
     * @throws RegexTimeoutException if execution times out
     */
    public static boolean matches(String input, String pattern, long timeoutMs) {
        validate(pattern);

        // Use interruptible matching
        return matchWithTimeout(input, pattern, timeoutMs);
    }

    private static boolean matchWithTimeout(String input, String pattern, long timeoutMs) {
        // Simple implementation - in production use RE2/J or similar
        Thread matchThread = new Thread(() -> {
            try {
                Pattern.compile(pattern).matcher(input).matches();
            } catch (Exception e) {
                // Ignore
            }
        });

        matchThread.start();
        try {
            matchThread.join(timeoutMs);
            if (matchThread.isAlive()) {
                matchThread.interrupt();
                throw new RegexTimeoutException("Regex execution timed out");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RegexTimeoutException("Matching interrupted");
        }

        // Actually perform the match
        return Pattern.compile(pattern).matcher(input).matches();
    }

    private static boolean hasNestedQuantifiers(String pattern) {
        // Check for patterns like (a+)+, (a*)*, etc.
        int depth = 0;
        boolean inQuantifier = false;

        for (char c : pattern.toCharArray()) {
            switch (c) {
                case '(':
                    depth++;
                    inQuantifier = false;
                    break;
                case ')':
                    if (inQuantifier && depth > 0) {
                        return true;
                    }
                    depth--;
                    inQuantifier = false;
                    break;
                case '*', '+', '?', '{':
                    if (depth > 0) {
                        inQuantifier = true;
                    }
                    break;
            }
        }
        return false;
    }

    private static boolean hasExcessiveAlternation(String pattern) {
        // Count alternations
        long alternationCount = pattern.chars().filter(c -> c == '|').count();
        return alternationCount > 10;
    }

    /**
     * Unsafe regex exception.
     */
    public static class UnsafeRegexException extends RuntimeException {
        public UnsafeRegexException(String message) {
            super(message);
        }
    }

    /**
     * Regex timeout exception.
     */
    public static class RegexTimeoutException extends RuntimeException {
        public RegexTimeoutException(String message) {
            super(message);
        }
    }
}
