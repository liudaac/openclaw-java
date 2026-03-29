package openclaw.memory.store;

/**
 * FTS5 tokenizer types for full-text search
 * Supports CJK (Chinese, Japanese, Korean) text search
 */
public enum FtsTokenizer {
    PORTER("porter"),
    ICU("icu"),
    UNICODE61("unicode61"),
    TRIGRAM("trigram");

    private final String value;

    FtsTokenizer(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
