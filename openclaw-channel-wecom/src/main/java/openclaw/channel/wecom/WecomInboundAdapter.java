package openclaw.channel.wecom;

import openclaw.sdk.channel.ChannelInboundAdapter;
import openclaw.sdk.channel.ChannelMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * WeCom channel inbound adapter.
 *
 * @author OpenClaw Team
 * @version 2026.3.30
 */
public class WecomInboundAdapter implements ChannelInboundAdapter {

    private static final Logger logger = LoggerFactory.getLogger(WecomInboundAdapter.class);

    private Consumer<ChannelMessage> messageHandler;

    @Override
    public CompletableFuture<ProcessResult> onMessage(ChannelMessage message) {
        return CompletableFuture.supplyAsync(() -> {
            logger.info("Received message from WeCom: {}", message.text());
            if (messageHandler != null) {
                messageHandler.accept(message);
                return ProcessResult.success();
            }
            return ProcessResult.failure("No message handler registered");
        });
    }

    @Override
    public void onMessage(Consumer<ChannelMessage> handler) {
        this.messageHandler = handler;
    }

    @Override
    public void removeHandler(Consumer<ChannelMessage> handler) {
        if (this.messageHandler == handler) {
            this.messageHandler = null;
        }
    }
}
