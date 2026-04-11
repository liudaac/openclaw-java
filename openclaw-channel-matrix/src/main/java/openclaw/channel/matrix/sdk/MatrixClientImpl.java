package openclaw.channel.matrix.sdk;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

/**
 * Default implementation of MatrixClient using OkHttp and Matrix Client-Server API.
 */
public class MatrixClientImpl implements MatrixClient {
    private static final Logger logger = LoggerFactory.getLogger(MatrixClientImpl.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final String homeserver;
    private final String accessToken;
    private final String userId;
    private final String deviceId;
    private final OkHttpClient httpClient;
    private final Set<String> dmRoomIds = ConcurrentHashMap.newKeySet();
    private final Map<String, List<MatrixEventListener<?>>> listeners = new ConcurrentHashMap<>();

    private volatile boolean started = false;
    private volatile String resolvedUserId;

    public MatrixClientImpl(String homeserver, String accessToken) {
        this(homeserver, accessToken, null, null);
    }

    public MatrixClientImpl(String homeserver, String accessToken, String userId, String deviceId) {
        this.homeserver = homeserver.endsWith("/") ? homeserver.substring(0, homeserver.length() - 1) : homeserver;
        this.accessToken = accessToken;
        this.userId = userId;
        this.deviceId = deviceId;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();
    }

    @Override
    public CompletableFuture<String> getUserId() {
        if (resolvedUserId != null) {
            return CompletableFuture.completedFuture(resolvedUserId);
        }
        if (userId != null) {
            resolvedUserId = userId;
            return CompletableFuture.completedFuture(userId);
        }
        return doRequestJson("GET", "/_matrix/client/v3/account/whoami", null, null)
                .thenApply(response -> {
                    String id = response.get("user_id").asText();
                    resolvedUserId = id;
                    return id;
                });
    }

    @Override
    public String getDeviceId() {
        return deviceId;
    }

    @Override
    public CompletableFuture<Set<String>> getJoinedRooms() {
        return doRequestJson("GET", "/_matrix/client/v3/joined_rooms", null, null)
                .thenApply(response -> {
                    Set<String> rooms = new HashSet<>();
                    JsonNode joinedRooms = response.get("joined_rooms");
                    if (joinedRooms != null && joinedRooms.isArray()) {
                        for (JsonNode room : joinedRooms) {
                            rooms.add(room.asText());
                        }
                    }
                    return rooms;
                });
    }

    @Override
    public CompletableFuture<Set<String>> getJoinedRoomMembers(String roomId) {
        String encodedRoomId = URLEncoder.encode(roomId, StandardCharsets.UTF_8);
        return doRequestJson("GET", "/_matrix/client/v3/rooms/" + encodedRoomId + "/joined_members", null, null)
                .thenApply(response -> {
                    Set<String> members = new HashSet<>();
                    JsonNode joined = response.get("joined");
                    if (joined != null && joined.isObject()) {
                        joined.fieldNames().forEachRemaining(members::add);
                    }
                    return members;
                });
    }

    @Override
    public CompletableFuture<Map<String, Object>> getAccountData(String eventType) {
        String encodedType = URLEncoder.encode(eventType, StandardCharsets.UTF_8);
        return getUserId().thenCompose(uid -> {
            String path = "/_matrix/client/v3/user/" + URLEncoder.encode(uid, StandardCharsets.UTF_8) + "/account_data/" + encodedType;
            return doRequestJson("GET", path, null, null);
        }).thenApply(this::jsonNodeToMap);
    }

    @Override
    public CompletableFuture<Void> setAccountData(String eventType, Map<String, Object> content) {
        String encodedType = URLEncoder.encode(eventType, StandardCharsets.UTF_8);
        ObjectNode body = mapper.valueToTree(content);
        return getUserId().thenCompose(uid -> {
            String path = "/_matrix/client/v3/user/" + URLEncoder.encode(uid, StandardCharsets.UTF_8) + "/account_data/" + encodedType;
            return doRequestJson("PUT", path, null, body);
        }).thenApply(r -> null);
    }

    @Override
    public CompletableFuture<Map<String, Object>> getRoomStateEvent(String roomId, String eventType, String stateKey) {
        String encodedRoomId = URLEncoder.encode(roomId, StandardCharsets.UTF_8);
        String encodedType = URLEncoder.encode(eventType, StandardCharsets.UTF_8);
        String encodedStateKey = stateKey != null ? URLEncoder.encode(stateKey, StandardCharsets.UTF_8) : "";
        return doRequestJson("GET", "/_matrix/client/v3/rooms/" + encodedRoomId + "/state/" + encodedType + "/" + encodedStateKey, null, null)
                .thenApply(this::jsonNodeToMap);
    }

    @Override
    public CompletableFuture<String> resolveRoom(String aliasOrRoomId) {
        if (aliasOrRoomId.startsWith("!")) {
            return CompletableFuture.completedFuture(aliasOrRoomId);
        }
        if (!aliasOrRoomId.startsWith("#")) {
            return CompletableFuture.completedFuture(aliasOrRoomId);
        }
        String encodedAlias = URLEncoder.encode(aliasOrRoomId, StandardCharsets.UTF_8);
        return doRequestJson("GET", "/_matrix/client/v3/directory/room/" + encodedAlias, null, null)
                .thenApply(response -> response.get("room_id").asText())
                .exceptionally(ex -> null);
    }

    @Override
    public CompletableFuture<Void> joinRoom(String roomId) {
        String encodedRoomId = URLEncoder.encode(roomId, StandardCharsets.UTF_8);
        ObjectNode body = mapper.createObjectNode();
        return doRequestJson("POST", "/_matrix/client/v3/rooms/" + encodedRoomId + "/join", null, body)
                .thenApply(r -> null);
    }

    @Override
    public CompletableFuture<String> sendMessage(String roomId, Map<String, Object> content) {
        String encodedRoomId = URLEncoder.encode(roomId, StandardCharsets.UTF_8);
        String txnId = generateTxnId();
        ObjectNode body = mapper.valueToTree(content);
        return doRequestJson("PUT", "/_matrix/client/v3/rooms/" + encodedRoomId + "/send/m.room.message/" + txnId, null, body)
                .thenApply(response -> response.get("event_id").asText());
    }

    @Override
    public CompletableFuture<String> sendStateEvent(String roomId, String eventType, String stateKey, Map<String, Object> content) {
        String encodedRoomId = URLEncoder.encode(roomId, StandardCharsets.UTF_8);
        String encodedType = URLEncoder.encode(eventType, StandardCharsets.UTF_8);
        String encodedStateKey = stateKey != null ? URLEncoder.encode(stateKey, StandardCharsets.UTF_8) : "";
        ObjectNode body = mapper.valueToTree(content);
        return doRequestJson("PUT", "/_matrix/client/v3/rooms/" + encodedRoomId + "/state/" + encodedType + "/" + encodedStateKey, null, body)
                .thenApply(response -> response.get("event_id").asText());
    }

    @Override
    public CompletableFuture<String> sendReaction(String roomId, String eventId, String emoji) {
        Map<String, Object> content = new HashMap<>();
        content.put("m.relates_to", Map.of(
                "rel_type", "m.annotation",
                "event_id", eventId,
                "key", emoji
        ));
        String encodedRoomId = URLEncoder.encode(roomId, StandardCharsets.UTF_8);
        String txnId = generateTxnId();
        ObjectNode body = mapper.valueToTree(content);
        return doRequestJson("PUT", "/_matrix/client/v3/rooms/" + encodedRoomId + "/send/m.reaction/" + txnId, null, body)
                .thenApply(response -> response.get("event_id").asText());
    }

    @Override
    public CompletableFuture<String> redactEvent(String roomId, String eventId, String reason) {
        String encodedRoomId = URLEncoder.encode(roomId, StandardCharsets.UTF_8);
        String encodedEventId = URLEncoder.encode(eventId, StandardCharsets.UTF_8);
        String txnId = generateTxnId();
        ObjectNode body = mapper.createObjectNode();
        if (reason != null && !reason.isEmpty()) {
            body.put("reason", reason);
        }
        return doRequestJson("PUT", "/_matrix/client/v3/rooms/" + encodedRoomId + "/redact/" + encodedEventId + "/" + txnId, null, body)
                .thenApply(response -> response.get("event_id").asText());
    }

    @Override
    public CompletableFuture<Map<String, Object>> getUserProfile(String userId) {
        String encodedUserId = URLEncoder.encode(userId, StandardCharsets.UTF_8);
        return doRequestJson("GET", "/_matrix/client/v3/profile/" + encodedUserId, null, null)
                .thenApply(this::jsonNodeToMap);
    }

    @Override
    public CompletableFuture<Void> setDisplayName(String displayName) {
        return getUserId().thenCompose(uid -> {
            String encodedUserId = URLEncoder.encode(uid, StandardCharsets.UTF_8);
            ObjectNode body = mapper.createObjectNode();
            body.put("displayname", displayName);
            return doRequestJson("PUT", "/_matrix/client/v3/profile/" + encodedUserId + "/displayname", null, body);
        }).thenApply(r -> null);
    }

    @Override
    public CompletableFuture<Void> setAvatarUrl(String avatarUrl) {
        return getUserId().thenCompose(uid -> {
            String encodedUserId = URLEncoder.encode(uid, StandardCharsets.UTF_8);
            ObjectNode body = mapper.createObjectNode();
            body.put("avatar_url", avatarUrl);
            return doRequestJson("PUT", "/_matrix/client/v3/profile/" + encodedUserId + "/avatar_url", null, body);
        }).thenApply(r -> null);
    }

    @Override
    public CompletableFuture<Map<String, Object>> getEvent(String roomId, String eventId) {
        String encodedRoomId = URLEncoder.encode(roomId, StandardCharsets.UTF_8);
        String encodedEventId = URLEncoder.encode(eventId, StandardCharsets.UTF_8);
        return doRequestJson("GET", "/_matrix/client/v3/rooms/" + encodedRoomId + "/event/" + encodedEventId, null, null)
                .thenApply(this::jsonNodeToMap);
    }

    @Override
    public CompletableFuture<Void> setTyping(String roomId, boolean typing, int timeoutMs) {
        String encodedRoomId = URLEncoder.encode(roomId, StandardCharsets.UTF_8);
        return getUserId().thenCompose(uid -> {
            String encodedUserId = URLEncoder.encode(uid, StandardCharsets.UTF_8);
            ObjectNode body = mapper.createObjectNode();
            body.put("typing", typing);
            if (typing) {
                body.put("timeout", timeoutMs);
            }
            return doRequestJson("PUT", "/_matrix/client/v3/rooms/" + encodedRoomId + "/typing/" + encodedUserId, null, body);
        }).thenApply(r -> null);
    }

    @Override
    public CompletableFuture<Void> sendReadReceipt(String roomId, String eventId) {
        String encodedRoomId = URLEncoder.encode(roomId, StandardCharsets.UTF_8);
        String encodedEventId = URLEncoder.encode(eventId, StandardCharsets.UTF_8);
        ObjectNode body = mapper.createObjectNode();
        return doRequestJson("POST", "/_matrix/client/v3/rooms/" + encodedRoomId + "/receipt/m.read/" + encodedEventId, null, body)
                .thenApply(r -> null);
    }

    @Override
    public CompletableFuture<byte[]> downloadContent(String mxcUrl) {
        // Parse mxc://server/mediaId
        if (!mxcUrl.startsWith("mxc://")) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Invalid MXC URL: " + mxcUrl));
        }
        String[] parts = mxcUrl.substring(6).split("/", 2);
        if (parts.length != 2) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Invalid MXC URL: " + mxcUrl));
        }
        String server = URLEncoder.encode(parts[0], StandardCharsets.UTF_8);
        String mediaId = URLEncoder.encode(parts[1], StandardCharsets.UTF_8);

