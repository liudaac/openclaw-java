package openclaw.agent.heartbeat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

/**
 * Processor for heartbeat responses.
 *
 * <p>Handles HEARTBEAT_OK token detection, stripping, and message filtering.</p>
 *
 * @author OpenClaw Team
 * @version 2026.3.17
 */
public class HeartbeatProcessor {

    private static final Logger logger = LoggerFactory.getLogger(HeartbeatProcessor.class);

    /**
     * Result of stripping heartbeat token from text.
     */
    public record StripResult(
        boolean shouldSkip,
        String text,
        boolean didStrip
    ) {
        public static StripResult skip() {
            return new StripResult(true, "", false);
        }

        public static StripResult keep(String text) {
            return new StripResult(false, text, false);
        }

        public static StripResult stripped(String text) {
            return new StripResult(false, text, true);
        }
    }

    /**
     * Mode for token stripping.
     */
    public enum StripMode {
        HEARTBEAT,
        MESSAGE
    }

    private final HeartbeatConfig config;

    // Regex patterns for token detection
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]*>");
    private static final Pattern MARKDOWN_WRAPPER_PATTERN = Pattern.compile("^[*`~_]+|[*`~_]+$");
    private static final Pattern NBSP_PATTERN = Pattern.compile("&nbsp;", Pattern.CASE_INSENSITIVE);

    public HeartbeatProcessor(HeartbeatConfig config) {
        this.config = config;
    }

    /**
     * Check if content is effectively empty (only whitespace, comments, empty list items).
     *
     * <p>This allows skipping heartbeat API calls when HEARTBEAT.md has no actionable tasks.</p>
     *
     * @param content the file content
     * @return true if effectively empty
     */
    public boolean isHeartbeatContentEffectivelyEmpty(String content) {
        if (content == null) {
            return false;
        }

        String[] lines = content.split("\\n");
        for (String line : lines) {
            String trimmed = line.trim();

            // Skip empty lines
            if (trimmed.isEmpty()) {
                continue;
            }

            // Skip markdown header lines (# followed by space or EOL)
            // Intentionally does NOT skip lines like "#TODO" or "#hashtag"
            if (Pattern.matches("^#+(\\s|$)", trimmed)) {
                continue;
            }

            // Skip empty markdown list items like "- [ ]" or "* [ ]" or just "- "
            if (Pattern.matches("^[-*+]\\s*(\\[[\\sXx]?\\]\\s*)?$", trimmed)) {
                continue;
            }

            // Found actionable content
            return false;
        }

        // All lines were empty or comments
        return true;
    }

    /**
     * Strip heartbeat token from text.
     *
     * <p>Handles HEARTBEAT_OK wrapped in HTML/Markdown, with surrounding whitespace,
     * and optional trailing punctuation.</p>
     *
     * @param raw the raw text
     * @param mode the strip mode
     * @return the strip result
     */
    public StripResult stripHeartbeatToken(String raw, StripMode mode) {
        if (raw == null || raw.trim().isEmpty()) {
            return StripResult.skip();
        }

        String trimmed = raw.trim();
        String token = HeartbeatConfig.HEARTBEAT_TOKEN;

        // Normalize lightweight markup
        String normalized = normalizeMarkup(trimmed);
        String normalizedTrimmed = normalized.trim();

        // Check if token exists
        boolean hasToken = trimmed.contains(token) || normalizedTrimmed.contains(token);
        if (!hasToken) {
            return StripResult.keep(trimmed);
        }

        // Try stripping from original and normalized
        StripResult originalResult = stripTokenAtEdges(trimmed, token);
        StripResult normalizedResult = stripTokenAtEdges(normalizedTrimmed, token);

        StripResult picked = originalResult.didStrip && !originalResult.text.isEmpty()
            ? originalResult
            : normalizedResult;

        if (!picked.didStrip) {
            return StripResult.keep(trimmed);
        }

        String rest = picked.text.trim();
        if (rest.isEmpty()) {
            return StripResult.skip();
        }

        // In heartbeat mode, skip if remaining text is within ackMaxChars
        if (mode == StripMode.HEARTBEAT && rest.length() <= config.getAckMaxChars()) {
            return StripResult.skip();
        }

        return StripResult.stripped(rest);
    }

    /**
     * Check if text is a silent reply (only NO_REPLY token).
     *
     * @param text the text
     * @return true if silent reply
     */
    public boolean isSilentReplyText(String text) {
        if (text == null) {
            return false;
        }
        String trimmed = text.trim();
        return trimmed.equals(HeartbeatConfig.SILENT_REPLY_TOKEN);
    }

    /**
     * Strip silent reply token from text.
     *
     * @param text the text
     * @return text with token removed
     */
    public String stripSilentToken(String text) {
        if (text == null) {
            return "";
        }
        String token = HeartbeatConfig.SILENT_REPLY_TOKEN;
        String regex = "(?:^|\\s+|\\*+)" + Pattern.quote(token) + "\\s*$";
        return text.replaceAll(regex, "").trim();
    }

    /**
     * Check if text starts with silent reply prefix.
     *
     * @param text the text
     * @return true if starts with silent prefix
     */
    public boolean isSilentReplyPrefixText(String text) {
        if (text == null) {
            return false;
        }

        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            return false;
        }

        // Guard against suppressing natural-language "No..." text
        if (!trimmed.equals(trimmed.toUpperCase())) {
            return false;
        }

        String normalized = trimmed.toUpperCase();
        if (normalized.length() < 2) {
            return false;
        }

        if (!Pattern.matches("^[A-Z_]+$", normalized)) {
            return false;
        }

        String tokenUpper = HeartbeatConfig.SILENT_REPLY_TOKEN.toUpperCase();
        if (!tokenUpper.startsWith(normalized)) {
            return false;
        }

        if (normalized.contains("_")) {
            return true;
        }

        // Only allow bare "NO" because NO_REPLY streaming can transiently emit that fragment
        return tokenUpper.equals(HeartbeatConfig.SILENT_REPLY_TOKEN) && normalized.equals("NO");
    }

    /**
     * Check if delivery should be skipped (heartbeat-only response).
     *
     * @param text the response text
     * @param hasMedia whether response has media
     * @return true if should skip delivery
     */
    public boolean shouldSkipHeartbeatOnlyDelivery(String text, boolean hasMedia) {
        if (hasMedia) {
            return false;
        }

        StripResult result = stripHeartbeatToken(text, StripMode.HEARTBEAT);
        return result.shouldSkip();
    }

    /**
     * Normalize markup in text (HTML tags, nbsp, markdown wrappers).
     *
     * @param text the text
     * @return normalized text
     */
    private String normalizeMarkup(String text) {
        String result = HTML_TAG_PATTERN.matcher(text).replaceAll(" ");
        result = NBSP_PATTERN.matcher(result).replaceAll(" ");
        result = MARKDOWN_WRAPPER_PATTERN.matcher(result).replaceAll("");
        return result;
    }

    /**
     * Strip token from edges of text.
     *
     * @param text the text
     * @param token the token to strip
     * @return the strip result
     */
    private StripResult stripTokenAtEdges(String text, String token) {
        if (text == null || text.isEmpty()) {
            return StripResult.keep("");
        }

        String result = text.trim();
        if (result.isEmpty()) {
            return StripResult.keep("");
        }

        // Pattern for token at end with optional trailing punctuation (up to 4 chars)
        String tokenEscaped = Pattern.quote(token);
        Pattern tokenAtEndPattern = Pattern.compile(
            tokenEscaped + "[^\\w]{0,4}$"
        );

        if (!result.contains(token)) {
            return StripResult.keep(result);
        }

        boolean didStrip = false;
        boolean changed = true;

        while (changed) {
            changed = false;
            String next = result.trim();

            // Strip from beginning
            if (next.startsWith(token)) {
                String after = next.substring(token.length()).trim();
                result = after;
                didStrip = true;
                changed = true;
                continue;
            }

            // Strip from end (with optional trailing punctuation)
            if (tokenAtEndPattern.matcher(next).find()) {
                int idx = next.lastIndexOf(token);
                if (idx >= 0) {
                    String before = next.substring(0, idx).trim();
                    String after = next.substring(idx + token.length()).trim();
                    result = (before + after).trim();
                    didStrip = true;
                    changed = true;
                }
            }
        }

        // Collapse multiple spaces
        String collapsed = result.replaceAll("\\s+", " ").trim();
        return new StripResult(false, collapsed, didStrip);
    }
}