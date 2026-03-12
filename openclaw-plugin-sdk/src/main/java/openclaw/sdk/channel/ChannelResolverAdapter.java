package openclaw.sdk.channel;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Channel resolver adapter for routing resolution.
 *
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public interface ChannelResolverAdapter {

    /**
     * Resolves a target to a channel route.
     *
     * @param account the account
     * @param target the target identifier
     * @return the resolved route if found
     */
    CompletableFuture<Optional<ResolvedRoute>> resolveTarget(
            Object account,
            String target
    );

    /**
     * Resolves a sender to a user.
     *
     * @param account the account
     * @param senderId the sender ID
     * @return the resolved user if found
     */
    CompletableFuture<Optional<ResolvedUser>> resolveSender(
            Object account,
            String senderId
    );

    /**
     * Resolved route.
     *
     * @param channelId the channel ID
     * @param accountId the account ID
     * @param targetId the target ID
     * @param targetType the target type
     * @param metadata additional metadata
     */
    record ResolvedRoute(
            String channelId,
            String accountId,
            String targetId,
            TargetType targetType,
            Map<String, Object> metadata
    ) {
    }

    /**
     * Target type.
     */
    enum TargetType {
        USER,
        GROUP,
        CHANNEL,
        THREAD
    }

    /**
     * Resolved user.
     *
     * @param userId the user ID
     * @param userName the user name
     * @param displayName the display name
     * @param isAuthorized whether the user is authorized
     * @param metadata additional metadata
     */
    record ResolvedUser(
            String userId,
            String userName,
            String displayName,
            boolean isAuthorized,
            Map<String, Object> metadata
    ) {
    }
}
