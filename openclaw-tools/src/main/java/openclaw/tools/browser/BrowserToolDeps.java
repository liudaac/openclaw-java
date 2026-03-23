package openclaw.tools.browser;

import openclaw.browser.BrowserService;
import openclaw.security.ssrf.FetchGuard;

import java.nio.file.Path;
import java.util.function.Supplier;

/**
 * Browser tool dependencies for dependency injection.
 *
 * <p>Enables test isolation and dependency mocking for browser automation.</p>
 *
 * @author OpenClaw Team
 * @version 2026.3.23
 */
public class BrowserToolDeps {

    private Supplier<BrowserService> browserServiceSupplier;
    private Supplier<FetchGuard> fetchGuardSupplier;
    private Supplier<Path> screenshotDirSupplier;

    /**
     * Creates default dependencies.
     */
    public BrowserToolDeps() {
        this.browserServiceSupplier = null; // Will be injected by Spring
        this.fetchGuardSupplier = FetchGuard::new;
        this.screenshotDirSupplier = () -> Path.of("/tmp/browser-screenshots");
    }

    // Getters
    public Supplier<BrowserService> getBrowserServiceSupplier() {
        return browserServiceSupplier;
    }

    public Supplier<FetchGuard> getFetchGuardSupplier() {
        return fetchGuardSupplier;
    }

    public Supplier<Path> getScreenshotDirSupplier() {
        return screenshotDirSupplier;
    }

    // Setters for testing
    public void setBrowserServiceSupplier(Supplier<BrowserService> browserServiceSupplier) {
        this.browserServiceSupplier = browserServiceSupplier;
    }

    public void setFetchGuardSupplier(Supplier<FetchGuard> fetchGuardSupplier) {
        this.fetchGuardSupplier = fetchGuardSupplier;
    }

    public void setScreenshotDirSupplier(Supplier<Path> screenshotDirSupplier) {
        this.screenshotDirSupplier = screenshotDirSupplier;
    }

    /**
     * Resets all dependencies to defaults.
     */
    public void resetToDefaults() {
        this.browserServiceSupplier = null;
        this.fetchGuardSupplier = FetchGuard::new;
        this.screenshotDirSupplier = () -> Path.of("/tmp/browser-screenshots");
    }

    /**
     * Testing utilities for dependency injection.
     */
    public static class Testing {
        private static final ThreadLocal<BrowserToolDeps> testDeps = new ThreadLocal<>();

        /**
         * Sets dependencies for tests.
         *
         * @param deps the dependencies to use, or null to reset
         */
        public static void setDepsForTest(BrowserToolDeps deps) {
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
        public static BrowserToolDeps getTestDeps() {
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
