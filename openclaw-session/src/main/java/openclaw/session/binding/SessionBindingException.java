package openclaw.session.binding;

import java.util.Objects;

/**
 * Exception for session binding errors.
 *
 * @author OpenClaw Team
 * @version 2026.3.21
 * @since 2026.3.21
 */
public class SessionBindingException extends RuntimeException {

    private final SessionBindingErrorCode code;
    private final String channel;
    private final String accountId;
    private final SessionBindingPlacement placement;

    public SessionBindingException(
            SessionBindingErrorCode code,
            String message) {
        this(code, message, null, null, null);
    }

    public SessionBindingException(
            SessionBindingErrorCode code,
            String message,
            String channel,
            String accountId,
            SessionBindingPlacement placement) {
        super(message);
        this.code = Objects.requireNonNull(code, "code cannot be null");
        this.channel = channel;
        this.accountId = accountId;
        this.placement = placement;
    }

    public SessionBindingErrorCode getCode() {
        return code;
    }

    public String getChannel() {
        return channel;
    }

    public String getAccountId() {
        return accountId;
    }

    public SessionBindingPlacement getPlacement() {
        return placement;
    }

    /**
     * Checks if the error is a session binding error.
     *
     * @param error the error to check
     * @return true if it's a SessionBindingException
     */
    public static boolean isSessionBindingError(Throwable error) {
        return error instanceof SessionBindingException;
    }
}
