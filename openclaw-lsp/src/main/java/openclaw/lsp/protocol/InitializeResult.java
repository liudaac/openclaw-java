package openclaw.lsp.protocol;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * LSP Initialize Result.
 *
 * @author OpenClaw Team
 * @version 2026.3.18
 * @since 2026.3.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InitializeResult {

    @JsonProperty("capabilities")
    private ServerCapabilities capabilities;

    @JsonProperty("serverInfo")
    private ServerInfo serverInfo;

    public InitializeResult() {
    }

    public InitializeResult(ServerCapabilities capabilities) {
        this.capabilities = capabilities;
    }

    public ServerCapabilities getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(ServerCapabilities capabilities) {
        this.capabilities = capabilities;
    }

    public ServerInfo getServerInfo() {
        return serverInfo;
    }

    public void setServerInfo(ServerInfo serverInfo) {
        this.serverInfo = serverInfo;
    }

    /**
     * Server capabilities.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ServerCapabilities {
        @JsonProperty("hoverProvider")
        private Boolean hoverProvider;

        @JsonProperty("completionProvider")
        private CompletionProvider completionProvider;

        @JsonProperty("definitionProvider")
        private Boolean definitionProvider;

        @JsonProperty("referencesProvider")
        private Boolean referencesProvider;

        @JsonProperty("diagnosticProvider")
        private DiagnosticProvider diagnosticProvider;

        public Boolean getHoverProvider() {
            return hoverProvider;
        }

        public void setHoverProvider(Boolean hoverProvider) {
            this.hoverProvider = hoverProvider;
        }

        public CompletionProvider getCompletionProvider() {
            return completionProvider;
        }

        public void setCompletionProvider(CompletionProvider completionProvider) {
            this.completionProvider = completionProvider;
        }

        public Boolean getDefinitionProvider() {
            return definitionProvider;
        }

        public void setDefinitionProvider(Boolean definitionProvider) {
            this.definitionProvider = definitionProvider;
        }

        public Boolean getReferencesProvider() {
            return referencesProvider;
        }

        public void setReferencesProvider(Boolean referencesProvider) {
            this.referencesProvider = referencesProvider;
        }

        public DiagnosticProvider getDiagnosticProvider() {
            return diagnosticProvider;
        }

        public void setDiagnosticProvider(DiagnosticProvider diagnosticProvider) {
            this.diagnosticProvider = diagnosticProvider;
        }
    }

    /**
     * Completion provider options.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CompletionProvider {
        @JsonProperty("triggerCharacters")
        private String[] triggerCharacters;

        public String[] getTriggerCharacters() {
            return triggerCharacters;
        }

        public void setTriggerCharacters(String[] triggerCharacters) {
            this.triggerCharacters = triggerCharacters;
        }
    }

    /**
     * Diagnostic provider options.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DiagnosticProvider {
        @JsonProperty("identifier")
        private String identifier;

        @JsonProperty("interFileDependencies")
        private Boolean interFileDependencies;

        @JsonProperty("workspaceDiagnostics")
        private Boolean workspaceDiagnostics;

        public String getIdentifier() {
            return identifier;
        }

        public void setIdentifier(String identifier) {
            this.identifier = identifier;
        }

        public Boolean getInterFileDependencies() {
            return interFileDependencies;
        }

        public void setInterFileDependencies(Boolean interFileDependencies) {
            this.interFileDependencies = interFileDependencies;
        }

        public Boolean getWorkspaceDiagnostics() {
            return workspaceDiagnostics;
        }

        public void setWorkspaceDiagnostics(Boolean workspaceDiagnostics) {
            this.workspaceDiagnostics = workspaceDiagnostics;
        }
    }

    /**
     * Server information.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ServerInfo {
        @JsonProperty("name")
        private String name;

        @JsonProperty("version")
        private String version;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }
    }
}
