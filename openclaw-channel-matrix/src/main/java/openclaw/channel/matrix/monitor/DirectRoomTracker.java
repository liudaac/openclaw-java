package openclaw.channel.matrix.monitor;

import openclaw.channel.matrix.sdk.MatrixClient;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Direct room tracker for Matrix DM optimization.
 * Simplified implementation of the TypeScript direct.ts optimization.
 */
public class DirectRoomTracker {

    private static final long DM_CACHE_TTL_MS = 30_000;
    private static final long RECENT_INVITE_TTL_MS = 30_000;
    private static final int MAX_TRACKED_DM_ROOMS = 1024;
    private static final int MAX_TRACKED_DM_MEMBER_FLAGS = 2048;

    private final MatrixClient client;
    private long lastDmUpdateMs = 0;
    private boolean hasSeededDmCache = false;
    private String cachedSelfUserId;

    private final Map<String, JoinedMembersCacheEntry> joinedMembersCache = new ConcurrentHashMap<>();
    private final Map<String, DirectMemberFlagCacheEntry> directMemberFlagCache = new ConcurrentHashMap<>();
    private final Map<String, RecentInviteEntry> recentInviteCandidates = new ConcurrentHashMap<>();

    public DirectRoomTracker(MatrixClient client) {
        this.client = client;
    }

    /**
     * Check if a room is a direct message room with proper state tracking.
     */
    public boolean isDirectMessageRoom(String roomId, String senderId) {
        // Check cache first
        DirectMemberFlagCacheEntry cached = directMemberFlagCache.get(roomId);
        long now = System.currentTimeMillis();
        
        if (cached != null && now - cached.ts < DM_CACHE_TTL_MS) {
            return cached.isDirect != null ? cached.isDirect : false;
        }

        // Resolve from client
        Boolean isDirect = resolveDirectMemberFlag(roomId, senderId);
        rememberBounded(directMemberFlagCache, roomId, 
            new DirectMemberFlagCacheEntry(isDirect, now), 
            MAX_TRACKED_DM_MEMBER_FLAGS);
        
        return isDirect != null ? isDirect : false;
    }

    /**
     * Track a recent invite candidate for potential promotion.
     */
    public void trackRecentInvite(String roomId, String remoteUserId) {
        long now = System.currentTimeMillis();
        rememberBounded(recentInviteCandidates, roomId, 
            new RecentInviteEntry(remoteUserId, now),
            MAX_TRACKED_DM_ROOMS);
    }

    /**
     * Check if an invite is recent enough to be promoted.
     */
    public boolean canPromoteRecentInvite(String roomId) {
        RecentInviteEntry entry = recentInviteCandidates.get(roomId);
        if (entry == null) {
            return false;
        }
        long now = System.currentTimeMillis();
        return now - entry.ts < RECENT_INVITE_TTL_MS;
    }

    /**
     * Refresh DM cache from server.
     */
    public void refreshDmCache() {
        long now = System.currentTimeMillis();
        if (now - lastDmUpdateMs < DM_CACHE_TTL_MS) {
            return;
        }
        lastDmUpdateMs = now;
        // In real implementation: hasSeededDmCache = client.getDms().update() || hasSeededDmCache;
        hasSeededDmCache = true;
    }

    private Boolean resolveDirectMemberFlag(String roomId, String userId) {
        // Simplified: In real implementation, check Matrix m.direct account data
        return null; // Unknown, let other logic decide
    }

    private <T> void rememberBounded(Map<String, T> map, String key, T value, int maxSize) {
        map.put(key, value);
        if (map.size() > maxSize) {
            String oldest = map.keySet().iterator().next();
            map.remove(oldest);
        }
    }

    // Cache entry records
    private record JoinedMembersCacheEntry(String[] members, long ts) {}
    private record DirectMemberFlagCacheEntry(Boolean isDirect, long ts) {}
    private record RecentInviteEntry(String remoteUserId, long ts) {}
}
