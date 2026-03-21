package openclaw.channel.wecom;

import openclaw.sdk.channel.*;

import java.util.concurrent.CompletableFuture;

/**
 * WeCom outbound message adapter.
 *
 * @author OpenClaw Team
 * @version 2026.3.21
 * @since 2026.3.21
 */
public class WecomOutboundAdapter implements ChannelOutboundAdapter {

    @Override
    public CompletableFuture<ChannelMessage> sendText(ChannelOutboundRequest request) {
        // TODO: Implement WeCom API call
        return CompletableFuture.completedFuture(
                ChannelMessage.builder()
                        .id("wecom-msg-" + System.currentTimeMillis())
                        .text(request.getText())
                        .build()
        );
    }

    @Override
    public CompletableFuture<ChannelMessage> sendImage(ChannelOutboundRequest request) {
        // TODO: Implement WeCom API call
        return CompletableFuture.completedFuture(
                ChannelMessage.builder()
                        .id("wecom-img-" + System.currentTimeMillis())
                        .build()
        );
    }

    @Override
    public CompletableFuture<ChannelMessage> sendFile(ChannelOutboundRequest request) {
        // TODO: Implement WeCom API call
        return CompletableFuture.completedFuture(
                ChannelMessage.builder()
                        .id("wecom-file-" + System.currentTimeMillis())
                        .build()
        );
    }
}
