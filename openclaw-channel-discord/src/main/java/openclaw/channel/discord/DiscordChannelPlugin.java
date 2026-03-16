package openclaw.channel.discord;

import openclaw.sdk.channel.*;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Discord channel plugin implementation.
 */
public class DiscordChannelPlugin implements ChannelPlugin<DiscordChannelPlugin.DiscordAccount, Void, Void> {

    @Override
    public ChannelId getId() {
        return ChannelId.DISCORD;
    }

    @Override
    public ChannelMeta getMeta() {
        return ChannelMeta.builder()
                .name("Discord")
                .description("Discord messaging platform")
                .icon("discord")
                .color("#5865F2")
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
    public ChannelConfigAdapter<DiscordAccount> getConfigAdapter() {
        return new DiscordConfigAdapter();
    }

    @Override
    public Optional<ChannelOutboundAdapter> getOutboundAdapter() {
        return Optional.of(new DiscordOutboundAdapter());
    }

    @Override
    public Optional<ChannelInboundAdapter> getInboundAdapter() {
        return Optional.empty();
    }

    @Override
    public Optional<ChannelMentionAdapter> getMentionAdapter() {
        return Optional.of(new DiscordMentionAdapter());
    }

    @Override
    public Optional<ChannelDirectoryAdapter> getDirectoryAdapter() {
        return Optional.empty();
    }

    /**
     * Discord account.
     */
    public record DiscordAccount(
            String botToken,
            String botUsername,
            Optional<String> webhookUrl
    ) {
    }
}
