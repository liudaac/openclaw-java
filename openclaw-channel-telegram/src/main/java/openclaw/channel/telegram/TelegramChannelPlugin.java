package openclaw.channel.telegram;

import openclaw.sdk.channel.*;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Telegram channel plugin implementation.
 *
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public class TelegramChannelPlugin implements ChannelPlugin<TelegramAccount, Void, Void> {

    private TelegramConfigAdapter configAdapter;
    private TelegramOutboundAdapter outboundAdapter;
    private TelegramConfigAdapter telegramConfigAdapter;

    @Override
    public ChannelId getId() {
        return ChannelId.TELEGRAM;
    }

    @Override
    public ChannelMeta getMeta() {
        return ChannelMeta.builder()
                .name("Telegram")
                .description("Telegram messaging platform")
                .icon("telegram")
                .color("#0088cc")
                .docsUrl("https://docs.openclaw.ai/channels/telegram")
                .tags(List.of("messaging", "bot", "popular"))
                .build();
    }

    @Override
    public ChannelCapabilities getCapabilities() {
        return ChannelCapabilities.builder()
                .supportsText(true)
                .supportsImages(true)
                .supportsFiles(true)
                .supportsVoice(true)
                .supportsVideo(true)
                .supportsTyping(true)
                .supportsReactions(true)
                .supportsThreads(true)
                .supportsEditing(true)
                .supportsDeleting(true)
                .supportsMarkdown(true)
                .supportsMentions(true)
                .supportsGroups(true)
                .supportsDMs(true)
                .supportsPolling(true)
                .build();
    }

    @Override
    public ChannelConfigAdapter<TelegramAccount> getConfigAdapter() {
        if (configAdapter == null) {
            configAdapter = new TelegramConfigAdapter();
        }
        return configAdapter;
    }

    @Override
    public Optional<ChannelOutboundAdapter> getOutboundAdapter() {
        if (outboundAdapter == null) {
            outboundAdapter = new TelegramOutboundAdapter();
        }
        return Optional.of(outboundAdapter);
    }

    @Override
    public Optional<ChannelGroupAdapter> getGroupAdapter() {
        return Optional.of(new TelegramGroupAdapter());
    }

    @Override
    public Optional<ChannelSecurityAdapter<TelegramAccount>> getSecurityAdapter() {
        return Optional.of(new TelegramSecurityAdapter());
    }

    @Override
    public Optional<ChannelCommandAdapter> getCommandAdapter() {
        return Optional.of(new TelegramCommandAdapter());
    }

    @Override
    public Optional<ChannelMentionAdapter> getMentionAdapter() {
        return Optional.of(new TelegramMentionAdapter());
    }

    @Override
    public Optional<ChannelInboundAdapter> getInboundAdapter() {
        return Optional.of(new TelegramInboundAdapter(
                new TelegramAccount("", "", Optional.empty(), "https://api.telegram.org"),
                outboundAdapter != null ? outboundAdapter : new TelegramOutboundAdapter()
        ));
    }

    /**
     * Telegram account.
     *
     * @param botToken the bot token
     * @param botUsername the bot username
     * @param webhookUrl the webhook URL
     * @param apiUrl the API URL
     */
    public record TelegramAccount(
            String botToken,
            String botUsername,
            Optional<String> webhookUrl,
            String apiUrl
    ) {
    }
}
