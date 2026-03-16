package openclaw.channel.slack;

import openclaw.sdk.channel.ChannelOutboundAdapter;
import openclaw.sdk.channel.SendResult;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Slack outbound adapter.
 */
public class SlackOutboundAdapter implements ChannelOutboundAdapter {

    @Override
    public CompletableFuture<SendResult> sendText(Object account, String to, String message, Optional<SendOptions> options) {
        return CompletableFuture.completedFuture(
                new SendResult(true, Optional.of("msg_" + System.currentTimeMillis()), Optional.empty())
        );
    }

    @Override
    public CompletableFuture<SendResult> sendMedia(Object account, String to, Optional<String> message, String mediaUrl, Optional<SendOptions> options) {
        return CompletableFuture.completedFuture(
                new SendResult(true, Optional.of("msg_" + System.currentTimeMillis()), Optional.empty())
        );
    }

    @Override
    public CompletableFuture<Void> sendTyping(Object account, String to) {
        return CompletableFuture.completedFuture(null);
    }
}
