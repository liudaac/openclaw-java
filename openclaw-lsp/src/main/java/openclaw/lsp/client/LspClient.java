package openclaw.lsp.client;

import openclaw.lsp.protocol.InitializeParams;
import openclaw.lsp.protocol.InitializeResult;
import openclaw.lsp.protocol.JsonRpcMessage;
import openclaw.lsp.protocol.ServerCapabilities;

import java.util.concurrent.CompletableFuture;

/**
 * LSP Client interface.
 * Defines the contract for LSP clients.
 *
 * @author OpenClaw Team
 * @version 2026.3.18
 * @since 2026.3.0
 */
public interface LspClient {

    /**
     * Connect to the LSP server.
     *
     * @return future that completes when connected
     */
    CompletableFuture<Void> connect();

    /**
     * Disconnect from the LSP server.
     *
     * @return future that completes when disconnected
     */
    CompletableFuture<Void> disconnect();

    /**
     * Check if client is connected.
     *
     * @return true if connected
     */
    boolean isConnected();

    /**
     * Send a request to the LSP server.
     *
     * @param <T> the response type
     * @param method the method name
     * @param params the parameters
     * @param responseType the response class
     * @return future with the response
     */
    <T> CompletableFuture<T> sendRequest(String method, Object params, Class<T> responseType);

    /**
     * Send a notification to the LSP server.
     *
     * @param method the method name
     * @param params the parameters
     */
    void sendNotification(String method, Object params);

    /**
     * Initialize the client.
     *
     * @param params the initialize parameters
     * @return future with the initialize result
     */
    CompletableFuture<InitializeResult> initialize(InitializeParams params);

    /**
     * Shutdown the client.
     *
     * @return future that completes when shutdown
     */
    CompletableFuture<Void> shutdown();

    /**
     * Send exit notification.
     */
    void exit();

    /**
     * Get server capabilities.
     *
     * @return the server capabilities
     */
    ServerCapabilities getCapabilities();

    /**
     * Set server capabilities.
     *
     * @param capabilities the capabilities
     */
    void setCapabilities(ServerCapabilities capabilities);

    /**
     * Check if client is initialized.
     *
     * @return true if initialized
     */
    boolean isInitialized();

    /**
     * Set request timeout.
     *
     * @param timeoutMillis timeout in milliseconds
     */
    void setRequestTimeout(long timeoutMillis);

    /**
     * Get request timeout.
     *
     * @return timeout in milliseconds
     */
    long getRequestTimeout();
}
