package openclaw.memory.batch;

import openclaw.memory.embedding.EmbeddingProvider;
import openclaw.memory.embedding.EmbeddingProvider.EmbeddingVector;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

/**
 * Batch embedding processor with concurrency control.
 *
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public class BatchEmbeddingProcessor {

    private final EmbeddingProvider provider;
    private final int maxConcurrency;
    private final int batchSize;
    private final ExecutorService executor;
    private final Semaphore semaphore;

    public BatchEmbeddingProcessor(EmbeddingProvider provider) {
        this(provider, 5, 100);
    }

    public BatchEmbeddingProcessor(EmbeddingProvider provider, int maxConcurrency, int batchSize) {
        this.provider = provider;
        this.maxConcurrency = maxConcurrency;
        this.batchSize = batchSize;
        this.executor = Executors.newFixedThreadPool(maxConcurrency);
        this.semaphore = new Semaphore(maxConcurrency);
    }

    /**
     * Processes a batch of texts for embedding.
     *
     * @param texts the texts to embed
     * @return the embedding results
     */
    public CompletableFuture<BatchResult> processBatch(List<String> texts) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return doProcessBatch(texts);
            } catch (Exception e) {
                throw new BatchProcessingException("Failed to process batch", e);
            }
        }, executor);
    }

    private BatchResult doProcessBatch(List<String> texts) throws InterruptedException {
        List<EmbeddingVector> results = new java.util.ArrayList<>();
        List<BatchError> errors = new java.util.ArrayList<>();

        // Split into sub-batches
        List<List<String>> batches = splitIntoBatches(texts, batchSize);

        List<CompletableFuture<Void>> futures = new java.util.ArrayList<>();

        for (List<String> batch : batches) {
            semaphore.acquire();

            CompletableFuture<Void> future = provider.embedBatch(batch)
                    .thenAccept(vectors -> {
                        synchronized (results) {
                            results.addAll(vectors);
                        }
                    })
                    .exceptionally(throwable -> {
                        synchronized (errors) {
                            errors.add(new BatchError(batch, throwable.getMessage()));
                        }
                        return null;
                    })
                    .whenComplete((v, t) -> semaphore.release());

            futures.add(future);
        }

        // Wait for all batches to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        return new BatchResult(results, errors, texts.size());
    }

    private List<List<String>> splitIntoBatches(List<String> texts, int batchSize) {
        List<List<String>> batches = new java.util.ArrayList<>();
        for (int i = 0; i < texts.size(); i += batchSize) {
            batches.add(texts.subList(i, Math.min(i + batchSize, texts.size())));
        }
        return batches;
    }

    /**
     * Shuts down the processor.
     */
    public void shutdown() {
        executor.shutdown();
    }

    /**
     * Batch processing result.
     *
     * @param embeddings the successful embeddings
     * @param errors the errors
     * @param totalCount the total count
     */
    public record BatchResult(
            List<EmbeddingVector> embeddings,
            List<BatchError> errors,
            int totalCount
    ) {

        /**
         * Gets the success count.
         *
         * @return the count
         */
        public int successCount() {
            return embeddings.size();
        }

        /**
         * Gets the error count.
         *
         * @return the count
         */
        public int errorCount() {
            return errors.size();
        }

        /**
         * Checks if all succeeded.
         *
         * @return true if all succeeded
         */
        public boolean allSucceeded() {
            return errors.isEmpty();
        }
    }

    /**
     * Batch error.
     *
     * @param texts the texts that failed
     * @param error the error message
     */
    public record BatchError(
            List<String> texts,
            String error
    ) {
    }

    /**
     * Batch processing exception.
     */
    public static class BatchProcessingException extends RuntimeException {
        public BatchProcessingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
