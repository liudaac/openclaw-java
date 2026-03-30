package openclaw.lsp.session;

import openclaw.lsp.client.LspClient;
import openclaw.lsp.protocol.InitializeResult;
import openclaw.sdk.tool.AgentTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * LSP Session.
 * Manages a connection to an LSP server and associated tools.
 *
 * @author OpenClaw Team
 * @version 2026.3.18
 * @since 2026.3.0
 */
public class LspSession {

    private static final Logger logger = LoggerFactory.getLogger(LspSession.class);

    private final String serverName;
    private final LspClient client;
    private InitializeResult.ServerCapabilities capabilities;
    private final List<AgentTool> tools = new ArrayList<>();
    private volatile boolean initialized = false;

    public LspSession(String serverName, LspClient client) {
        this.serverName = serverName;
        this.client = client;
    }

    /**
     * Get the server name.
     *
     * @return the server name
     */
    public String getServerName() {
        return serverName;
    }

    /**
     * Get the LSP client.
     *
     * @return the client
     */
    public LspClient getClient() {
        return client;
    }

    /**
     * Get server capabilities.
     *
     * @return the capabilities
     */
    public InitializeResult.ServerCapabilities getCapabilities() {
        return capabilities;
    }

    /**
     * Set server capabilities.
     *
     * @param capabilities the capabilities
     */
    public void setCapabilities(InitializeResult.ServerCapabilities capabilities) {
        this.capabilities = capabilities;
    }

    /**
     * Check if session is initialized.
     *
     * @return true if initialized
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * Mark session as initialized.
     *
     * @param initialized the initialized state
     */
    public void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }

    /**
     * Add a tool to this session.
     *
     * @param tool the tool to add
     */
    public void addTool(AgentTool tool) {
        tools.add(tool);
        logger.debug("Added tool {} to session {}", tool.getName(), serverName);
    }

    /**
     * Get all tools for this session.
     *
     * @return the tools
     */
    public List<AgentTool> getTools() {
        return new ArrayList<>(tools);
    }

    /**
     * Check if a capability is supported.
     *
     * @param capability the capability name
     * @return true if supported
     */
    public boolean hasCapability(String capability) {
        if (capabilities == null) {
            return false;
        }

        return switch (capability) {
            case "hover" -> capabilities.getHoverProvider() != null && capabilities.getHoverProvider();
            case "completion" -> capabilities.getCompletionProvider() != null;
            case "definition" -> capabilities.getDefinitionProvider() != null && capabilities.getDefinitionProvider();
            case "references" -> capabilities.getReferencesProvider() != null && capabilities.getReferencesProvider();
            case "diagnostics" -> capabilities.getDiagnosticProvider() != null;
            default -> false;
        };
    }

    /**
     * Dispose the session.
     */
    public void dispose() {
        logger.info("Disposing LSP session: {}", serverName);
        tools.clear();
        if (client != null) {
            try {
                client.shutdown().thenRun(() -> {
                    client.exit();
                    client.disconnect();
                }).get();
            } catch (Exception e) {
                logger.warn("Error during session disposal", e);
            }
        }
    }

    @Override
    public String toString() {
        return "LspSession{" +
                "serverName='" + serverName + '\'' +
                ", initialized=" + initialized +
                ", tools=" + tools.size() +
                '}';
    }
}
