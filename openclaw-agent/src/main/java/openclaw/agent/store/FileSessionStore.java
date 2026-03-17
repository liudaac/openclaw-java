package openclaw.agent.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import openclaw.agent.AcpProtocol.AgentMessage;
import openclaw.agent.AcpProtocol.AgentSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * File-based session store implementation.
 *
 * <p>Persistent storage using JSON files. Suitable for simple deployments.</p>
 *
 * @author OpenClaw Team
 * @version 2026.3.17
 */
public class FileSessionStore implements SessionStore {

    private static final Logger logger = LoggerFactory.getLogger(FileSessionStore.class);

    private final Path baseDir;
    private final String extension;
    private final ObjectMapper objectMapper;
    private final StoreConfig.SessionConfig config;
    private final ScheduledExecutorService cleanupScheduler;

    public FileSessionStore(StoreConfig.FileConfig fileConfig, StoreConfig.SessionConfig sessionConfig) {
        this.baseDir = Paths.get(fileConfig.getBaseDir());
        this.extension = fileConfig.getExtension();
        this.config = sessionConfig;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.cleanupScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "file-store-cleanup");
            t.setDaemon(true);
            return t;
        });
    }

    @Override
    public CompletableFuture<Void> initialize() {
        return CompletableFuture.runAsync(() -> {
            try {
                Files.createDirectories(baseDir);
                logger.info("Initialized file session store at {}", baseDir);
                
                // Start cleanup scheduler
                if (config.isAutoCleanup()) {
                    startCleanupScheduler();
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to initialize file store", e);
            }
        });
    }

    @Override
    public CompletableFuture<Void> shutdown() {
        return CompletableFuture.runAsync(() -> {
            cleanupScheduler.shutdown();
            logger.info("File session store shutdown");
        });
    }

    @Override
    public CompletableFuture<Void> saveSession(String sessionKey, AgentSession session) {
        return CompletableFuture.runAsync(() -> {
            try {
                Path filePath = getSessionFilePath(sessionKey);
                SessionData data = new SessionData(session, Instant.now());
                objectMapper.writeValue(filePath.toFile(), data);
            } catch (IOException e) {
                throw new RuntimeException("Failed to save session: " + sessionKey, e);
            }
        });
    }

    @Override
    public CompletableFuture<Optional<AgentSession>> getSession(String sessionKey) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Path filePath = getSessionFilePath(sessionKey);
                if (!Files.exists(filePath)) {
                    return Optional.empty();
                }
                
                SessionData data = objectMapper.readValue(filePath.toFile(), SessionData.class);
                
                // Check TTL
                if (isExpired(data.lastActivity)) {
                    Files.deleteIfExists(filePath);
                    return Optional.empty();
                }
                
                return Optional.ofNullable(data.session);
            } catch (IOException e) {
                logger.warn("Failed to read session: {}", sessionKey, e);
                return Optional.empty();
            }
        });
    }

    @Override
    public CompletableFuture<Void> deleteSession(String sessionKey) {
        return CompletableFuture.runAsync(() -> {
            try {
                Path filePath = getSessionFilePath(sessionKey);
                Files.deleteIfExists(filePath);
            } catch (IOException e) {
                throw new RuntimeException("Failed to delete session: " + sessionKey, e);
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> exists(String sessionKey) {
        return CompletableFuture.supplyAsync(() -> {
            Path filePath = getSessionFilePath(sessionKey);
            return Files.exists(filePath);
        });
    }

    @Override
    public CompletableFuture<Void> appendMessage(String sessionKey, AgentMessage message) {
        return getSession(sessionKey).thenCompose(opt -> {
            if (opt.isEmpty()) {
                return CompletableFuture.failedFuture(
                    new IllegalStateException("Session not found: " + sessionKey));
            }
            
            // Read existing messages
            return readMessages(sessionKey).thenCompose(messages -> {
                messages.add(message);
                
                // Trim to max messages
                int maxMessages = config.getMaxMessages();
                if (messages.size() > maxMessages) {
                    messages = messages.subList(messages.size() - maxMessages, messages.size());
                }
                
                // Save messages
                return saveMessages(sessionKey, messages);
            });
        });
    }

    @Override
    public CompletableFuture<List<AgentMessage>> getMessages(String sessionKey, int limit) {
        return readMessages(sessionKey).thenApply(messages -> {
            int start = Math.max(0, messages.size() - limit);
            return messages.subList(start, messages.size());
        });
    }

    @Override
    public CompletableFuture<List<String>> listSessionKeys() {
        return CompletableFuture.supplyAsync(() -> {
            try (Stream<Path> paths = Files.list(baseDir)) {
                return paths
                    .filter(p -> p.toString().endsWith(extension))
                    .map(p -> p.getFileName().toString())
                    .map(n -> n.substring(0, n.length() - extension.length()))
                    .collect(Collectors.toList());
            } catch (IOException e) {
                logger.warn("Failed to list sessions", e);
                return List.of();
            }
        });
    }

    @Override
    public StoreType getStoreType() {
        return StoreType.FILE;
    }

    // Private methods

    private Path getSessionFilePath(String sessionKey) {
        // Sanitize session key for file name
        String safeKey = sessionKey.replaceAll("[^a-zA-Z0-9_-]", "_");
        return baseDir.resolve(safeKey + extension);
    }

    private Path getMessagesFilePath(String sessionKey) {
        String safeKey = sessionKey.replaceAll("[^a-zA-Z0-9_-]", "_");
        return baseDir.resolve(safeKey + ".messages" + extension);
    }

    private CompletableFuture<List<AgentMessage>> readMessages(String sessionKey) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Path filePath = getMessagesFilePath(sessionKey);
                if (!Files.exists(filePath)) {
                    return new ArrayList<>();
                }
                return objectMapper.readValue(filePath.toFile(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, AgentMessage.class));
            } catch (IOException e) {
                logger.warn("Failed to read messages: {}", sessionKey, e);
                return new ArrayList<>();
            }
        });
    }

    private CompletableFuture<Void> saveMessages(String sessionKey, List<AgentMessage> messages) {
        return CompletableFuture.runAsync(() -> {
            try {
                Path filePath = getMessagesFilePath(sessionKey);
                objectMapper.writeValue(filePath.toFile(), messages);
            } catch (IOException e) {
                throw new RuntimeException("Failed to save messages: " + sessionKey, e);
            }
        });
    }

    private boolean isExpired(Instant lastActivity) {
        return lastActivity.plus(config.getTtl()).isBefore(Instant.now());
    }

    private void startCleanupScheduler() {
        cleanupScheduler.scheduleAtFixedRate(
            this::cleanupExpiredSessions,
            config.getCleanupInterval().toMinutes(),
            config.getCleanupInterval().toMinutes(),
            TimeUnit.MINUTES
        );
    }

    private void cleanupExpiredSessions() {
        try {
            listSessionKeys().thenAccept(keys -> {
                for (String key : keys) {
                    getSession(key).thenAccept(opt -> {
                        if (opt.isEmpty()) {
                            // Already expired and deleted
                            return;
                        }
                        // Update last activity to trigger TTL check
                        Path filePath = getSessionFilePath(key);
                        try {
                            SessionData data = objectMapper.readValue(filePath.toFile(), SessionData.class);
                            if (isExpired(data.lastActivity)) {
                                Files.deleteIfExists(filePath);
                                Files.deleteIfExists(getMessagesFilePath(key));
                                logger.debug("Cleaned up expired session: {}", key);
                            }
                        } catch (IOException e) {
                            logger.warn("Failed to cleanup session: {}", key, e);
                        }
                    });
                }
            });
        } catch (Exception e) {
            logger.error("Cleanup failed", e);
        }
    }

    // Inner class for session data
    private static class SessionData {
        public AgentSession session;
        public Instant lastActivity;

        public SessionData() {}

        public SessionData(AgentSession session, Instant lastActivity) {
            this.session = session;
            this.lastActivity = lastActivity;
        }
    }
}