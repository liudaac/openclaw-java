package openclaw.sdk.channel;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Channel directory adapter for user/group lookup.
 *
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public interface ChannelDirectoryAdapter {

    /**
     * Searches for users.
     *
     * @param account the account
     * @param query the search query
     * @return list of matching users
     */
    CompletableFuture<List<DirectoryUser>> searchUsers(
            Object account,
            String query
    );

    /**
     * Gets a user by ID.
     *
     * @param account the account
     * @param userId the user ID
     * @return the user if found
     */
    CompletableFuture<Optional<DirectoryUser>> getUser(
            Object account,
            String userId
    );

    /**
     * Gets the current user.
     *
     * @param account the account
     * @return the current user
     */
    CompletableFuture<DirectoryUser> getCurrentUser(Object account);

    /**
     * Directory user.
     *
     * @param id the user ID
     * @param name the user name
     * @param displayName the display name
     * @param email the email address
     * @param avatarUrl the avatar URL
     * @param isBot whether the user is a bot
     * @param metadata additional metadata
     */
    record DirectoryUser(
            String id,
            String name,
            String displayName,
            Optional<String> email,
            Optional<String> avatarUrl,
            boolean isBot,
            java.util.Map<String, Object> metadata
    ) {
    }
}
