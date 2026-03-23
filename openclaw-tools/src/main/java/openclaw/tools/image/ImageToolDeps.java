package openclaw.tools.image;

import java.net.http.HttpClient;
import java.nio.file.Path;
import java.util.function.Supplier;

/**
 * Image tool dependencies for dependency injection.
 *
 * <p>Enables test isolation and dependency mocking for image generation.</p>
 *
 * @author OpenClaw Team
 * @version 2026.3.23
 */
public class ImageToolDeps {

    private Supplier<HttpClient> httpClientSupplier;
    private Supplier<Path> imageDirSupplier;
    private Supplier<String> apiKeySupplier;

    /**
     * Creates default dependencies.
     */
    public ImageToolDeps() {
        this.httpClientSupplier = () -> HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(30))
                .build();
        this.imageDirSupplier = () -> Path.of("/tmp/images");
        this.apiKeySupplier = () -> System.getenv("OPENAI_API_KEY");
    }

    // Getters
    public Supplier<HttpClient> getHttpClientSupplier() {
        return httpClientSupplier;
    }

    public Supplier<Path> getImageDirSupplier() {
        return imageDirSupplier;
    }

    public Supplier<String> getApiKeySupplier() {
        return apiKeySupplier;
    }

    // Setters for testing
    public void setHttpClientSupplier(Supplier<HttpClient> httpClientSupplier) {
        this.httpClientSupplier = httpClientSupplier;
    }

    public void setImageDirSupplier(Supplier<Path> imageDirSupplier) {
        this.imageDirSupplier = imageDirSupplier;
    }

    public void setApiKeySupplier(Supplier<String> apiKeySupplier) {
        this.apiKeySupplier = apiKeySupplier;
    }

    /**
     * Resets all dependencies to defaults.
     */
    public void resetToDefaults() {
        this.httpClientSupplier = () -> HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(30))
                .build();
        this.imageDirSupplier = () -> Path.of("/tmp/images");
        this.apiKeySupplier = () -> System.getenv("OPENAI_API_KEY");
    }

    /**
     * Testing utilities for dependency injection.
     */
    public static class Testing {
        private static final ThreadLocal<ImageToolDeps> testDeps = new ThreadLocal<>();

        /**
         * Sets dependencies for tests.
         *
         * @param deps the dependencies to use, or null to reset
         */
        public static void setDepsForTest(ImageToolDeps deps) {
            if (deps != null) {
                testDeps.set(deps);
            } else {
                testDeps.remove();
            }
        }

        /**
         * Gets test dependencies.
         *
         * @return the test dependencies, or null if not set
         */
        public static ImageToolDeps getTestDeps() {
            return testDeps.get();
        }

        /**
         * Clears test dependencies.
         */
        public static void clearTestDeps() {
            testDeps.remove();
        }
    }
}
