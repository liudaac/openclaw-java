package openclaw.channel.feishu;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Resolver for Feishu tool accounts with synthetic account fallback.
 *
 * <p>This class implements the tool account resolution logic that mirrors
 * the TypeScript implementation in extensions/feishu/src/tool-account.ts.
 * It handles the fallback from synthetic tool accounts (e.g., "agent-spawner")
 * to configured default accounts or the first available enabled account.</p>
 *
 * <p>Key behaviors:
 * <ul>
 *   <li>Explicit accountId takes precedence</li>
 *   <li>Configured defaultAccount is second choice</li>
 *   <li>Contextual accountId is used only if it's a real configured account</li>
 *   <li>Synthetic accounts (not in config) fall back to default/first account</li>
 * </ul>
 * </p>
 *
 * @author OpenClaw Team
 * @version 2026.3.28
 * @since 2026.3.28
 */
public class FeishuToolAccountResolver {

    private static final Logger logger = LoggerFactory.getLogger(FeishuToolAccountResolver.class);

    private final Map<String, Object> config;

    public FeishuToolAccountResolver(Map<String, Object> config) {
        this.config = config != null ? config : Map.of();
    }

    /**
     * Resolves the implicit tool account ID with synthetic account fallback.
     *
     * <p>This method implements the core logic from resolveImplicitToolAccountId in
     * the TypeScript implementation. It validates that contextual accounts are
     * real configured accounts before using them.</p>
     *
     * @param explicitAccountId the explicitly provided account ID (from execute params)
     * @param contextualAccountId the contextual account ID (from agent context, e.g., agentAccountId)
     * @return the resolved account ID, or empty if no valid account found
     */
    public Optional<String> resolveImplicitToolAccountId(
            String explicitAccountId,
            String contextualAccountId) {

        // 1. Explicit accountId takes precedence
        String normalizedExplicit = normalizeOptionalAccountId(explicitAccountId);
        if (normalizedExplicit != null) {
            logger.debug("Using explicit accountId: {}", normalizedExplicit);
            return Optional.of(normalizedExplicit);
        }

        // 2. Configured defaultAccount is second choice
        String configuredDefault = readConfiguredDefaultAccountId();
        if (configuredDefault != null) {
            logger.debug("Using configured defaultAccount: {}", configuredDefault);
            return Optional.of(configuredDefault);
        }

        // 3. Contextual accountId is used only if it's a real configured account
        String normalizedContextual = normalizeOptionalAccountId(contextualAccountId);
        if (normalizedContextual == null) {
            logger.debug("No contextual accountId provided");
            return Optional.empty();
        }

        // Validate that the contextual account is a real configured account
        // This prevents synthetic accounts (like "agent-spawner") from being used
        Set<String> configuredAccountIds = listFeishuAccountIds();
        if (!configuredAccountIds.contains(normalizedContextual)) {
            logger.debug("Contextual accountId '{}' is not a configured account, falling back", normalizedContextual);
            return Optional.empty();
        }

        // Check if the account is enabled
        if (!isAccountEnabled(normalizedContextual)) {
            logger.debug("Contextual accountId '{}' is disabled, falling back", normalizedContextual);
            return Optional.empty();
        }

        logger.debug("Using validated contextual accountId: {}", normalizedContextual);
        return Optional.of(normalizedContextual);
    }

    /**
     * Resolves the tool account with full fallback chain.
     *
     * @param explicitAccountId the explicitly provided account ID
     * @param contextualAccountId the contextual account ID (may be synthetic)
     * @return the resolved account ID, or empty if no valid account
     */
    public Optional<String> resolveToolAccount(
            String explicitAccountId,
            String contextualAccountId) {
        return resolveImplicitToolAccountId(explicitAccountId, contextualAccountId);
    }

    /**
     * Normalizes an optional account ID.
     *
     * @param value the account ID value
     * @return the normalized account ID, or null if empty/blank
     */
    private String normalizeOptionalAccountId(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * Reads the configured default account ID from config.
     *
     * @return the default account ID, or null if not configured
     */
    @SuppressWarnings("unchecked")
    private String readConfiguredDefaultAccountId() {
        Object channels = config.get("channels");
        if (!(channels instanceof Map)) {
            return null;
        }

        Object feishu = ((Map<String, Object>) channels).get("feishu");
        if (!(feishu instanceof Map)) {
            return null;
        }

        Object defaultAccount = ((Map<String, Object>) feishu).get("defaultAccount");
        if (!(defaultAccount instanceof String)) {
            return null;
        }

        return normalizeOptionalAccountId((String) defaultAccount);
    }

    /**
     * Lists all configured Feishu account IDs.
     *
     * @return set of configured account IDs
     */
    @SuppressWarnings("unchecked")
    private Set<String> listFeishuAccountIds() {
        Object channels = config.get("channels");
        if (!(channels instanceof Map)) {
            return Set.of();
        }

        Object feishu = ((Map<String, Object>) channels).get("feishu");
        if (!(feishu instanceof Map)) {
            return Set.of();
        }

        Object accounts = ((Map<String, Object>) feishu).get("accounts");
        if (!(accounts instanceof Map)) {
            return Set.of();
        }

        return ((Map<String, Object>) accounts).keySet();
    }

    /**
     * Checks if an account is enabled.
     *
     * @param accountId the account ID
     * @return true if enabled
     */
    @SuppressWarnings("unchecked")
    private boolean isAccountEnabled(String accountId) {
        Object channels = config.get("channels");
        if (!(channels instanceof Map)) {
            return false;
        }

        Object feishu = ((Map<String, Object>) channels).get("feishu");
        if (!(feishu instanceof Map)) {
            return false;
        }

        Object accounts = ((Map<String, Object>) feishu).get("accounts");
        if (!(accounts instanceof Map)) {
            return false;
        }

        Object account = ((Map<String, Object>) accounts).get(accountId);
        if (!(account instanceof Map)) {
            return false;
        }

        Object enabled = ((Map<String, Object>) account).get("enabled");
        if (enabled instanceof Boolean) {
            return (Boolean) enabled;
        }

        // Default to true if not explicitly disabled
        return true;
    }

    /**
     * Gets the first enabled account ID.
     *
     * @return the first enabled account ID, or empty if none found
     */
    public Optional<String> getFirstEnabledAccountId() {
        Set<String> accountIds = listFeishuAccountIds();
        for (String accountId : accountIds) {
            if (isAccountEnabled(accountId)) {
                return Optional.of(accountId);
            }
        }
        return Optional.empty();
    }
}
