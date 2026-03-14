package openclaw.browser.batch;

/**
 * Result of a single action within a batch.
 *
 * @author OpenClaw Team
 * @version 2026.3.14
 */
public record ActionResult(
    boolean ok,
    String error,      // Error message if failed
    Object result,     // Return value (e.g., evaluate result, screenshot bytes)
    long durationMs    // Execution time in milliseconds
) {
    
    /**
     * Create a successful result.
     */
    public static ActionResult success() {
        return new ActionResult(true, null, null, 0);
    }
    
    /**
     * Create a successful result with return value.
     */
    public static ActionResult success(Object result) {
        return new ActionResult(true, null, result, 0);
    }
    
    /**
     * Create a successful result with duration.
     */
    public static ActionResult success(long durationMs) {
        return new ActionResult(true, null, null, durationMs);
    }
    
    /**
     * Create a successful result with return value and duration.
     */
    public static ActionResult success(Object result, long durationMs) {
        return new ActionResult(true, null, result, durationMs);
    }
    
    /**
     * Create a failed result.
     */
    public static ActionResult failure(String error) {
        return new ActionResult(false, error, null, 0);
    }
    
    /**
     * Create a failed result with duration.
     */
    public static ActionResult failure(String error, long durationMs) {
        return new ActionResult(false, error, null, durationMs);
    }
    
    /**
     * Check if this result has a return value.
     */
    public boolean hasResult() {
        return result != null;
    }
    
    /**
     * Get result as specific type.
     */
    @SuppressWarnings("unchecked")
    public <T> T resultAs(Class<T> type) {
        if (result == null) {
            return null;
        }
        if (type.isInstance(result)) {
            return (T) result;
        }
        throw new ClassCastException("Cannot cast " + result.getClass() + " to " + type);
    }
}
