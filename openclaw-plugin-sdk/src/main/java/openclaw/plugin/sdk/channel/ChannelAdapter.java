package openclaw.plugin.sdk.channel;

/**
 * Base interface for all channel adapters.
 *
 * @author OpenClaw Team
 * @version 2026.3.14
 */
public interface ChannelAdapter {
    
    /**
     * Gets the channel name.
     *
     * @return the channel name
     */
    String getChannelName();
    
    /**
     * Checks if the adapter is available.
     *
     * @return true if available
     */
    boolean isAvailable();
}
