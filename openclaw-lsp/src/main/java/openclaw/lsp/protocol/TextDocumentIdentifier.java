package openclaw.lsp.protocol;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Text document identifier.
 *
 * @author OpenClaw Team
 * @version 2026.3.18
 * @since 2026.3.0
 */
public class TextDocumentIdentifier {

    @JsonProperty("uri")
    private String uri;

    public TextDocumentIdentifier() {
    }

    public TextDocumentIdentifier(String uri) {
        this.uri = uri;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    @Override
    public String toString() {
        return uri;
    }
}
