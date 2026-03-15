package openclaw.agent.context;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default context engine implementation.
 *
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public class DefaultContextEngine implements ContextEngine {

    private final Map<String, ContextSession> sessions = new ConcurrentHashMap<>();
    private ContextConfig config;
    private final List<ContextHook> hooks = new ArrayList<>();

    @Override
    public CompletableFuture<Void> initialize(ContextConfig config) {
        return CompletableFuture.runAsync(() -> {
            this.config = config;
        });
    }

    @Override
    public CompletableFuture<ContextSnapshot> bootstrap(String sessionKey, Map<String, Object> context) {
        return CompletableFuture.supplyAsync(() -> {
            // Apply hooks
            Map<String, Object> currentContext = context;
            for (ContextHook hook : hooks) {
                currentContext = hook.onBootstrap(currentContext);
            }

            ContextSession session = new ContextSession(sessionKey);
            session.metadata.putAll(currentContext);
            sessions.put(sessionKey, session);

            return createSnapshot(session);
        });
    }

    @Override
    public CompletableFuture<ContextSnapshot> ingest(String sessionKey, IngestData data) {
        return CompletableFuture.supplyAsync(() -> {
            ContextSession session = sessions.get(sessionKey);
            if (session == null) {
                throw new IllegalStateException("Session not found: " + sessionKey);
            }

            // Apply hooks
            IngestData currentData = data;
            for (ContextHook hook : hooks) {
                currentData = hook.onIngest(currentData);
            }

            // Add to messages
            ContextMessage message = new ContextMessage(
                    currentData.type().name().toLowerCase(),
                    currentData.content(),
                    estimateTokens(currentData.content())
            );
            session.messages.add(message);

            return createSnapshot(session);
        });
    }

    @Override
    public CompletableFuture<ContextSnapshot> assemble(String sessionKey) {
        return CompletableFuture.supplyAsync(() -> {
            ContextSession session = sessions.get(sessionKey);
            if (session == null) {
                throw new IllegalStateException("Session not found: " + sessionKey);
            }

            ContextSnapshot snapshot = createSnapshot(session);

            // Apply hooks
            ContextSnapshot currentSnapshot = snapshot;
            for (ContextHook hook : hooks) {
                currentSnapshot = hook.onAssemble(currentSnapshot);
            }

            return currentSnapshot;
        });
    }

    @Override
    public CompletableFuture<ContextSnapshot> compact(String sessionKey) {
        return CompletableFuture.supplyAsync(() -> {
            ContextSession session = sessions.get(sessionKey);
            if (session == null) {
                throw new IllegalStateException("Session not found: " + sessionKey);
            }

            // Remove old messages if over limit
            while (session.messages.size() > config.maxContextSize() / 100) {
                session.messages.remove(0);
            }

            ContextSnapshot snapshot = createSnapshot(session);

            // Apply hooks
            ContextSnapshot currentSnapshot = snapshot;
            for (ContextHook hook : hooks) {
                currentSnapshot = hook.onCompact(currentSnapshot);
            }

            return currentSnapshot;
        });
    }

    @Override
    public CompletableFuture<Optional<ContextSnapshot>> getContext(String sessionKey) {
        return CompletableFuture.supplyAsync(() -> {
            ContextSession session = sessions.get(sessionKey);
            if (session == null) {
                return Optional.empty();
            }
            return Optional.of(createSnapshot(session));
        });
    }

    @Override
    public CompletableFuture<Void> clearContext(String sessionKey) {
        return CompletableFuture.runAsync(() -> {
            sessions.remove(sessionKey);
        });
    }

    public void addHook(ContextHook hook) {
        hooks.add(hook);
    }

    private ContextSnapshot createSnapshot(ContextSession session) {
        int tokenCount = session.messages.stream()
                .mapToInt(ContextMessage::tokenCount)
                .sum();

        return new ContextSnapshot(
                session.sessionKey,
                new ArrayList<>(session.messages),
                new ConcurrentHashMap<>(session.metadata),
                tokenCount,
                System.currentTimeMillis()
        );
    }

    private int estimateTokens(String text) {
        // Rough estimate: 1 token ~= 4 characters
        return text.length() / 4;
    }

    private static class ContextSession {
        final String sessionKey;
        final List<ContextMessage> messages = new ArrayList<>();
        final Map<String, Object> metadata = new ConcurrentHashMap<>();

        ContextSession(String sessionKey) {
            this.sessionKey = sessionKey;
        }
    }
}
