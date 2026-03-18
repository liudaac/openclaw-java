package openclaw.plugin.sdk.websearch.utils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.Optional;

/**
 * Validation utilities for web search.
 *
 * @author OpenClaw Team
 * @version 2026.3.18
 */
public final class ValidationUtils {

    private ValidationUtils() {
        // Utility class
    }

    // Constants
    public static final int DEFAULT_SEARCH_COUNT = 5;
    public static final int MAX_SEARCH_COUNT = 10;
    public static final int DEFAULT_TIMEOUT_SECONDS = 30;
    public static final int DEFAULT_CACHE_TTL_MINUTES = 60;

    /**
     * Read string parameter from args.
     *
     * @param params the parameters
     * @param key the key
     * @return the string value or null
     */
    public static String readStringParam(Map<String, Object> params, String key) {
        return readStringParam(params, key, false);
    }

    /**
     * Read string parameter from args.
     *
     * @param params the parameters
     * @param key the key
     * @param required if true, throws exception if missing
     * @return the string value
     * @throws IllegalArgumentException if required and missing
     */
    public static String readStringParam(Map<String, Object> params, String key, boolean required) {
        Object value = params.get(key);
        if (value == null) {
            if (required) {
                throw new IllegalArgumentException("Missing required parameter: " + key);
            }
            return null;
        }
        String str = value.toString().trim();
        return str.isEmpty() ? null : str;
    }

    /**
     * Read integer parameter from args.
     *
     * @param params the parameters
     * @param key the key
     * @return the integer value or null
     */
    public static Integer readNumberParam(Map<String, Object> params, String key) {
        return readNumberParam(params, key, false);
    }

    /**
     * Read integer parameter from args.
     *
     * @param params the parameters
     * @param key the key
     * @param integerOnly if true, validates the value is an integer
     * @return the integer value or null
     */
    public static Integer readNumberParam(Map<String, Object> params, String key, boolean integerOnly) {
        Object value = params.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Read string array parameter from args.
     *
     * @param params the parameters
     * @param key the key
     * @return the string array or empty array
     */
    @SuppressWarnings("unchecked")
    public static String[] readStringArrayParam(Map<String, Object> params, String key) {
        Object value = params.get(key);
        if (value == null) {
            return new String[0];
        }
        if (value instanceof String[]) {
            return (String[]) value;
        }
        if (value instanceof java.util.List) {
            return ((java.util.List<String>) value).toArray(new String[0]);
        }
        return new String[]{value.toString()};
    }

    /**
     * Normalize freshness parameter.
     *
     * @param freshness the freshness value
     * @param provider the provider name
     * @return normalized freshness or null if invalid
     */
    public static String normalizeFreshness(String freshness, String provider) {
        if (freshness == null || freshness.isEmpty()) {
            return null;
        }

        String normalized = freshness.toLowerCase().trim();

        return switch (normalized) {
            case "day", "24h", "24 hours" -> "day";
            case "week", "7d", "7 days" -> "week";
            case "month", "30d", "30 days" -> "month";
            case "year", "365d", "365 days" -> "year";
            default -> null;
        };
    }

    /**
     * Normalize date to ISO format (YYYY-MM-DD).
     *
     * @param date the date string
     * @return ISO date string or null if invalid
     */
    public static String normalizeToIsoDate(String date) {
        if (date == null || date.isEmpty()) {
            return null;
        }

        // Try common formats
        String[] formats = {"yyyy-MM-dd", "yyyy/MM/dd", "dd-MM-yyyy", "dd/MM/yyyy"};

        for (String format : formats) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
                LocalDate parsed = LocalDate.parse(date.trim(), formatter);
                return parsed.format(DateTimeFormatter.ISO_LOCAL_DATE);
            } catch (DateTimeParseException e) {
                // Try next format
            }
        }

        return null;
    }

    /**
     * Convert ISO date to Perplexity format (MM/DD/YYYY).
     *
     * @param isoDate the ISO date string
     * @return Perplexity format date
     */
    public static String isoToPerplexityDate(String isoDate) {
        if (isoDate == null || isoDate.isEmpty()) {
            return null;
        }
        try {
            LocalDate date = LocalDate.parse(isoDate, DateTimeFormatter.ISO_LOCAL_DATE);
            return date.format(DateTimeFormatter.ofPattern("MM/dd/yyyy"));
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /**
     * Resolve search count with bounds checking.
     *
     * @param count the requested count
     * @param defaultCount the default count
     * @return resolved count
     */
    public static int resolveSearchCount(Integer count, int defaultCount) {
        if (count == null) {
            return defaultCount;
        }
        return Math.max(1, Math.min(count, MAX_SEARCH_COUNT));
    }

    /**
     * Resolve search timeout from config.
     *
     * @param searchConfig the search config
     * @return timeout in seconds
     */
    public static int resolveSearchTimeoutSeconds(Map<String, Object> searchConfig) {
        if (searchConfig == null) {
            return DEFAULT_TIMEOUT_SECONDS;
        }
        Object timeout = searchConfig.get("timeoutSeconds");
        if (timeout instanceof Number) {
            return ((Number) timeout).intValue();
        }
        return DEFAULT_TIMEOUT_SECONDS;
    }

    /**
     * Resolve cache TTL from config.
     *
     * @param searchConfig the search config
     * @return TTL in milliseconds
     */
    public static long resolveSearchCacheTtlMs(Map<String, Object> searchConfig) {
        if (searchConfig == null) {
            return DEFAULT_CACHE_TTL_MINUTES * 60 * 1000L;
        }
        Object ttl = searchConfig.get("cacheTtlMinutes");
        if (ttl instanceof Number) {
            return ((Number) ttl).intValue() * 60 * 1000L;
        }
        return DEFAULT_CACHE_TTL_MINUTES * 60 * 1000L;
    }

    /**
     * Extract site name from URL.
     *
     * @param url the URL
     * @return site name or null
     */
    public static String resolveSiteName(String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }
        try {
            java.net.URL parsed = new java.net.URL(url);
            String host = parsed.getHost();
            // Remove www. prefix
            if (host.startsWith("www.")) {
                host = host.substring(4);
            }
            return host;
        } catch (java.net.MalformedURLException e) {
            return null;
        }
    }

    /**
     * Validate language code (ISO 639-1).
     *
     * @param language the language code
     * @return true if valid
     */
    public static boolean isValidLanguageCode(String language) {
        if (language == null || language.isEmpty()) {
            return false;
        }
        return language.matches("^[a-zA-Z]{2}$");
    }

    /**
     * Validate country code (ISO 3166-1 alpha-2).
     *
     * @param country the country code
     * @return true if valid
     */
    public static boolean isValidCountryCode(String country) {
        if (country == null || country.isEmpty()) {
            return false;
        }
        return country.matches("^[a-zA-Z]{2}$");
    }

    /**
     * Wrap web content with safety markers.
     *
     * @param content the content
     * @param source the source type
     * @return wrapped content
     */
    public static String wrapWebContent(String content, String source) {
        if (content == null || content.isEmpty()) {
            return "";
        }
        // Add markers to indicate external/untrusted content
        return String.format("<!-- %s -->%s<!-- /%s -->", source, content, source);
    }
}