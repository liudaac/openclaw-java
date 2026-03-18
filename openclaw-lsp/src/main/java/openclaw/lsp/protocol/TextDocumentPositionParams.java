package openclaw.lsp.protocol;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Parameters for text document position requests.
 *
 * @author OpenClaw Team
 * @version 2026.3.18
 * @since 2026.3.0
 */
public class TextDocumentPositionParams {

    @JsonProperty("textDocument")
    private TextDocumentIdentifier textDocument;

    @JsonProperty("position")
    private Position position;

    public TextDocumentPositionParams() {
    }

    public TextDocumentPositionParams(TextDocumentIdentifier textDocument, Position position) {
        this.textDocument = textDocument;
        this.position = position;
    }

    public TextDocumentIdentifier getTextDocument() {
        return textDocument;
    }

    public void setTextDocument(TextDocumentIdentifier textDocument) {
        this.textDocument = textDocument;
    }

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
    }
}
