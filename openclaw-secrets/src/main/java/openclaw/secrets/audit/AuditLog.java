package openclaw.secrets.audit;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Audit log interface for secret operations.
 *
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public interface AuditLog extends AutoCloseable {

    /**
     * Logs an action.
     *
     * @param action the action
     * @param target the target
     * @param details additional details
     */
    void log(Action action, String target, Map<String, Object> details);

    /**
     * Gets audit entries.
     *
     * @param limit the maximum entries
     * @return list of entries
     */
    List<AuditEntry> getEntries(int limit);

    /**
     * Gets audit entries for a target.
     *
     * @param target the target
     * @param limit the maximum entries
     * @return list of entries
     */
    List<AuditEntry> getEntriesForTarget(String target, int limit);

    /**
     * Gets audit entries since a timestamp.
     *
     * @param since the timestamp
     * @param limit the maximum entries
     * @return list of entries
     */
    List<AuditEntry> getEntriesSince(Instant since, int limit);

    /**
     * Clears old entries.
     *
     * @param olderThan entries older than this will be cleared
     */
    void clearOldEntries(Instant olderThan);

    /**
     * Creates a no-op audit log.
     *
     * @return the no-op log
     */
    static AuditLog noop() {
        return new NoopAuditLog();
    }

    /**
     * Audit action.
     */
    enum Action {
        STORE,
        RETRIEVE,
        DELETE,
        ROTATE,
        STORE_CREDENTIAL,
        DELETE_CREDENTIAL,
        CONFIG_READ,
        CONFIG_WRITE
    }

    /**
     * Audit entry.
     *
     * @param timestamp the timestamp
     * @param action the action
     * @param target the target
     * @param details additional details
     * @param success whether the action succeeded
     */
    record AuditEntry(
            Instant timestamp,
            Action action,
            String target,
            Map<String, Object> details,
            boolean success
    ) {
    }

    /**
     * No-op audit log implementation.
     */
    class NoopAuditLog implements AuditLog {
        @Override
        public void log(Action action, String target, Map<String, Object> details) {
            // No-op
        }

        @Override
        public List<AuditEntry> getEntries(int limit) {
            return List.of();
        }

        @Override
        public List<AuditEntry> getEntriesForTarget(String target, int limit) {
            return List.of();
        }

        @Override
        public List<AuditEntry> getEntriesSince(Instant since, int limit) {
            return List.of();
        }

        @Override
        public void clearOldEntries(Instant olderThan) {
            // No-op
        }

        @Override
        public void close() {
            // No-op
        }
    }
}
