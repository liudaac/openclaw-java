package openclaw.channel.telegram;

import openclaw.sdk.channel.ChannelConfigAdapter;
import openclaw.sdk.channel.ChannelConversationAdapter;
import openclaw.sdk.channel.ChannelSecurityAdapter;
import openclaw.sdk.config.ConfigStore;
import openclaw.sdk.config.SessionStore;
import openclaw.sdk.web.WebMediaLoader;

import java.util.function.Supplier;

/**
 * Telegram bot dependencies for dependency injection.
 *
 * <p>Enables test isolation and dependency mocking for Telegram bot operations.</p>
 *
 * @author OpenClaw Team
 * @version 2026.3.23
 */
public class TelegramBotDeps {

    private Supplier<ConfigStore> configStoreSupplier;
    private Supplier<SessionStore> sessionStoreSupplier;
    private Supplier<ChannelConfigAdapter> configAdapterSupplier;
    private Supplier<ChannelConversationAdapter> conversationAdapterSupplier;
    private Supplier<ChannelSecurityAdapter> securityAdapterSupplier;
    private Supplier<WebMediaLoader> webMediaLoaderSupplier;

    /**
     * Creates default dependencies.
     */
    public TelegramBotDeps() {
        // Default suppliers will be set by Spring or manually
        this.configStoreSupplier = null;
        this.sessionStoreSupplier = null;
        this.configAdapterSupplier = null;
        this.conversationAdapterSupplier = null;
        this.securityAdapterSupplier = null;
        this.webMediaLoaderSupplier = null;
    }

    // Getters
    public Supplier<ConfigStore> getConfigStoreSupplier() {
        return configStoreSupplier;
    }

    public Supplier<SessionStore> getSessionStoreSupplier() {
        return sessionStoreSupplier;
    }

    public Supplier<ChannelConfigAdapter> getConfigAdapterSupplier() {
        return configAdapterSupplier;
    }

    public Supplier<ChannelConversationAdapter> getConversationAdapterSupplier() {
        return conversationAdapterSupplier;
    }

    public Supplier<ChannelSecurityAdapter> getSecurityAdapterSupplier() {
        return securityAdapterSupplier;
    }

    public Supplier<WebMediaLoader> getWebMediaLoaderSupplier() {
        return webMediaLoaderSupplier;
    }

    // Setters for testing
    public void setConfigStoreSupplier(Supplier<ConfigStore> configStoreSupplier) {
        this.configStoreSupplier = configStoreSupplier;
    }

    public void setSessionStoreSupplier(Supplier<SessionStore> sessionStoreSupplier) {
        this.sessionStoreSupplier = sessionStoreSupplier;
    }

    public void setConfigAdapterSupplier(Supplier<ChannelConfigAdapter> configAdapterSupplier) {
        this.configAdapterSupplier = configAdapterSupplier;
    }

    public void setConversationAdapterSupplier(Supplier<ChannelConversationAdapter> conversationAdapterSupplier) {
        this.conversationAdapterSupplier = conversationAdapterSupplier;
    }

    public void setSecurityAdapterSupplier(Supplier<ChannelSecurityAdapter> securityAdapterSupplier) {
        this.securityAdapterSupplier = securityAdapterSupplier;
    }

    public void setWebMediaLoaderSupplier(Supplier<WebMediaLoader> webMediaLoaderSupplier) {
        this.webMediaLoaderSupplier = webMediaLoaderSupplier;
    }

    /**
     * Resets all dependencies to defaults.
     */
    public void resetToDefaults() {
        this.configStoreSupplier = null;
        this.sessionStoreSupplier = null;
        this.configAdapterSupplier = null;
        this.conversationAdapterSupplier = null;
        this.securityAdapterSupplier = null;
        this.webMediaLoaderSupplier = null;
    }

    /**
     * Testing utilities for dependency injection.
     */
    public static class Testing {
        private static final ThreadLocal<TelegramBotDeps> testDeps = new ThreadLocal<>();

        /**
         * Sets dependencies for tests.
         *
         * @param deps the dependencies to use, or null to reset
         */
        public static void setDepsForTest(TelegramBotDeps deps) {
            if (deps != null) {
                testDeps.set(deps);
            } else {
                testDeps.remove();
            }
        }

        /**
         * Gets test dependencies.
         *
         * @return the test dependencies, or null if not set
         */
        public static TelegramBotDeps getTestDeps() {
            return testDeps.get();
        }

        /**
         * Clears test dependencies.
         */
        public static void clearTestDeps() {
            testDeps.remove();
        }
    }
}
