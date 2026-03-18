package openclaw.lsp.session;

import openclaw.lsp.client.LspClient;
import openclaw.lsp.client.StdioLspClient;
import openclaw.lsp.config.LspServerConfig;
import openclaw.lsp.process.LspProcessManager;
import openclaw.lsp.protocol.InitializeParams;
import openclaw.lsp.protocol.InitializeResult;
import openclaw.sdk.tool.AgentTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages LSP sessions.
 *
 * @author OpenClaw Team
 * @version 2026.3.18
 * @since 2026.3.0
 */
public class LspSessionManager {

    private static final Logger logger = LoggerFactory.getLogger(LspSessionManager.class);

    private final LspProcessManager processManager;
    private final Map<String, LspSession> sessions = new ConcurrentHashMap<>();
    private final LspToolFactory toolFactory = new LspToolFactory();

    public LspSessionManager() {
        this.processManager = new LspProcessManager();
    }

    public LspSessionManager(LspProcessManager processManager) {
        this.processManager = processManager;
    }

    /**
     * Start a new LSP session.
     *
     * @param config the server configuration
     * @return future with the session
     */
    public CompletableFuture<LspSession> startSession(LspServerConfig config) {
        String name = config.getName();

        if (sessions.containsKey(name)) {
            return CompletableFuture.completedFuture(sessions.get(name));
        }

        logger.info("Starting LSP session: {}", name);

        try {
            // Start process
            Process process = processManager.startProcess(config.getCommand(), config.getArgs());

            // Create client
            LspClient client = new StdioLspClient(process);
            client.setRequestTimeout(config.getRequestTimeout());

            // Create session
            LspSession session = new LspSession(name, client);

            // Connect and initialize
            return client.connect()
                    .thenCompose(v -> {
                        InitializeParams params = new InitializeParams();
                        params.setProcessId((int) ProcessHandle.current().pid());
                        params.setRootUri(config.getRootUri());
                        // Set capabilities
                        InitializeParams.ClientCapabilities caps = new InitializeParams.ClientCapabilities();
                        InitializeParams.TextDocumentCapabilities textDoc = new InitializeParams.TextDocumentCapabilities();
                        textDoc.setHover(Map.of("contentFormat", List.of("plaintext", "markdown")));
                        textDoc.setCompletion(Map.of("completionItem", Map.of("snippetSupport", false)));
                        textDoc.setDefinition(Map.of());
                        textDoc.setReferences(Map.of());
                        caps.setTextDocument(textDoc);
                        params.setCapabilities(caps);

                        return client.initialize(params);
                    })
                    .thenApply(result -> {
                        session.setCapabilities(result.getCapabilities());
                        session.setInitialized(true);

                        // Create tools
                        List<AgentTool> tools = toolFactory.createTools(session);
                        for (AgentTool tool : tools) {
                            session.addTool(tool);
                        }

                        sessions.put(name, session);
                        logger.info("LSP session started: {} with {} tools", name, tools.size());
                        return session;
                    });

        } catch (IOException e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Stop a session.
     *
     * @param name the session name
     */
    public void stopSession(String name) {
        LspSession session = sessions.remove(name);
        if (session != null) {
            session.dispose();
            logger.info("LSP session stopped: {}", name);
        }
    }

    /**
     * Get a session by name.
     *
     * @param name the session name
     * @return optional session
     */
    public Optional<LspSession> getSession(String name) {
        return Optional.ofNullable(sessions.get(name));
    }

    /**
     * Get all sessions.
     *
     * @return collection of sessions
     */
    public Collection<LspSession> getAllSessions() {
        return new ArrayList<>(sessions.values());
    }

    /**
     * Get all tools from all sessions.
     *
     * @return list of tools
     */
    public List<AgentTool> getAllTools() {
        List<AgentTool> allTools = new ArrayList<>();
        for (LspSession session : sessions.values()) {
            allTools.addAll(session.getTools());
        }
        return allTools;
    }

    /**
     * Stop all sessions.
     */
    public void stopAll() {
        logger.info("Stopping all LSP sessions");
        for (String name : new ArrayList<>(sessions.keySet())) {
            stopSession(name);
        }
        processManager.stopAll();
    }

    /**
     * Check if a session exists.
     *
     * @param name the session name
     * @return true if exists
     */
    public boolean hasSession(String name) {
        return sessions.containsKey(name);
    }
}
