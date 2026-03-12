package openclaw.channel.discord;

import openclaw.sdk.channel.*;
import openclaw.sdk.channel.ChannelCapabilities.Builder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Discord Channel Plugin - High Priority Improvement
 *
 * <p>Discord channel integration using JDA (Java Discord API).</p>
 */
public class DiscordChannelPlugin implements ChannelPlugin<DiscordChannelPlugin.DiscordAccount, DiscordChannelPlugin.DiscordConfig, Void> {

    private static final Logger logger = LoggerFactory.getLogger(DiscordChannelPlugin.class);

    private DiscordAccount account;
    private DiscordConfig config;
    private DiscordOutboundAdapter outboundAdapter;
    private DiscordInboundAdapter inboundAdapter;
    private DiscordMentionAdapter mentionAdapter;
    private DiscordDirectoryAdapter directoryAdapter;

    public DiscordChannelPlugin() {
        this.outboundAdapter = new DiscordOutboundAdapter();
        this.inboundAdapter = new DiscordInboundAdapter();
        this.mentionAdapter = new DiscordMentionAdapter();
        this.directoryAdapter = new DiscordDirectoryAdapter();
    }

    @Override
    public ChannelId getId() {
        return ChannelId.DISCORD;
    }

    @Override
    public ChannelMeta getMeta() {
        return new ChannelMeta(
                "Discord",
                "Discord messaging platform",
                "https://discord.com",
                Map.of(
                        "features", "text,voice,threads,roles",
                        "rate_limit", "5/5s"
                )
        );
    }

    @Override
    public ChannelCapabilities getCapabilities() {
        return new Builder()
                .supportsOutbound(true)
                .supportsInbound(true)
                .supportsMentions(true)
                .supportsThreads(true)
                .supportsVoice(false)
                .supportsFiles(true)
                .maxMessageLength(2000)
                .supportedFormats(new String[]{"text", "markdown", "embed"})
                .build();
    }

    @Override
    public CompletableFuture<ChannelInitializeResult<Void>> initialize(DiscordAccount account, DiscordConfig config) {
        this.account = account;
        this.config = config;

        logger.info("Initializing Discord channel with bot: {}", account.botName());

        return CompletableFuture.supplyAsync(() -> {
            try {
                // Initialize JDA client
                outboundAdapter.initialize(account);
                inboundAdapter.initialize(account, outboundAdapter);

                logger.info("Discord channel initialized successfully");
                return ChannelInitializeResult.success(null);

            } catch (Exception e) {
                logger.error("Failed to initialize Discord channel: {}", e.getMessage(), e);
                return ChannelInitializeResult.failure(e.getMessage());
            }
        });
    }

    @Override
    public CompletableFuture<Void> shutdown() {
        logger.info("Shutting down Discord channel");

        return CompletableFuture.runAsync(() -> {
            try {
                if (outboundAdapter != null) {
                    outboundAdapter.shutdown();
                }
                if (inboundAdapter != null) {
                    inboundAdapter.shutdown();
                }
                logger.info("Discord channel shutdown complete");
            } catch (Exception e) {
                logger.error("Error during shutdown: {}", e.getMessage());
            }
        });
    }

    @Override
    public Optional<ChannelOutboundAdapter> getOutboundAdapter() {
        return Optional.of(outboundAdapter);
    }

    @Override
    public Optional<ChannelInboundAdapter> getInboundAdapter() {
        return Optional.of(inboundAdapter);
    }

    @Override
    public Optional<ChannelMentionAdapter> getMentionAdapter() {
        return Optional.of(mentionAdapter);
    }

    @Override
    public Optional<ChannelDirectoryAdapter> getDirectoryAdapter() {
        return Optional.of(directoryAdapter);
    }

    /**
     * Discord account configuration
     */
    public record DiscordAccount(
            String botToken,
            String botName,
            String applicationId,
            Optional<String> webhookUrl
    ) {
        public DiscordAccount(String botToken, String botName) {
            this(botToken, botName, "", Optional.empty());
        }
    }

    /**
     * Discord configuration
     */
    public record DiscordConfig(
            String defaultGuildId,
            String defaultChannelId,
            boolean enableDMs,
            boolean enableThreads,
            int maxShards,
            Map<String, Object> options
    ) {
        public DiscordConfig() {
            this("", "", true, true, 1, Map.of());
        }
    }
}
