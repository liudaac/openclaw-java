package openclaw.cron.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Utility class for normalizing cron job identity fields.
 *
 * <p>Handles migration from legacy "jobId" field to standard "id" field.</p>
 *
 * @author OpenClaw Team
 * @version 2026.4.8
 */
public class JobIdentityNormalizer {

    private static final Logger logger = LoggerFactory.getLogger(JobIdentityNormalizer.class);

    private JobIdentityNormalizer() {
        // Utility class
    }

    /**
     * Normalize job identity in a map.
     * Converts legacy "jobId" field to "id" field if present.
     *
     * @param jobMap the job data map
     * @return the normalized map
     */
    public static Map<String, Object> normalize(Map<String, Object> jobMap) {
        if (jobMap == null) {
            return null;
        }

        // Check for legacy jobId field
        if (jobMap.containsKey("jobId") && !jobMap.containsKey("id")) {
            Object jobId = jobMap.get("jobId");
            if (jobId != null) {
                logger.warn("Legacy 'jobId' field detected in cron job data. " +
                        "Migrating to 'id' field. jobId={}", jobId);
                jobMap.put("id", jobId);
            }
        }

        return jobMap;
    }

    /**
     * Normalize job identity string.
     * Handles cases where the ID might be stored in different formats.
     *
     * @param jobId the job ID string
     * @return the normalized ID
     */
    public static String normalizeId(String jobId) {
        if (jobId == null || jobId.isEmpty()) {
            return jobId;
        }

        // Trim whitespace
        String normalized = jobId.trim();

        // Log if normalization changed the value
        if (!normalized.equals(jobId)) {
            logger.debug("Normalized job ID from '{}' to '{}'", jobId, normalized);
        }

        return normalized;
    }

    /**
     * Check if a map contains legacy jobId field.
     *
     * @param jobMap the job data map
     * @return true if legacy field is present
     */
    public static boolean hasLegacyJobId(Map<String, Object> jobMap) {
        return jobMap != null && jobMap.containsKey("jobId") && !jobMap.containsKey("id");
    }
}
