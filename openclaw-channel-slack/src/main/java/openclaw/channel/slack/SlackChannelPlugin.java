package openclaw.channel.slack;

import com.slack.api.Slack;
import com.slack.api.methods.MethodsClient;
import openclaw.sdk.channel.*;
import openclaw.sdk.channel.ChannelCapabilities.Builder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Slack Channel Plugin - High Priority Improvement
 *
 * <p>Slack channel integration using Slack API.</p>
 */
public class SlackChannelPlugin implements ChannelPlugin<SlackChannelPlugin.SlackAccount, SlackChannelPlugin.SlackConfig, Void> {

    private static final Logger logger = LoggerFactory.getLogger(SlackChannelPlugin.class);

    private SlackAccount account;
    private SlackConfig config;
    private Slack slack;
    private MethodsClient methods;
    private SlackOutboundAdapter outboundAdapter;
    private SlackInboundAdapter inboundAdapter;
    private SlackMentionAdapter mentionAdapter;
    private SlackDirectoryAdapter directoryAdapter;

    public SlackChannelPlugin() {
        this.slack = Slack.getInstance();
        this.outboundAdapter = new SlackOutboundAdapter();
        this.inboundAdapter = new SlackInboundAdapter();
        this.mentionAdapter = new SlackMentionAdapter();
        this.directoryAdapter = new SlackDirectoryAdapter();
    }

    @Override
    public ChannelId getId() {
        return ChannelId.SLACK;
    }

    @Override
    public ChannelMeta getMeta() {
        return new ChannelMeta(
                "Slack",
                "Slack messaging platform for teams",
                "https://slack.com",
                Map.of(
                        "features", "text,threads,reactions,files,apps",
                        "rate_limit", "1/1s"
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
                .maxMessageLength(4000)
                .supportedFormats(new String[]{"text", "markdown", "blocks"})
                .build();
    }

    @Override
    public CompletableFuture<ChannelInitializeResult<Void>> initialize(SlackAccount account, SlackConfig config) {
        this.account = account;
        this.config = config;

        logger.info("Initializing Slack channel with bot: {}", account.botName());

        return CompletableFuture.supplyAsync(() -> {
            try {
                // Initialize Slack client
                this.methods = slack.methods(account.botToken());

                // Test connection
                var authTest = methods.authTest(r -> r);
                if (!authTest.isOk()) {
                    return ChannelInitializeResult.failure("Auth failed: " + authTest.getError());
                }

                logger.info("Slack bot authenticated: {}", authTest.getUser());

                // Initialize adapters
                outboundAdapter.initialize(account, methods);
                inboundAdapter.initialize(account, config, outboundAdapter);
                directoryAdapter.setMethods(methods);

                logger.info("Slack channel initialized successfully");
                return ChannelInitializeResult.success(null);

            } catch (Exception e) {
                logger.error("Failed to initialize Slack channel: {}", e.getMessage(), e);
                return ChannelInitializeResult.failure(e.getMessage());
            }
        });
    }

    @Override
    public CompletableFuture<Void> shutdown() {
        logger.info("Shutting down Slack channel");

        return CompletableFuture.runAsync(() -> {
            try {
                if (inboundAdapter != null) {
                    inboundAdapter.shutdown();
                }
                logger.info("Slack channel shutdown complete");
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

    public MethodsClient getMethods() {
        return methods;
    }

    /**
     * Slack account configuration
     */
    public record SlackAccount(
            String botToken,
            String signingSecret,
            String botName,
            String botUserId,
            Optional<String> webhookUrl
    ) {
        public SlackAccount(String botToken, String signingSecret) {
            this(botToken, signingSecret, "", "", Optional.empty());
        }
    }

    /**
     * Slack configuration
     */
    public record SlackConfig(
            String defaultChannel,
            boolean enableDMs,
            boolean enableThreads,
            Map<String, Object> options
    ) {
        public SlackConfig() {
            this("", true, true, Map.of());
        }
    }
}
