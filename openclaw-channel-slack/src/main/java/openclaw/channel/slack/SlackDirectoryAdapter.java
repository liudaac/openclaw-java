package openclaw.channel.slack;

import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.request.users.UsersInfoRequest;
import com.slack.api.methods.request.users.UsersListRequest;
import com.slack.api.methods.response.users.UsersInfoResponse;
import com.slack.api.methods.response.users.UsersListResponse;
import com.slack.api.model.User;
import openclaw.sdk.channel.ChannelDirectoryAdapter;
import openclaw.sdk.channel.DirectoryQueryResult;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Slack Directory Adapter
 */
public class SlackDirectoryAdapter implements ChannelDirectoryAdapter {

    private MethodsClient methods;

    public void setMethods(MethodsClient methods) {
        this.methods = methods;
    }

    @Override
    public CompletableFuture<DirectoryQueryResult> query(DirectoryQuery query) {
        return CompletableFuture.supplyAsync(() -> {
            if (methods == null) {
                return DirectoryQueryResult.failure("Slack client not initialized");
            }

            try {
                switch (query.type()) {
                    case USER:
                        return queryUser(query);
                    case CHAT:
                        return queryChannel(query);
                    default:
                        return DirectoryQueryResult.failure("Unsupported query type: " + query.type());
                }

            } catch (Exception e) {
                return DirectoryQueryResult.failure(e.getMessage());
            }
        });
    }

    private DirectoryQueryResult queryUser(DirectoryQuery query) {
        try {
            UsersInfoResponse response = methods.usersInfo(UsersInfoRequest.builder()
                    .user(query.query())
                    .build());

            if (!response.isOk()) {
                return DirectoryQueryResult.failure("User not found: " + response.getError());
            }

            User user = response.getUser();
            DirectoryEntry entry = new DirectoryEntry(
                    user.getId(),
                    user.getName(),
                    user.getProfile().getImageOriginal(),
                    Optional.ofNullable(user.getRealName()).orElse(user.getName()),
                    Map.of(
                            "email", user.getProfile().getEmail() != null ? user.getProfile().getEmail() : "",
                            "title", user.getProfile().getTitle() != null ? user.getProfile().getTitle() : "",
                            "isAdmin", user.isAdmin(),
                            "isBot", user.isBot()
                    )
            );

            return DirectoryQueryResult.success(List.of(entry));

        } catch (Exception e) {
            return DirectoryQueryResult.failure(e.getMessage());
        }
    }

    private DirectoryQueryResult queryChannel(DirectoryQuery query) {
        // Slack channel query would require conversations.info
        // Simplified implementation
        return DirectoryQueryResult.failure("Channel query not implemented");
    }

    @Override
    public CompletableFuture<List<DirectoryEntry>> listUsers(String chatId, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            if (methods == null) {
                return List.of();
            }

            try {
                UsersListResponse response = methods.usersList(UsersListRequest.builder()
                        .limit(limit)
                        .build());

                if (!response.isOk()) {
                    return List.of();
                }

                return response.getMembers().stream()
                        .filter(user -> !user.isBot())
                        .map(user -> new DirectoryEntry(
                                user.getId(),
                                user.getName(),
                                user.getProfile().getImageOriginal(),
                                Optional.ofNullable(user.getRealName()).orElse(user.getName()),
                                Map.of(
                                        "email", user.getProfile().getEmail() != null ? user.getProfile().getEmail() : "",
                                        "title", user.getProfile().getTitle() != null ? user.getProfile().getTitle() : ""
                                )
                        ))
                        .collect(Collectors.toList());

            } catch (Exception e) {
                return List.of();
            }
        });
    }

    @Override
    public CompletableFuture<List<DirectoryEntry>> listChats(String groupId, int limit) {
        // Slack channels list
        return CompletableFuture.completedFuture(List.of());
    }

    @Override
    public CompletableFuture<Optional<DirectoryEntry>> getUserInfo(String userId) {
        return query(new DirectoryQuery(DirectoryQuery.QueryType.USER, userId, 1))
                .thenApply(result -> {
                    if (result.success() && !result.entries().isEmpty()) {
                        return Optional.of(result.entries().get(0));
                    }
                    return Optional.empty();
                });
    }

    @Override
    public CompletableFuture<Optional<DirectoryEntry>> getChatInfo(String chatId) {
        return CompletableFuture.completedFuture(Optional.empty());
    }
}
