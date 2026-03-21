package openclaw.channel.matrix;

import java.util.regex.Pattern;

/**
 * Matrix mention pattern builder with agent ID support.
 *
 * @author OpenClaw Team
 * @version 2026.3.21
 * @since 2026.3.21
 */
public class MatrixMentionPattern {

    private static final String DEFAULT_MXID_LOCALPART = "openclaw";

    /**
     * Builds mention regex patterns for Matrix.
     *
     * @param agentId the agent ID (optional)
     * @return the mention patterns
     */
    public static MentionPatterns build(String agentId) {
        String localpart = agentId != null && !agentId.isBlank()
                ? sanitizeLocalpart(agentId)
                : DEFAULT_MXID_LOCALPART;

        // Build patterns for different mention formats
        // Format: @localpart:domain or @localpart
        String mentionPattern = "@" + Pattern.quote(localpart) + "(:[^\\s]+)?";

        // Also match display name mentions if configured
        String displayNamePattern = buildDisplayNamePattern(agentId);

        return new MentionPatterns(
                Pattern.compile(mentionPattern, Pattern.CASE_INSENSITIVE),
                displayNamePattern != null ? Pattern.compile(displayNamePattern, Pattern.CASE_INSENSITIVE) : null
        );
    }

    /**
     * Sanitizes a localpart for Matrix MXID.
     */
    private static String sanitizeLocalpart(String agentId) {
        // Matrix localpart rules: lowercase, no special chars except ._=-
        return agentId.toLowerCase()
                .replaceAll("[^a-z0-9._=-]", "")
                .replaceAll("^[^a-z0-9]+", "") // Remove leading non-alphanumeric
                .replaceAll("[^a-z0-9]+$", ""); // Remove trailing non-alphanumeric
    }

    /**
     * Builds display name pattern.
     */
    private static String buildDisplayNamePattern(String agentId) {
        if (agentId == null || agentId.isBlank()) {
            return null;
        }
        // Match display name mentions
        return "\\b" + Pattern.quote(agentId) + "\\b";
    }

    /**
     * Mention patterns.
     */
    public record MentionPatterns(
            Pattern mxidPattern,
            Pattern displayNamePattern
    ) {
        /**
         * Checks if text contains a mention.
         *
         * @param text the text
         * @return true if contains mention
         */
        public boolean containsMention(String text) {
            if (text == null) {
                return false;
            }
            if (mxidPattern != null && mxidPattern.matcher(text).find()) {
                return true;
            }
            if (displayNamePattern != null && displayNamePattern.matcher(text).find()) {
                return true;
            }
            return false;
        }
    }
}
