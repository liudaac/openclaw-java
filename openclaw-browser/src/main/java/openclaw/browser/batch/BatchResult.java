package openclaw.browser.batch;

import java.util.List;

/**
 * Result of a batch operation.
 *
 * @author OpenClaw Team
 * @version 2026.3.14
 */
public record BatchResult(
    List<ActionResult> results,
    int completedCount,
    int failedCount,
    long totalDurationMs
) {
    
    /**
     * Check if all actions succeeded.
     */
    public boolean allSucceeded() {
        return failedCount == 0;
    }
    
    /**
     * Check if any action succeeded.
     */
    public boolean anySucceeded() {
        return completedCount > 0;
    }
    
    /**
     * Get the first error message, if any.
     */
    public String firstError() {
        return results.stream()
            .filter(r -> !r.ok())
            .map(ActionResult::error)
            .findFirst()
            .orElse(null);
    }
    
    /**
     * Get all error messages.
     */
    public List<String> allErrors() {
        return results.stream()
            .filter(r -> !r.ok())
            .map(ActionResult::error)
            .toList();
    }
    
    /**
     * Get result at specific index.
     */
    public ActionResult get(int index) {
        if (index < 0 || index >= results.size()) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + results.size());
        }
        return results.get(index);
    }
    
    /**
     * Get the last result.
     */
    public ActionResult last() {
        if (results.isEmpty()) {
            throw new IllegalStateException("No results available");
        }
        return results.get(results.size() - 1);
    }
}
