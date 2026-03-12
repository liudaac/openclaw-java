package openclaw.sdk.core;

import java.util.Optional;

/**
 * Plugin logger interface for structured logging.
 *
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public interface PluginLogger {

    /**
     * Logs a debug message.
     *
     * @param message the message
     */
    default void debug(String message) {
        // Default: no-op
    }

    /**
     * Logs an info message.
     *
     * @param message the message
     */
    void info(String message);

    /**
     * Logs a warning message.
     *
     * @param message the message
     */
    void warn(String message);

    /**
     * Logs an error message.
     *
     * @param message the message
     */
    void error(String message);

    /**
     * Logs an error with exception.
     *
     * @param message the message
     * @param throwable the exception
     */
    default void error(String message, Throwable throwable) {
        error(message + ": " + Optional.ofNullable(throwable)
                .map(Throwable::getMessage)
                .orElse("unknown"));
    }

    /**
     * Creates a logger with a specific prefix.
     *
     * @param prefix the prefix
     * @return a prefixed logger
     */
    default PluginLogger withPrefix(String prefix) {
        return new PrefixedPluginLogger(this, prefix);
    }

    /**
     * Internal prefixed logger implementation.
     */
    class PrefixedPluginLogger implements PluginLogger {
        private final PluginLogger delegate;
        private final String prefix;

        PrefixedPluginLogger(PluginLogger delegate, String prefix) {
            this.delegate = delegate;
            this.prefix = prefix;
        }

        @Override
        public void debug(String message) {
            delegate.debug("[" + prefix + "] " + message);
        }

        @Override
        public void info(String message) {
            delegate.info("[" + prefix + "] " + message);
        }

        @Override
        public void warn(String message) {
            delegate.warn("[" + prefix + "] " + message);
        }

        @Override
        public void error(String message) {
            delegate.error("[" + prefix + "] " + message);
        }
    }
}
