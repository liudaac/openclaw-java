package openclaw.lsp.session;

import openclaw.lsp.protocol.ServerCapabilities;
import openclaw.lsp.tool.*;
import openclaw.sdk.tool.AgentTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory for creating LSP tools based on server capabilities.
 *
 * @author OpenClaw Team
 * @version 2026.3.18
 * @since 2026.3.0
 */
public class LspToolFactory {

    private static final Logger logger = LoggerFactory.getLogger(LspToolFactory.class);

    /**
     * Create tools based on session capabilities.
     *
     * @param session the LSP session
     * @return list of tools
     */
    public List<AgentTool> createTools(LspSession session) {
        List<AgentTool> tools = new ArrayList<>();
        ServerCapabilities caps = session.getCapabilities();

        if (caps == null) {
            logger.warn("No capabilities for session: {}", session.getServerName());
            return tools;
        }

        // Hover
        if (Boolean.TRUE.equals(caps.getHoverProvider())) {
            tools.add(new LspHoverTool(session));
            logger.debug("Created hover tool for {}", session.getServerName());
        }

        // Completion
        if (caps.getCompletionProvider() != null) {
            tools.add(new LspCompletionTool(session));
            logger.debug("Created completion tool for {}", session.getServerName());
        }

        // Definition
        if (Boolean.TRUE.equals(caps.getDefinitionProvider())) {
            tools.add(new LspDefinitionTool(session));
            logger.debug("Created definition tool for {}", session.getServerName());
        }

        // References
        if (Boolean.TRUE.equals(caps.getReferencesProvider())) {
            tools.add(new LspReferencesTool(session));
            logger.debug("Created references tool for {}", session.getServerName());
        }

        // Diagnostics
        if (caps.getDiagnosticProvider() != null) {
            tools.add(new LspDiagnosticsTool(session));
            logger.debug("Created diagnostics tool for {}", session.getServerName());
        }

        logger.info("Created {} tools for LSP session {}", tools.size(), session.getServerName());
        return tools;
    }
}
