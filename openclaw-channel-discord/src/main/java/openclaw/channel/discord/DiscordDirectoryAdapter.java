package openclaw.channel.discord;

import openclaw.sdk.channel.ChannelDirectoryAdapter;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Discord directory adapter.
 */
public class DiscordDirectoryAdapter implements ChannelDirectoryAdapter {

    @Override
    public CompletableFuture<DirectoryResult> queryDirectory(Object account, DirectoryQuery query) {
        return CompletableFuture.completedFuture(
                new DirectoryResult(List.of(), false, Optional.empty())
        );
    }

    @Override
    public CompletableFuture<Optional<DirectoryEntry>> getEntry(Object account, String entryId) {
        return CompletableFuture.completedFuture(Optional.empty());
    }

    @Override
    public CompletableFuture<Boolean> isAvailable(Object account) {
        return CompletableFuture.completedFuture(true);
    }
}
