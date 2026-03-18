package openclaw.lsp.protocol;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Position in a text document.
 *
 * @author OpenClaw Team
 * @version 2026.3.18
 * @since 2026.3.0
 */
public class Position {

    @JsonProperty("line")
    private int line;

    @JsonProperty("character")
    private int character;

    public Position() {
    }

    public Position(int line, int character) {
        this.line = line;
        this.character = character;
    }

    public int getLine() {
        return line;
    }

    public void setLine(int line) {
        this.line = line;
    }

    public int getCharacter() {
        return character;
    }

    public void setCharacter(int character) {
        this.character = character;
    }

    @Override
    public String toString() {
        return line + ":" + character;
    }
}
