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
    public CompletableFuture<List<DirectoryUser>> searchUsers(Object account, String query) {
        return CompletableFuture.completedFuture(List.of());
    }

    @Override
    public CompletableFuture<Optional<DirectoryUser>> getUser(Object account, String userId) {
        return CompletableFuture.completedFuture(Optional.empty());
    }

    @Override
    public CompletableFuture<DirectoryUser> getCurrentUser(Object account) {
        return CompletableFuture.completedFuture(
                new DirectoryUser("0", "Unknown", "Unknown", Optional.empty(), Optional.empty(), false, java.util.Map.of())
        );
    }
}
