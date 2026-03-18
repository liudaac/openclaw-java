package openclaw.plugin.sdk.websearch.utils;

import openclaw.plugin.sdk.websearch.WebSearchProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;

/**
 * Credential utilities for web search providers.
 *
 * @author OpenClaw Team
 * @version 2026.3.18
 */
public final class CredentialUtils {

    private static final Logger logger = LoggerFactory.getLogger(CredentialUtils.class);

    private CredentialUtils() {
        // Utility class
    }

    /**
     * Read configured secret string.
     *
     * @param value the value
     * @param path the config path (for logging)
     * @return the string value or null
     */
    public static String readConfiguredSecretString(Object value, String path) {
        if (value == null) {
            return null;
        }
        String str = value.toString().trim();
        return str.isEmpty() ? null : str;
    }

    /**
     * Read provider environment variable value.
     *
     * @param envVars array of environment variable names to try
     * @return the value or null
     */
    public static String readProviderEnvValue(String[] envVars) {
        if (envVars == null) {
            return null;
        }
        for (String envVar : envVars) {
            String value = System.getenv(envVar);
            if (value != null && !value.trim().isEmpty()) {
                logger.debug("Found credential in environment variable: {}", envVar);
                return value.trim();
            }
        }
        return null;
    }

    /**
     * Read provider environment variable value (single var).
     *
     * @param envVar the environment variable name
     * @return the value or null
     */
    public static String readProviderEnvValue(String envVar) {
        return readProviderEnvValue(new String[]{envVar});
    }

    /**
     * Resolve provider web search plugin config.
     *
     * @param config the OpenClaw config
     * @param providerId the provider ID
     * @return the config map or null
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> resolveProviderWebSearchPluginConfig(
            WebSearchProvider.OpenClawConfig config,
            String providerId) {
        // This is a placeholder - actual implementation would navigate the config structure
        // For now, return null and let implementations handle their own config resolution
        return null;
    }

    /**
     * Set provider web search plugin config value.
     *
     * @param configTarget the target config
     * @param providerId the provider ID
     * @param key the key
     * @param value the value
     */
    public static void setProviderWebSearchPluginValue(
            WebSearchProvider.OpenClawConfig configTarget,
            String providerId,
            String key,
            Object value) {
        // Placeholder implementation
        logger.debug("Setting config value for provider {}: {} = {}", providerId, key, value);
    }

    /**
     * Get scoped credential value.
     *
     * @param searchConfig the search config
     * @param providerId the provider ID
     * @param key the key
     * @return optional value
     */
    public static Optional<Object> getScopedCredentialValue(
            Map<String, Object> searchConfig,
            String providerId,
            String key) {
        if (searchConfig == null) {
            return Optional.empty();
        }

        // Try provider-specific config first
        Object providerConfig = searchConfig.get(providerId);
        if (providerConfig instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> providerMap = (Map<String, Object>) providerConfig;
            Object value = providerMap.get(key);
            if (value != null) {
                return Optional.of(value);
            }
        }

        // Fall back to top-level key
        return Optional.ofNullable(searchConfig.get(key));
    }

    /**
     * Set scoped credential value.
     *
     * @param searchConfigTarget the target config
     * @param providerId the provider ID
     * @param key the key
     * @param value the value
     */
    @SuppressWarnings("unchecked")
    public static void setScopedCredentialValue(
            Map<String, Object> searchConfigTarget,
            String providerId,
            String key,
            Object value) {
        Object providerConfig = searchConfigTarget.get(providerId);
        if (!(providerConfig instanceof Map)) {
            providerConfig = new java.util.HashMap<String, Object>();
            searchConfigTarget.put(providerId, providerConfig);
        }
        ((Map<String, Object>) providerConfig).put(key, value);
    }

    /**
     * Get top-level credential value.
     *
     * @param searchConfig the search config
     * @param key the key
     * @return optional value
     */
    public static Optional<Object> getTopLevelCredentialValue(
            Map<String, Object> searchConfig,
            String key) {
        if (searchConfig == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(searchConfig.get(key));
    }

    /**
     * Set top-level credential value.
     *
     * @param searchConfigTarget the target config
     * @param key the key
     * @param value the value
     */
    public static void setTopLevelCredentialValue(
            Map<String, Object> searchConfigTarget,
            String key,
            Object value) {
        searchConfigTarget.put(key, value);
    }

    /**
     * Mask credential for logging.
     *
     * @param credential the credential
     * @return masked string
     */
    public static String maskCredential(String credential) {
        if (credential == null || credential.length() < 8) {
            return "***";
        }
        return credential.substring(0, 4) + "..." + credential.substring(credential.length() - 4);
    }

    /**
     * Check if credential looks like an API key.
     *
     * @param credential the credential
     * @param prefixes expected prefixes
     * @return true if matches
     */
    public static boolean hasApiKeyPrefix(String credential, String... prefixes) {
        if (credential == null || prefixes == null) {
            return false;
        }
        String lower = credential.toLowerCase();
        for (String prefix : prefixes) {
            if (lower.startsWith(prefix.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
}
