package openclaw.session.binding;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.Set;

/**
 * Default implementation of SessionBindingService.
 *
 * @author OpenClaw Team
 * @version 2026.3.21
 * @since 2026.3.21
 */
@Service
public class DefaultSessionBindingService implements SessionBindingService {

    private static final Logger logger = LoggerFactory.getLogger(DefaultSessionBindingService.class);

    // Store bindings by bindingId
    private final Map<String, SessionBindingRecord> bindingsById;

    // Store bindings by session key
    private final Map<String, List<SessionBindingRecord>> bindingsBySession;

    // Store adapters by channel:account
    private final Map<String, SessionBindingAdapter> adapters;

    public DefaultSessionBindingService() {
        this.bindingsById = new ConcurrentHashMap<>();
        this.bindingsBySession = new ConcurrentHashMap<>();
        this.adapters = new ConcurrentHashMap<>();
    }

    /**
     * Registers a binding adapter.
     *
     * @param adapter the adapter to register
     */
    public void registerAdapter(SessionBindingAdapter adapter) {
        String key = buildAdapterKey(adapter.getChannel(), adapter.getAccountId());
        adapters.put(key, adapter);
        logger.info("Registered session binding adapter for {}:{}", 
                adapter.getChannel(), adapter.getAccountId());
    }

    /**
     * Unregisters a binding adapter.
     *
     * @param channel the channel
     * @param accountId the account ID
     */
    public void unregisterAdapter(String channel, String accountId) {
        String key = buildAdapterKey(channel, accountId);
        adapters.remove(key);
        logger.info("Unregistered session binding adapter for {}:{}", channel, accountId);
    }

    /**
     * Unregisters a specific binding adapter instance.
     *
     * @param adapter the adapter to unregister
     */
    public void unregisterAdapter(SessionBindingAdapter adapter) {
        String key = buildAdapterKey(adapter.getChannel(), adapter.getAccountId());
        adapters.remove(key, adapter);
        logger.info("Unregistered specific session binding adapter for {}:{}", 
                adapter.getChannel(), adapter.getAccountId());
    }

    /**
     * Resets all adapters and bindings for testing.
     */
    public void resetForTest() {
        adapters.clear();
        bindingsById.clear();
        bindingsBySession.clear();
        logger.info("Reset session binding service for testing");
    }

    /**
     * Gets all registered adapter keys.
     *
     * @return set of adapter keys
     */
    public Set<String> getRegisteredAdapterKeys() {
        return Set.copyOf(adapters.keySet());
    }

    /**
     * Gets the count of registered adapters.
     *
     * @return adapter count
     */
    public int getAdapterCount() {
        return adapters.size();
    }

    @Override
    public Mono<SessionBindingRecord> bind(SessionBindingBindInput input) {
        return Mono.fromCallable(() -> {
            ConversationRef conversation = input.getConversation();
            SessionBindingAdapter adapter = resolveAdapter(conversation);

            if (adapter == null) {
                throw new SessionBindingException(
                        SessionBindingErrorCode.BINDING_ADAPTER_UNAVAILABLE,
                        "Session binding adapter unavailable for " + conversation,
                        conversation.getChannel(),
                        conversation.getAccountId(),
                        null
                );
            }

            if (!adapter.isBindSupported()) {
                throw new SessionBindingException(
                        SessionBindingErrorCode.BINDING_CAPABILITY_UNSUPPORTED,
                        "Session binding adapter does not support binding for " + conversation,
                        conversation.getChannel(),
                        conversation.getAccountId(),
                        null
                );
            }

            SessionBindingPlacement placement = input.getPlacement();
            if (placement == null) {
                placement = inferDefaultPlacement(conversation);
            }

            List<SessionBindingPlacement> supportedPlacements = adapter.getPlacements();
            if (!supportedPlacements.contains(placement)) {
                throw new SessionBindingException(
                        SessionBindingErrorCode.BINDING_CAPABILITY_UNSUPPORTED,
                        "Session binding placement \"" + placement + "\" is not supported for " + conversation,
                        conversation.getChannel(),
                        conversation.getAccountId(),
                        placement
                );
            }

            // Create binding record
            String bindingId = UUID.randomUUID().toString();
            Instant now = Instant.now();
            Instant expiresAt = input.getTtl() != null ? now.plus(input.getTtl()) : null;

            SessionBindingRecord record = SessionBindingRecord.builder()
                    .bindingId(bindingId)
                    .targetSessionKey(input.getTargetSessionKey())
                    .targetKind(input.getTargetKind())
                    .conversation(conversation)
                    .status(BindingStatus.ACTIVE)
                    .boundAt(now)
                    .expiresAt(expiresAt)
                    .metadata(input.getMetadata())
                    .build();

            // Store binding
            bindingsById.put(bindingId, record);
            bindingsBySession
                    .computeIfAbsent(input.getTargetSessionKey(), k -> new CopyOnWriteArrayList<>())
                    .add(record);

            logger.info("Created session binding {} for session {}", 
                    bindingId, input.getTargetSessionKey());

            return record;
        });
    }

