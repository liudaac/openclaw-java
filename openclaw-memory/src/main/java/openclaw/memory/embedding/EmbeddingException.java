package openclaw.memory.embedding;

/**
 * Exception thrown during embedding operations.
 *
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public class EmbeddingException extends RuntimeException {

    public EmbeddingException(String message) {
        super(message);
    }

    public EmbeddingException(String message, Throwable cause) {
        super(message, cause);
    }

    public EmbeddingException(Throwable cause) {
        super(cause);
    }
}
