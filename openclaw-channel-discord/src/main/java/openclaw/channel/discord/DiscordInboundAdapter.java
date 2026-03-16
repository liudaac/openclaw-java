package openclaw.channel.discord;

import openclaw.sdk.channel.ChannelInboundAdapter;
import openclaw.sdk.channel.ChannelMessage;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Discord inbound adapter.
 */
public class DiscordInboundAdapter implements ChannelInboundAdapter {

    private final CopyOnWriteArrayList<Consumer<ChannelMessage>> messageHandlers = new CopyOnWriteArrayList<>();

    @Override
    public CompletableFuture<ChannelInboundAdapter.ProcessResult> onMessage(ChannelMessage message) {
        return CompletableFuture.supplyAsync(() -> {
            for (Consumer<ChannelMessage> handler : messageHandlers) {
                try {
                    handler.accept(message);
                } catch (Exception e) {
                    // Log error
                }
            }
            return ChannelInboundAdapter.ProcessResult.success();
        });
    }

    @Override
    public void onMessage(Consumer<ChannelMessage> handler) {
        messageHandlers.add(handler);
    }

    @Override
    public void removeHandler(Consumer<ChannelMessage> handler) {
        messageHandlers.remove(handler);
    }
}
