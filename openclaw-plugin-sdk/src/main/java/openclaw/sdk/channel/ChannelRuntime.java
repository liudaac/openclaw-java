package openclaw.sdk.channel;

import java.util.concurrent.CompletableFuture;

/**
 * Channel runtime for messaging operations.
 *
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public interface ChannelRuntime {

    /**
     * Sends a message to a channel.
     *
     * @param request the send request
     * @return a future containing the send result
     */
    CompletableFuture<SendResult> sendMessage(SendMessageRequest request);

    /**
     * Sends a typing indicator to a channel.
     *
     * @param channelId the channel ID
     * @return a future that completes when sent
     */
    CompletableFuture<Void> sendTypingIndicator(String channelId);

    /**
     * Uploads media to a channel.
     *
     * @param request the upload request
     * @return a future containing the upload result
     */
    CompletableFuture<MediaUploadResult> uploadMedia(MediaUploadRequest request);

    /**
     * Gets channel information.
     *
     * @param channelId the channel ID
     * @return a future containing the channel info
     */
    CompletableFuture<ChannelInfo> getChannelInfo(String channelId);

    /**
     * Checks if a channel is available.
     *
     * @param channelId the channel ID
     * @return a future containing availability status
     */
    CompletableFuture<Boolean> isChannelAvailable(String channelId);
}
