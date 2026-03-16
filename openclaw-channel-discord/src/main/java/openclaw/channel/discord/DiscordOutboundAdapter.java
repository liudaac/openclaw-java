package openclaw.channel.discord;

import openclaw.sdk.channel.ChannelOutboundAdapter;
import openclaw.sdk.channel.SendMessageRequest;
import openclaw.sdk.channel.SendResult;

import java.util.concurrent.CompletableFuture;

/**
 * Discord outbound adapter.
 */
public class DiscordOutboundAdapter implements ChannelOutboundAdapter {

    @Override
    public CompletableFuture<SendResult> sendMessage(Object account, SendMessageRequest request) {
        return CompletableFuture.completedFuture(
                SendResult.success("msg_" + System.currentTimeMillis())
        );
    }

    @Override
    public CompletableFuture<Void> sendTyping(Object account, String chatId) {
        return CompletableFuture.completedFuture(null);
    }
}
