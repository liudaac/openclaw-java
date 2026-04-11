package openclaw.tools.lsp;

import openclaw.lsp.session.LspSession;
import openclaw.lsp.session.LspSessionManager;
import openclaw.lsp.tool.*;
import openclaw.sdk.tool.AgentTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * LSP Tool Configuration.
 * Registers LSP tools when LSP support is available.
 *
 * @author OpenClaw Team
 * @version 2026.4.2
 */
@Configuration
@ConditionalOnClass(LspSessionManager.class)
public class LspToolConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(LspToolConfiguration.class);

    @Bean
    public LspSessionManager lspSessionManager() {
        logger.info("Initializing LSP Session Manager");
        return new LspSessionManager();
    }

    @Bean
    public LspToolRegistry lspToolRegistry(LspSessionManager sessionManager) {
        logger.info("Initializing LSP Tool Registry");
        return new LspToolRegistry(sessionManager);
    }

    /**
     * LSP Tool Registry - manages LSP tool instances.
     */
    public static class LspToolRegistry {
        
        private final LspSessionManager sessionManager;

        public LspToolRegistry(LspSessionManager sessionManager) {
            this.sessionManager = sessionManager;
        }

        /**
         * Get all LSP tools for a session.
         */
        public List<AgentTool> getToolsForSession(String sessionId) {
            LspSession session = sessionManager.getSession(sessionId).orElse(null);
            if (session == null) {
                // Return default session tools (first available)
                session = sessionManager.getAllSessions().stream().findFirst().orElse(null);
            }
            
            if (session == null) {
                return List.of();
            }

            return List.of(
                new LspHoverTool(session),
                new LspDefinitionTool(session),
                new LspCompletionTool(session),
                new LspReferencesTool(session),
                new LspDiagnosticsTool(session)
            );
        }

        /**
         * Get default LSP tools.
         */
        public List<AgentTool> getDefaultTools() {
            LspSession session = sessionManager.getAllSessions().stream().findFirst().orElse(null);
            if (session == null) {
                logger.warn("No default LSP session available");
                return List.of();
            }

            return getToolsForSession(session.getServerName());
        }
    }
}
