package openclaw.sdk.channel;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Channel threading adapter for thread management.
 *
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public interface ChannelThreadingAdapter {

    /**
     * Creates a new thread.
     *
     * @param account the account
     * @param parentId the parent message ID
     * @param name the thread name
     * @return the created thread
     */
    CompletableFuture<ThreadInfo> createThread(
            Object account,
            String parentId,
            Optional<String> name
    );

    /**
     * Gets thread information.
     *
     * @param account the account
     * @param threadId the thread ID
     * @return the thread info if found
     */
    CompletableFuture<Optional<ThreadInfo>> getThread(Object account, String threadId);

    /**
     * Lists threads in a channel.
     *
     * @param account the account
     * @param channelId the channel ID
     * @return list of threads
     */
    CompletableFuture<List<ThreadInfo>> listThreads(Object account, String channelId);

    /**
     * Archives a thread.
     *
     * @param account the account
     * @param threadId the thread ID
     * @return completion future
     */
    CompletableFuture<Void> archiveThread(Object account, String threadId);

    /**
     * Unarchives a thread.
     *
     * @param account the account
     * @param threadId the thread ID
     * @return completion future
     */
    CompletableFuture<Void> unarchiveThread(Object account, String threadId);

    /**
     * Thread information.
     *
     * @param id the thread ID
     * @param name the thread name
     * @param parentId the parent message ID
     * @param channelId the channel ID
     * @param messageCount number of messages
     * @param isArchived whether the thread is archived
     * @param createdAt creation timestamp
     * @param lastActivityAt last activity timestamp
     */
    record ThreadInfo(
            String id,
            String name,
            String parentId,
            String channelId,
            int messageCount,
            boolean isArchived,
            long createdAt,
            long lastActivityAt
    ) {
    }
}
