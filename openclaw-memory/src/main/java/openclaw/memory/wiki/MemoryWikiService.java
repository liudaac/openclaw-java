package openclaw.memory.wiki;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Memory Wiki Service interface.
 *
 * <p>Provides belief-layer digests and public artifacts management.</p>
 *
 * <p>Equivalent to Node.js extensions/memory-wiki</p>
 *
 * @author OpenClaw Team
 * @version 2026.4.8
 */
public interface MemoryWikiService {

    /**
     * Initialize the wiki service.
     *
     * @return completion future
     */
    CompletableFuture<Void> initialize();

    /**
     * Create a belief-layer digest.
     *
     * @param request the digest request
     * @return the created digest
     */
    CompletableFuture<BeliefLayerDigest> createDigest(DigestRequest request);

    /**
     * Get a belief-layer digest by ID.
     *
     * @param digestId the digest ID
     * @return the digest if found
     */
    CompletableFuture<Optional<BeliefLayerDigest>> getDigest(String digestId);

    /**
     * List all digests for a session.
     *
     * @param sessionKey the session key
     * @return list of digests
     */
    CompletableFuture<List<BeliefLayerDigest>> listDigests(String sessionKey);

    /**
     * Create a public artifact.
     *
     * @param request the artifact request
     * @return the created artifact
     */
    CompletableFuture<PublicArtifact> createArtifact(ArtifactRequest request);

    /**
     * Get a public artifact by ID.
     *
     * @param artifactId the artifact ID
     * @return the artifact if found
     */
    CompletableFuture<Optional<PublicArtifact>> getArtifact(String artifactId);

    /**
     * List all artifacts for a session.
     *
     * @param sessionKey the session key
     * @return list of artifacts
     */
    CompletableFuture<List<PublicArtifact>> listArtifacts(String sessionKey);

    /**
     * Query wiki content.
     *
     * @param query the query string
     * @return query results
     */
    CompletableFuture<WikiQueryResult> query(String query);

    /**
     * Compile wiki content.
     *
     * @param request the compile request
     * @return compilation result
     */
    CompletableFuture<CompileResult> compile(CompileRequest request);

    /**
     * Apply wiki changes.
     *
     * @param request the apply request
     * @return apply result
     */
    CompletableFuture<ApplyResult> apply(ApplyRequest request);

    /**
     * Get wiki status.
     *
     * @param sessionKey the session key
     * @return status info
     */
    CompletableFuture<WikiStatus> getStatus(String sessionKey);

    // Request/Response records

    record DigestRequest(
            String sessionKey,
            String content,
            DigestType type,
            java.util.Map<String, Object> metadata
    ) {}

    record ArtifactRequest(
            String sessionKey,
            String name,
            String content,
            ArtifactType type,
            java.util.Map<String, Object> metadata
    ) {}

    record CompileRequest(
            String sessionKey,
            String source,
            CompileOptions options
    ) {}

    record ApplyRequest(
            String sessionKey,
            String changeset,
            ApplyOptions options
    ) {}

    record WikiQueryResult(
            List<WikiDocument> documents,
            int totalCount,
            String query
    ) {}

    record CompileResult(
            boolean success,
            String output,
            List<String> errors
    ) {}

    record ApplyResult(
            boolean success,
            String appliedId,
            Optional<String> error
    ) {}

    record WikiStatus(
            String sessionKey,
            int digestCount,
            int artifactCount,
            long lastUpdate,
            boolean isHealthy
    ) {}

    // Enums

    enum DigestType {
        BELIEF,
        OBSERVATION,
        REFLECTION,
        SUMMARY
    }

    enum ArtifactType {
        DOCUMENT,
        CODE,
        CONFIG,
        DATA
    }

    record CompileOptions(
            boolean strict,
            boolean preserveMetadata
    ) {
        public static CompileOptions defaults() {
            return new CompileOptions(false, true);
        }
    }

    record ApplyOptions(
            boolean dryRun,
            boolean backup
    ) {
        public static ApplyOptions defaults() {
            return new ApplyOptions(false, true);
        }
    }
}
