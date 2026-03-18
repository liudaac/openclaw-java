package openclaw.lsp.protocol;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * LSP Initialize Parameters.
 *
 * @author OpenClaw Team
 * @version 2026.3.18
 * @since 2026.3.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InitializeParams {

    @JsonProperty("processId")
    private Integer processId;

    @JsonProperty("clientInfo")
    private ClientInfo clientInfo;

    @JsonProperty("locale")
    private String locale;

    @JsonProperty("rootPath")
    private String rootPath;

    @JsonProperty("rootUri")
    private String rootUri;

    @JsonProperty("capabilities")
    private ClientCapabilities capabilities;

    @JsonProperty("trace")
    private String trace;

    @JsonProperty("workspaceFolders")
    private Object workspaceFolders;

    public InitializeParams() {
    }

    public InitializeParams(Integer processId, String rootUri, ClientCapabilities capabilities) {
        this.processId = processId;
        this.rootUri = rootUri;
        this.capabilities = capabilities;
    }

    public Integer getProcessId() {
        return processId;
    }

    public void setProcessId(Integer processId) {
        this.processId = processId;
    }

    public ClientInfo getClientInfo() {
        return clientInfo;
    }

    public void setClientInfo(ClientInfo clientInfo) {
        this.clientInfo = clientInfo;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public String getRootPath() {
        return rootPath;
    }

    public void setRootPath(String rootPath) {
        this.rootPath = rootPath;
    }

    public String getRootUri() {
        return rootUri;
    }

    public void setRootUri(String rootUri) {
        this.rootUri = rootUri;
    }

    public ClientCapabilities getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(ClientCapabilities capabilities) {
        this.capabilities = capabilities;
    }

    public String getTrace() {
        return trace;
    }

    public void setTrace(String trace) {
        this.trace = trace;
    }

    public Object getWorkspaceFolders() {
        return workspaceFolders;
    }

    public void setWorkspaceFolders(Object workspaceFolders) {
        this.workspaceFolders = workspaceFolders;
    }

    /**
     * Client information.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ClientInfo {
        @JsonProperty("name")
        private String name;

        @JsonProperty("version")
        private String version;

        public ClientInfo() {
        }

        public ClientInfo(String name, String version) {
            this.name = name;
            this.version = version;
        }

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

    /**
     * Client capabilities.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ClientCapabilities {
        @JsonProperty("textDocument")
        private TextDocumentCapabilities textDocument;

        public TextDocumentCapabilities getTextDocument() {
            return textDocument;
        }

        public void setTextDocument(TextDocumentCapabilities textDocument) {
            this.textDocument = textDocument;
        }
    }

    /**
     * Text document capabilities.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TextDocumentCapabilities {
        @JsonProperty("hover")
        private Map<String, Object> hover;

        @JsonProperty("completion")
        private Map<String, Object> completion;

        @JsonProperty("definition")
        private Map<String, Object> definition;

        @JsonProperty("references")
        private Map<String, Object> references;

        public Map<String, Object> getHover() {
            return hover;
        }

        public void setHover(Map<String, Object> hover) {
            this.hover = hover;
        }

        public Map<String, Object> getCompletion() {
            return completion;
        }

        public void setCompletion(Map<String, Object> completion) {
            this.completion = completion;
        }

        public Map<String, Object> getDefinition() {
            return definition;
        }

        public void setDefinition(Map<String, Object> definition) {
            this.definition = definition;
        }

        public Map<String, Object> getReferences() {
            return references;
        }

        public void setReferences(Map<String, Object> references) {
            this.references = references;
        }
    }
}
