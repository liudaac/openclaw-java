package openclaw.channel.feishu;

import openclaw.sdk.channel.ChannelDirectoryAdapter;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Feishu directory adapter.
 *
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public class FeishuDirectoryAdapter implements ChannelDirectoryAdapter {

    @Override
    public CompletableFuture<List<DirectoryUser>> searchUsers(Object account, String query) {
        // TODO: Implement using Feishu contact API
        return CompletableFuture.completedFuture(List.of());
    }

    @Override
    public CompletableFuture<Optional<DirectoryUser>> getUser(Object account, String userId) {
        // TODO: Implement
        return CompletableFuture.completedFuture(Optional.empty());
    }

    @Override
    public CompletableFuture<DirectoryUser> getCurrentUser(Object account) {
        // TODO: Implement using bot info API
        return CompletableFuture.completedFuture(null);
    }
}
