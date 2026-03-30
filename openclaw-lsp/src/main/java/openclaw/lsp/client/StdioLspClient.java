package openclaw.lsp.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import openclaw.lsp.protocol.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * LSP Client implementation using stdio.
 *
 * @author OpenClaw Team
 * @version 2026.3.18
 * @since 2026.3.0
 */
public class StdioLspClient implements LspClient {

    private static final Logger logger = LoggerFactory.getLogger(StdioLspClient.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final Process process;
    private final BufferedReader reader;
    private final BufferedWriter writer;
    private final AtomicInteger requestId = new AtomicInteger(0);
    private final Map<Integer, CompletableFuture<JsonRpcMessage>> pendingRequests = new ConcurrentHashMap<>();
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    private volatile boolean connected = false;
    private volatile boolean initialized = false;
    private volatile InitializeResult.ServerCapabilities capabilities;
    private volatile long requestTimeout = 10000; // 10 seconds default

    private final StringBuilder readBuffer = new StringBuilder();

    public StdioLspClient(Process process) {
        this.process = process;
        this.reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
        this.writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
    }

    @Override
    public CompletableFuture<Void> connect() {
        return CompletableFuture.runAsync(() -> {
            connected = true;
            startReading();
            logger.info("LSP client connected");
        }, executor);
    }

    @Override
    public CompletableFuture<Void> disconnect() {
        return CompletableFuture.runAsync(() -> {
            connected = false;
            executor.shutdown();
            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
            logger.info("LSP client disconnected");
        });
    }

    @Override
    public boolean isConnected() {
        return connected && process.isAlive();
    }

    @Override
    public <T> CompletableFuture<T> sendRequest(String method, Object params, Class<T> responseType) {
        if (!isConnected()) {
            return CompletableFuture.failedFuture(new IllegalStateException("Client not connected"));
        }

        int id = requestId.incrementAndGet();
        JsonRpcMessage request = new JsonRpcMessage(id, method, params);
        String encoded = JsonRpcProtocol.encode(request);

        CompletableFuture<JsonRpcMessage> future = new CompletableFuture<>();
        pendingRequests.put(id, future);

        // Timeout
        executor.schedule(() -> {
            CompletableFuture<JsonRpcMessage> removed = pendingRequests.remove(id);
            if (removed != null && !removed.isDone()) {
                removed.completeExceptionally(new TimeoutException("Request timeout: " + method));
            }
        }, requestTimeout, TimeUnit.MILLISECONDS);

        try {
            writer.write(encoded);
            writer.flush();
            logger.debug("Sent request: id={}, method={}", id, method);
        } catch (IOException e) {
            pendingRequests.remove(id);
            return CompletableFuture.failedFuture(e);
        }

        return future.thenApply(response -> {
            if (response.getError() != null) {
                throw new RuntimeException("LSP error: " + response.getError().getMessage());
            }
            try {
                return objectMapper.convertValue(response.getResult(), responseType);
            } catch (Exception e) {
                throw new RuntimeException("Failed to convert response", e);
            }
        });
    }

    @Override
    public void sendNotification(String method, Object params) {
        if (!isConnected()) {
            logger.warn("Cannot send notification, client not connected");
            return;
        }

        JsonRpcMessage notification = new JsonRpcMessage(method, params);
        String encoded = JsonRpcProtocol.encode(notification);

        try {
            writer.write(encoded);
            writer.flush();
            logger.debug("Sent notification: method={}", method);
        } catch (IOException e) {
            logger.error("Failed to send notification", e);
        }
    }

    @Override
    public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
        return sendRequest("initialize", params, InitializeResult.class)
                .thenApply(result -> {
                    this.capabilities = result.getCapabilities();
                    this.initialized = true;
                    // Send initialized notification
                    sendNotification("initialized", new Object());
                    return result;
                });
    }

    @Override
    public CompletableFuture<Void> shutdown() {
        return sendRequest("shutdown", null, Object.class)
                .thenApply(result -> null);
    }

    @Override
    public void exit() {
        sendNotification("exit", null);
    }

    @Override
    public InitializeResult.ServerCapabilities getCapabilities() {
        return capabilities;
    }

    @Override
    public void setCapabilities(InitializeResult.ServerCapabilities capabilities) {
        this.capabilities = capabilities;
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }

    @Override
    public void setRequestTimeout(long timeoutMillis) {
        this.requestTimeout = timeoutMillis;
    }

    @Override
    public long getRequestTimeout() {
        return requestTimeout;
    }

    private void startReading() {
        executor.submit(() -> {
            try {
                char[] buffer = new char[8192];
                int read;
                while (connected && (read = reader.read(buffer)) != -1) {
                    readBuffer.append(buffer, 0, read);
                    processBuffer();
                }
            } catch (IOException e) {
                if (connected) {
                    logger.error("Error reading from LSP server", e);
                }
            }
        });
    }

    private void processBuffer() {
        JsonRpcProtocol.DecodeResult result = JsonRpcProtocol.decode(readBuffer.toString());

        for (JsonRpcMessage message : result.getMessages()) {
            handleMessage(message);
        }

        readBuffer.setLength(0);
        readBuffer.append(result.getRemaining());
    }

    private void handleMessage(JsonRpcMessage message) {
        if (message.isResponse()) {
            Integer id = message.getId();
            if (id != null) {
                CompletableFuture<JsonRpcMessage> future = pendingRequests.remove(id);
                if (future != null) {
                    future.complete(message);
                }
            }
        } else if (message.isNotification()) {
            logger.debug("Received notification: method={}", message.getMethod());
            // Handle notifications if needed
        }
    }
}
