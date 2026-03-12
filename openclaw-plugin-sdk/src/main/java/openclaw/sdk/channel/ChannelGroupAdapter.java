package openclaw.sdk.channel;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Channel group adapter for group/room management.
 *
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public interface ChannelGroupAdapter {

    /**
     * Gets groups for an account.
     *
     * @param account the account
     * @return list of groups
     */
    CompletableFuture<List<Group>> getGroups(Object account);

    /**
     * Gets a specific group.
     *
     * @param account the account
     * @param groupId the group ID
     * @return the group if found
     */
    CompletableFuture<Optional<Group>> getGroup(Object account, String groupId);

    /**
     * Gets members of a group.
     *
     * @param account the account
     * @param groupId the group ID
     * @return list of members
     */
    CompletableFuture<List<GroupMember>> getMembers(Object account, String groupId);

    /**
     * Group information.
     *
     * @param id the group ID
     * @param name the group name
     * @param type the group type
     * @param memberCount number of members
     * @param metadata additional metadata
     */
    record Group(
            String id,
            String name,
            GroupType type,
            int memberCount,
            java.util.Map<String, Object> metadata
    ) {
    }

    /**
     * Group type.
     */
    enum GroupType {
        GROUP,
        CHANNEL,
        ROOM,
        THREAD,
        FORUM
    }

    /**
     * Group member.
     *
     * @param id the member ID
     * @param name the member name
     * @param role the member role
     * @param isBot whether the member is a bot
     */
    record GroupMember(
            String id,
            String name,
            MemberRole role,
            boolean isBot
    ) {
    }

    /**
     * Member role.
     */
    enum MemberRole {
        OWNER,
        ADMIN,
        MEMBER,
        GUEST
    }
}
