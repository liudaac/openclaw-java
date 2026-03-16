package openclaw.channel.slack;

import openclaw.sdk.channel.*;

import java.util.Optional;

/**
 * Slack channel plugin implementation.
 */
public class SlackChannelPlugin implements ChannelPlugin<SlackChannelPlugin.SlackAccount, Void, Void> {

    @Override
    public ChannelId getId() {
        return ChannelId.SLACK;
    }

    @Override
    public ChannelMeta getMeta() {
        return ChannelMeta.builder()
                .name("Slack")
                .description("Slack messaging platform")
                .build();
    }

    @Override
    public ChannelCapabilities getCapabilities() {
        return ChannelCapabilities.builder()
                .supportsText(true)
                .supportsMarkdown(true)
                .supportsMentions(true)
                .build();
    }

    @Override
    public ChannelConfigAdapter<SlackAccount> getConfigAdapter() {
        return new SlackConfigAdapter();
    }

    @Override
    public Optional<ChannelOutboundAdapter> getOutboundAdapter() {
        return Optional.of(new SlackOutboundAdapter());
    }

    @Override
    public Optional<ChannelInboundAdapter> getInboundAdapter() {
        return Optional.empty();
    }

    @Override
    public Optional<ChannelMentionAdapter> getMentionAdapter() {
        return Optional.of(new SlackMentionAdapter());
    }

    @Override
    public Optional<ChannelDirectoryAdapter> getDirectoryAdapter() {
        return Optional.empty();
    }

    /**
     * Slack account.
     */
    public record SlackAccount(
            String botToken,
            String botUsername,
            Optional<String> webhookUrl
    ) {
    }
}