    @Override
    public SessionBindingCapabilities getCapabilities(String channel, String accountId) {
        SessionBindingAdapter adapter = resolveAdapter(channel, accountId);
        if (adapter == null) {
            return SessionBindingCapabilities.unavailable();
        }

        return SessionBindingCapabilities.builder()
                .adapterAvailable(true)
                .bindSupported(adapter.isBindSupported())
                .unbindSupported(adapter.isUnbindSupported())
                .placements(adapter.getPlacements())
                .build();
    }

    @Override
    public List<SessionBindingRecord> listBySession(String targetSessionKey) {
        if (targetSessionKey == null || targetSessionKey.isBlank()) {
            return List.of();
        }

        List<SessionBindingRecord> records = bindingsBySession.get(targetSessionKey);
        if (records == null) {
            return List.of();
        }

        // Return deduplicated list
        return List.copyOf(new LinkedHashSet<>(records));
    }

    @Override
    public SessionBindingRecord resolveByConversation(ConversationRef ref) {
        if (ref == null || ref.getChannel() == null || ref.getConversationId() == null) {
            return null;
        }

        // Search through all bindings
        for (SessionBindingRecord record : bindingsById.values()) {
            if (record.getConversation().equals(ref) && 
                record.getStatus() == BindingStatus.ACTIVE) {
                return record;
            }
        }

        return null;
    }

    @Override
    public void touch(String bindingId) {
        if (bindingId == null || bindingId.isBlank()) {
            return;
        }

        SessionBindingRecord record = bindingsById.get(bindingId);
        if (record != null) {
            // In a real implementation, update last activity timestamp
            logger.debug("Touched binding {}", bindingId);
        }
    }

    @Override
    public Mono<List<SessionBindingRecord>> unbind(SessionBindingUnbindInput input) {
        return Mono.fromCallable(() -> {
            List<SessionBindingRecord> removed = new ArrayList<>();

            if (input.getBindingId() != null) {
                // Remove specific binding
                SessionBindingRecord record = bindingsById.remove(input.getBindingId());
                if (record != null) {
                    removed.add(record.withStatus(BindingStatus.ENDED));
                    removeFromSessionIndex(record);
                }
            } else if (input.getTargetSessionKey() != null) {
                // Remove all bindings for session
                List<SessionBindingRecord> sessionBindings = bindingsBySession.remove(input.getTargetSessionKey());
                if (sessionBindings != null) {
                    for (SessionBindingRecord record : sessionBindings) {
                        bindingsById.remove(record.getBindingId());
                        removed.add(record.withStatus(BindingStatus.ENDED));
                    }
                }
            }

            logger.info("Unbound {} session bindings for reason: {}", 
                    removed.size(), input.getReason());

            return removed;
        });
    }

    /**
     * Removes a binding from the session index.
     */
    private void removeFromSessionIndex(SessionBindingRecord record) {
        List<SessionBindingRecord> sessionBindings = bindingsBySession.get(record.getTargetSessionKey());
        if (sessionBindings != null) {
            sessionBindings.remove(record);
            if (sessionBindings.isEmpty()) {
                bindingsBySession.remove(record.getTargetSessionKey());
            }
        }
    }

    /**
     * Resolves adapter for a conversation.
     */
    private SessionBindingAdapter resolveAdapter(ConversationRef ref) {
        return resolveAdapter(ref.getChannel(), ref.getAccountId());
    }

    /**
     * Resolves adapter for channel/account.
     */
    private SessionBindingAdapter resolveAdapter(String channel, String accountId) {
        String key = buildAdapterKey(channel, accountId);
        return adapters.get(key);
    }

    /**
     * Builds adapter key.
     */
    private String buildAdapterKey(String channel, String accountId) {
        String normalizedChannel = channel != null ? channel.trim().toLowerCase() : "";
        String normalizedAccount = accountId != null && !accountId.isBlank() 
                ? accountId.trim().toLowerCase() 
                : "default";
        return normalizedChannel + ":" + normalizedAccount;
    }

    /**
     * Infers default placement.
     */
    private SessionBindingPlacement inferDefaultPlacement(ConversationRef ref) {
        return ref.getConversationId() != null && !ref.getConversationId().isBlank()
                ? SessionBindingPlacement.CURRENT
                : SessionBindingPlacement.CHILD;
    }
}