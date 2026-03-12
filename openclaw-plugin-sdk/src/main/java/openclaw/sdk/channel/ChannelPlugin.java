package openclaw.sdk.channel;

import java.util.Optional;

/**
 * Channel plugin interface - the main contract for channel implementations.
 *
 * <p>This is the Java equivalent of Node.js ChannelPlugin type.</p>
 *
 * @param <ResolvedAccount> the type of resolved account
 * @param <Probe> the type of probe result
 * @param <Audit> the type of audit info
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public interface ChannelPlugin<ResolvedAccount, Probe, Audit> {

    /**
     * Gets the channel ID.
     *
     * @return the channel ID
     */
    ChannelId getId();

    /**
     * Gets the channel metadata.
     *
     * @return the metadata
     */
    ChannelMeta getMeta();

    /**
     * Gets the channel capabilities.
     *
     * @return the capabilities
     */
    ChannelCapabilities getCapabilities();

    /**
     * Gets the configuration adapter (required).
     *
     * @return the config adapter
     */
    ChannelConfigAdapter<ResolvedAccount> getConfigAdapter();

    /**
     * Gets the onboarding adapter (optional).
     *
     * @return the onboarding adapter if available
     */
    default Optional<ChannelOnboardingAdapter> getOnboardingAdapter() {
        return Optional.empty();
    }

    /**
     * Gets the setup adapter (optional).
     *
     * @return the setup adapter if available
     */
    default Optional<ChannelSetupAdapter> getSetupAdapter() {
        return Optional.empty();
    }

    /**
     * Gets the pairing adapter (optional).
     *
     * @return the pairing adapter if available
     */
    default Optional<ChannelPairingAdapter> getPairingAdapter() {
        return Optional.empty();
    }

    /**
     * Gets the security adapter (optional).
     *
     * @return the security adapter if available
     */
    default Optional<ChannelSecurityAdapter<ResolvedAccount>> getSecurityAdapter() {
        return Optional.empty();
    }

    /**
     * Gets the group adapter (optional).
     *
     * @return the group adapter if available
     */
    default Optional<ChannelGroupAdapter> getGroupAdapter() {
        return Optional.empty();
    }

    /**
     * Gets the mention adapter (optional).
     *
     * @return the mention adapter if available
     */
    default Optional<ChannelMentionAdapter> getMentionAdapter() {
        return Optional.empty();
    }

    /**
     * Gets the outbound adapter (optional).
     *
     * @return the outbound adapter if available
     */
    default Optional<ChannelOutboundAdapter> getOutboundAdapter() {
        return Optional.empty();
    }

    /**
     * Gets the inbound adapter (optional).
     *
     * @return the inbound adapter if available
     */
    default Optional<ChannelInboundAdapter> getInboundAdapter() {
        return Optional.empty();
    }

    /**
     * Gets the status adapter (optional).
     *
     * @return the status adapter if available
     */
    default Optional<ChannelStatusAdapter<ResolvedAccount, Probe, Audit>> getStatusAdapter() {
        return Optional.empty();
    }

    /**
     * Gets the gateway adapter (optional).
     *
     * @return the gateway adapter if available
     */
    default Optional<ChannelGatewayAdapter<ResolvedAccount>> getGatewayAdapter() {
        return Optional.empty();
    }

    /**
     * Gets the auth adapter (optional).
     *
     * @return the auth adapter if available
     */
    default Optional<ChannelAuthAdapter> getAuthAdapter() {
        return Optional.empty();
    }

    /**
     * Gets the elevated adapter (optional).
     *
     * @return the elevated adapter if available
     */
    default Optional<ChannelElevatedAdapter> getElevatedAdapter() {
        return Optional.empty();
    }

    /**
     * Gets the command adapter (optional).
     *
     * @return the command adapter if available
     */
    default Optional<ChannelCommandAdapter> getCommandAdapter() {
        return Optional.empty();
    }

    /**
     * Gets the streaming adapter (optional).
     *
     * @return the streaming adapter if available
     */
    default Optional<ChannelStreamingAdapter> getStreamingAdapter() {
        return Optional.empty();
    }

    /**
     * Gets the threading adapter (optional).
     *
     * @return the threading adapter if available
     */
    default Optional<ChannelThreadingAdapter> getThreadingAdapter() {
        return Optional.empty();
    }

    /**
     * Gets the messaging adapter (optional).
     *
     * @return the messaging adapter if available
     */
    default Optional<ChannelMessagingAdapter> getMessagingAdapter() {
        return Optional.empty();
    }

    /**
     * Gets the agent prompt adapter (optional).
     *
     * @return the agent prompt adapter if available
     */
    default Optional<ChannelAgentPromptAdapter> getAgentPromptAdapter() {
        return Optional.empty();
    }

    /**
     * Gets the directory adapter (optional).
     *
     * @return the directory adapter if available
     */
    default Optional<ChannelDirectoryAdapter> getDirectoryAdapter() {
        return Optional.empty();
    }

    /**
     * Gets the resolver adapter (optional).
     *
     * @return the resolver adapter if available
     */
    default Optional<ChannelResolverAdapter> getResolverAdapter() {
        return Optional.empty();
    }

    /**
     * Gets the message action adapter (optional).
     *
     * @return the message action adapter if available
     */
    default Optional<ChannelMessageActionAdapter> getMessageActionAdapter() {
        return Optional.empty();
    }

    /**
     * Gets the heartbeat adapter (optional).
     *
     * @return the heartbeat adapter if available
     */
    default Optional<ChannelHeartbeatAdapter> getHeartbeatAdapter() {
        return Optional.empty();
    }
}