        // Try authenticated endpoint first
        String endpoint = "/_matrix/client/v1/media/download/" + server + "/" + mediaId;
        return doRequestRaw("GET", endpoint, Map.of("allow_remote", "true"), null)
                .exceptionallyCompose(ex -> {
                    // Fall back to legacy endpoint
                    String legacyEndpoint = "/_matrix/media/v3/download/" + server + "/" + mediaId;
                    return doRequestRaw("GET", legacyEndpoint, Map.of("allow_remote", "true"), null);
                });
    }

    @Override
    public CompletableFuture<String> uploadContent(byte[] data, String contentType, String filename) {
        String ct = contentType != null ? contentType : "application/octet-stream";
        RequestBody body = RequestBody.create(data, MediaType.parse(ct));
        Map<String, String> query = filename != null ? Map.of("filename", filename) : null;
        return doRequestInternal("POST", "/_matrix/client/v3/media/upload", query, body)
                .thenApply(response -> response.get("content_uri").asText());
    }

    @Override
    public String mxcToHttp(String mxcUrl) {
        if (!mxcUrl.startsWith("mxc://")) {
            return null;
        }
        String[] parts = mxcUrl.substring(6).split("/", 2);
        if (parts.length != 2) {
            return null;
        }
        return homeserver + "/_matrix/media/v3/download/" + parts[0] + "/" + parts[1];
    }

    @Override
    public boolean isDmRoom(String roomId) {
        return dmRoomIds.contains(roomId);
    }

    @Override
    public CompletableFuture<Boolean> refreshDmCache() {
        return getAccountData("m.direct").thenApply(direct -> {
            dmRoomIds.clear();
            if (direct == null) {
                return false;
            }
            for (Object value : direct.values()) {
                if (value instanceof List) {
                    for (Object roomId : (List<?>) value) {
                        if (roomId instanceof String) {
                            dmRoomIds.add((String) roomId);
                        }
                    }
                }
            }
            return !dmRoomIds.isEmpty();
        });
    }

    @Override
    public CompletableFuture<Void> start() {
        return refreshDmCache().thenApply(r -> {
            started = true;
            return null;
        });
    }

    @Override
    public void stop() {
        started = false;
    }

    @Override
    public boolean isStarted() {
        return started;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> void on(String eventName, MatrixEventListener<T> listener) {
        listeners.computeIfAbsent(eventName, k -> new CopyOnWriteArrayList<>()).add(listener);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> void off(String eventName, MatrixEventListener<T> listener) {
        List<MatrixEventListener<?>> list = listeners.get(eventName);
        if (list != null) {
            list.remove(listener);
        }
    }

    // Internal methods

    private CompletableFuture<JsonNode> doRequestJson(String method, String endpoint, Map<String, String> query, ObjectNode jsonBody) {
        RequestBody requestBody = jsonBody != null ? RequestBody.create(jsonBody.toString(), JSON) : null;
        return doRequestInternal(method, endpoint, query, requestBody);
    }

    private CompletableFuture<JsonNode> doRequestInternal(String method, String endpoint, Map<String, String> query, RequestBody body) {
        StringBuilder url = new StringBuilder(homeserver + endpoint);
        if (query != null && !query.isEmpty()) {
            url.append("?");
            boolean first = true;
            for (Map.Entry<String, String> entry : query.entrySet()) {
                if (!first) url.append("&");
                url.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8));
                url.append("=");
                url.append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
                first = false;
            }
        }

        Request.Builder builder = new Request.Builder()
                .url(url.toString())
                .header("Authorization", "Bearer " + accessToken);

        if ("GET".equals(method)) {
            builder.get();
        } else if (body != null) {
            builder.method(method, body);
        } else {
            builder.method(method, RequestBody.create(new byte[0], null));
        }

        Request request = builder.build();

        CompletableFuture<JsonNode> future = new CompletableFuture<>();
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                future.completeExceptionally(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    if (!response.isSuccessful()) {
                        future.completeExceptionally(new IOException("HTTP " + response.code() + ": " + response.message()));
                        return;
                    }
                    if (responseBody == null) {
                        future.complete(mapper.createObjectNode());
                        return;
                    }
                    String json = responseBody.string();
                    if (json.isEmpty()) {
                        future.complete(mapper.createObjectNode());
                    } else {
                        future.complete(mapper.readTree(json));
                    }
                }
            }
        });
        return future;
    }

    private CompletableFuture<byte[]> doRequestRaw(String method, String endpoint, Map<String, String> query, RequestBody body) {
        StringBuilder url = new StringBuilder(homeserver + endpoint);
        if (query != null && !query.isEmpty()) {
            url.append("?");
            boolean first = true;
            for (Map.Entry<String, String> entry : query.entrySet()) {
                if (!first) url.append("&");
                url.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8));
                url.append("=");
                url.append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
                first = false;
            }
        }

        Request.Builder builder = new Request.Builder()
                .url(url.toString())
                .header("Authorization", "Bearer " + accessToken);

        if ("GET".equals(method)) {
            builder.get();
        } else if (body != null) {
            builder.method(method, body);
        } else {
            builder.method(method, RequestBody.create(new byte[0], null));
        }

        Request request = builder.build();

        CompletableFuture<byte[]> future = new CompletableFuture<>();
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                future.completeExceptionally(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    if (!response.isSuccessful()) {
                        future.completeExceptionally(new IOException("HTTP " + response.code() + ": " + response.message()));
                        return;
                    }
                    if (responseBody == null) {
                        future.complete(new byte[0]);
                        return;
                    }
                    future.complete(responseBody.bytes());
                }
            }
        });
        return future;
    }

    private Map<String, Object> jsonNodeToMap(JsonNode node) {
        if (node == null || node.isNull()) {
            return new HashMap<>();
        }
        return mapper.convertValue(node, Map.class);
    }

    private String generateTxnId() {
        return "openclaw-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
}