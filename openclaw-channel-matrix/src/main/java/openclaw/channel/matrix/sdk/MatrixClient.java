package openclaw.channel.matrix.sdk;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Simplified Matrix client interface for OpenClaw Java Edition.
 * Provides core functionality needed by channel implementations.
 */
public interface MatrixClient {

    /**
     * Get the user ID of the logged-in user.
     */
    CompletableFuture<String> getUserId();

    /**
     * Get the device ID of the current session.
     */
    String getDeviceId();

    /**
     * Get list of joined room IDs.
     */
    CompletableFuture<Set<String>> getJoinedRooms();

    /**
     * Get joined members of a room.
     */
    CompletableFuture<Set<String>> getJoinedRoomMembers(String roomId);

    /**
     * Get account data by event type.
     */
    CompletableFuture<Map<String, Object>> getAccountData(String eventType);

    /**
     * Set account data.
     */
    CompletableFuture<Void> setAccountData(String eventType, Map<String, Object> content);

    /**
     * Get room state event.
     */
    CompletableFuture<Map<String, Object>> getRoomStateEvent(String roomId, String eventType, String stateKey);

    /**
     * Resolve a room alias to room ID.
     */
    CompletableFuture<String> resolveRoom(String aliasOrRoomId);

    /**
     * Join a room.
     */
    CompletableFuture<Void> joinRoom(String roomId);

    /**
     * Send a message to a room.
     */
    CompletableFuture<String> sendMessage(String roomId, Map<String, Object> content);

    /**
     * Send a state event to a room.
     */
    CompletableFuture<String> sendStateEvent(String roomId, String eventType, String stateKey, Map<String, Object> content);

    /**
     * Send a reaction to an event.
     */
    CompletableFuture<String> sendReaction(String roomId, String eventId, String emoji);

    /**
     * Redact an event.
     */
    CompletableFuture<String> redactEvent(String roomId, String eventId, String reason);

    /**
     * Get user profile.
     */
    CompletableFuture<Map<String, Object>> getUserProfile(String userId);

    /**
     * Set display name.
     */
    CompletableFuture<Void> setDisplayName(String displayName);

    /**
     * Set avatar URL.
     */
    CompletableFuture<Void> setAvatarUrl(String avatarUrl);

    /**
     * Get a specific event.
     */
    CompletableFuture<Map<String, Object>> getEvent(String roomId, String eventId);

    /**
     * Set typing indicator.
     */
    CompletableFuture<Void> setTyping(String roomId, boolean typing, int timeoutMs);

    /**
     * Send read receipt.
     */
    CompletableFuture<Void> sendReadReceipt(String roomId, String eventId);

    /**
     * Download content from MXC URL.
     */
    CompletableFuture<byte[]> downloadContent(String mxcUrl);

    /**
     * Upload content and get MXC URL.
     */
    CompletableFuture<String> uploadContent(byte[] data, String contentType, String filename);

    /**
     * Convert MXC URL to HTTP URL.
     */
    String mxcToHttp(String mxcUrl);

    /**
     * Check if a room is a direct message room.
     */
    boolean isDmRoom(String roomId);

    /**
     * Refresh DM cache from server.
     */
    CompletableFuture<Boolean> refreshDmCache();

    /**
     * Start the client and sync.
     */
    CompletableFuture<Void> start();

    /**
     * Stop the client.
     */
    void stop();

    /**
     * Check if client is started.
     */
    boolean isStarted();

    /**
     * Add event listener.
     */
    <T> void on(String eventName, MatrixEventListener<T> listener);

    /**
     * Remove event listener.
     */
    <T> void off(String eventName, MatrixEventListener<T> listener);

    /**
     * Event listener interface.
     */
    @FunctionalInterface
    interface MatrixEventListener<T> {
        void onEvent(T event);
    }
}
