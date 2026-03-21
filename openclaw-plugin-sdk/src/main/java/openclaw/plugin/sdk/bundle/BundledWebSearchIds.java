package openclaw.plugin.sdk.bundle;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Constants and utilities for bundled Web Search provider IDs.
 *
 * <p>This class defines the set of Web Search providers that are bundled
 * with OpenClaw and provides utility methods for working with them.</p>
 *
 * @author OpenClaw Team
 * @version 2026.3.21
 * @since 2026.3.21
 */
public final class BundledWebSearchIds {

    /**
     * Brave Search provider ID.
     */
    public static final String BRAVE = "brave";

    /**
     * Firecrawl Search provider ID.
     */
    public static final String FIRECRAWL = "firecrawl";

    /**
     * Google/Gemini Search provider ID.
     */
    public static final String GOOGLE = "google";

    /**
     * Moonshot/Kimi Search provider ID.
     */
    public static final String MOONSHOT = "moonshot";

    /**
     * Perplexity Search provider ID.
     */
    public static final String PERPLEXITY = "perplexity";

    /**
     * Tavily Search provider ID.
     */
    public static final String TAVILY = "tavily";

    /**
     * xAI/Grok Search provider ID.
     */
    public static final String XAI = "xai";

    /**
     * Array of all bundled Web Search provider IDs.
     */
    private static final String[] ALL_IDS = {
            BRAVE,
            FIRECRAWL,
            GOOGLE,
            MOONSHOT,
            PERPLEXITY,
            TAVILY,
            XAI
    };

    /**
     * Unmodifiable list of all bundled Web Search provider IDs.
     */
    public static final List<String> BUNDLED_IDS = Collections.unmodifiableList(
            Arrays.asList(ALL_IDS)
    );

    private BundledWebSearchIds() {
        // Utility class, prevent instantiation
    }

    /**
     * Returns a list of all bundled Web Search provider IDs.
     *
     * @return list of bundled provider IDs
     */
    public static List<String> listBundledIds() {
        return BUNDLED_IDS;
    }

    /**
     * Checks if a provider ID is a bundled Web Search provider.
     *
     * @param providerId the provider ID to check
     * @return true if the provider is bundled
     */
    public static boolean isBundled(String providerId) {
        if (providerId == null) {
            return false;
        }
        return BUNDLED_IDS.contains(providerId.toLowerCase());
    }

    /**
     * Gets the count of bundled Web Search providers.
     *
     * @return the count of bundled providers
     */
    public static int getBundledCount() {
        return ALL_IDS.length;
    }
}
