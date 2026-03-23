package openclaw.sdk.testing;

import java.util.concurrent.Callable;
import java.util.function.Supplier;

/**
 * Helper class for dependency injection testing.
 *
 * <p>Provides utilities for setting up and tearing down test dependencies
 * in a safe and consistent manner.</p>
 *
 * @author OpenClaw Team
 * @version 2026.3.23
 */
public class DITestHelper {

    private DITestHelper() {
        // Utility class
    }

    /**
     * Executes a test with temporary dependencies.
     *
     * @param depsSetter the function to set test dependencies
     * @param depsClearer the function to clear test dependencies
     * @param depsSupplier the function to get current dependencies
     * @param testDeps the test dependencies to use
     * @param test the test to execute
     * @param <D> the dependency type
     * @param <T> the return type
     * @return the test result
     * @throws Exception if the test throws an exception
     */
    public static <D, T> T withDeps(
            java.util.function.Consumer<D> depsSetter,
            Runnable depsClearer,
            Supplier<D> depsSupplier,
            D testDeps,
            Callable<T> test
    ) throws Exception {
        D originalDeps = depsSupplier.get();
        try {
            depsSetter.accept(testDeps);
            return test.call();
        } finally {
            if (originalDeps != null) {
                depsSetter.accept(originalDeps);
            } else {
                depsClearer.run();
            }
        }
    }

    /**
     * Executes a test with temporary dependencies (void return).
     *
     * @param depsSetter the function to set test dependencies
     * @param depsClearer the function to clear test dependencies
     * @param depsSupplier the function to get current dependencies
     * @param testDeps the test dependencies to use
     * @param test the test to execute
     * @param <D> the dependency type
     * @throws Exception if the test throws an exception
     */
    public static <D> void withDeps(
            java.util.function.Consumer<D> depsSetter,
            Runnable depsClearer,
            Supplier<D> depsSupplier,
            D testDeps,
            Runnable test
    ) throws Exception {
        D originalDeps = depsSupplier.get();
        try {
            depsSetter.accept(testDeps);
            test.run();
        } finally {
            if (originalDeps != null) {
                depsSetter.accept(originalDeps);
            } else {
                depsClearer.run();
            }
        }
    }

    /**
     * Creates a builder for test dependency setup.
     *
     * @param <D> the dependency type
     * @return a new builder
     */
    public static <D> TestDepsBuilder<D> builder() {
        return new TestDepsBuilder<>();
    }

    /**
     * Builder for test dependency setup.
     *
     * @param <D> the dependency type
     */
    public static class TestDepsBuilder<D> {
        private java.util.function.Consumer<D> depsSetter;
        private Runnable depsClearer;
        private Supplier<D> depsSupplier;
        private D testDeps;

        public TestDepsBuilder<D> setter(java.util.function.Consumer<D> setter) {
            this.depsSetter = setter;
            return this;
        }

        public TestDepsBuilder<D> clearer(Runnable clearer) {
            this.depsClearer = clearer;
            return this;
        }

        public TestDepsBuilder<D> supplier(Supplier<D> supplier) {
            this.depsSupplier = supplier;
            return this;
        }

        public TestDepsBuilder<D> deps(D deps) {
            this.testDeps = deps;
            return this;
        }

        public <T> T run(Callable<T> test) throws Exception {
            if (depsSetter == null || depsClearer == null || depsSupplier == null) {
                throw new IllegalStateException("Setter, clearer, and supplier must be set");
            }
            return withDeps(depsSetter, depsClearer, depsSupplier, testDeps, test);
        }

        public void run(Runnable test) throws Exception {
            if (depsSetter == null || depsClearer == null || depsSupplier == null) {
                throw new IllegalStateException("Setter, clearer, and supplier must be set");
            }
            withDeps(depsSetter, depsClearer, depsSupplier, testDeps, test);
        }
    }

    /**
     * A simple auto-closeable resource for test dependencies.
     *
     * @param <D> the dependency type
     */
    public static class TestDepsResource<D> implements AutoCloseable {
        private final java.util.function.Consumer<D> depsSetter;
        private final Runnable depsClearer;
        private final D originalDeps;

        public TestDepsResource(
                java.util.function.Consumer<D> depsSetter,
                Runnable depsClearer,
                Supplier<D> depsSupplier,
                D testDeps
        ) {
            this.depsSetter = depsSetter;
            this.depsClearer = depsClearer;
            this.originalDeps = depsSupplier.get();
            depsSetter.accept(testDeps);
        }

        @Override
        public void close() {
            if (originalDeps != null) {
                depsSetter.accept(originalDeps);
            } else {
                depsClearer.run();
            }
        }
    }
}
