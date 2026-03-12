package openclaw.sdk.core;

import openclaw.sdk.channel.ChannelRuntime;

import java.util.concurrent.CompletableFuture;

/**
 * Main plugin runtime interface providing access to subagent execution,
 * channel operations, and core services.
 *
 * <p>This is the Java equivalent of Node.js PluginRuntime type.</p>
 *
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public interface PluginRuntime {

    /**
     * Gets the subagent runtime for spawning and managing child sessions.
     *
     * @return the subagent runtime
     */
    SubagentRuntime subagent();

    /**
     * Gets the channel runtime for messaging operations.
     *
     * @return the channel runtime
     */
    ChannelRuntime channel();

    /**
     * Gets the core runtime for basic services.
     *
     * @return the core runtime
     */
    CoreRuntime core();

    /**
     * Initializes the plugin runtime with the given context.
     *
     * @param context the plugin context
     */
    void initialize(PluginContext context);

    /**
     * Shuts down the plugin runtime gracefully.
     */
    void shutdown();

    /**
     * Performs a health check on the runtime.
     *
     * @return the health status
     */
    CompletableFuture<HealthStatus> health();
}
