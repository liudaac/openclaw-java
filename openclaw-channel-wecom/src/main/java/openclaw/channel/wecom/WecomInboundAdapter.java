package openclaw.channel.wecom;

import openclaw.sdk.channel.*;

import java.util.concurrent.CompletableFuture;

/**
 * WeCom inbound message adapter.
 *
 * @author OpenClaw Team
 * @version 2026.3.21
 * @since 2026.3.21
 */
public class WecomInboundAdapter implements ChannelInboundAdapter {

    @Override
    public CompletableFuture<ChannelInboundResult> handleWebhook(ChannelWebhookPayload payload) {
        // TODO: Implement WeCom webhook handling
        return CompletableFuture.completedFuture(
                ChannelInboundResult.builder()
                        .handled(false)
                        .build()
        );
    }

    @Override
    public CompletableFuture<ChannelInboundResult> handlePoll(ChannelPollRequest request) {
        // TODO: Implement WeCom polling
        return CompletableFuture.completedFuture(
                ChannelInboundResult.builder()
                        .handled(false)
                        .build()
        );
    }
}
