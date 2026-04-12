package openclaw.tools;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * OpenClaw tools dependencies for dependency injection.
 *
 * <p>Enables test isolation and dependency mocking for OpenClaw tools.
 * Following the TypeScript openclaw-tools.ts pattern with comprehensive
 * dependency injection support.</p>
 *
 * @author OpenClaw Team
 * @version 2026.4.12
 */
public class OpenClawToolsDeps {

    // Core dependencies
    private Function<CallGatewayOptions, Object> callGateway;
    private Supplier<OpenClawConfig> configSupplier;
    private Function<String, String> workspaceDirResolver;
    
    // Tool-specific dependencies
    private Supplier<Boolean> sandboxEnabledSupplier;
    private Function<String, Boolean> toolEnabledChecker;
    private Supplier<String[]> pluginToolAllowlist;

    /**
     * Creates default dependencies.
     */
    public OpenClawToolsDeps() {
        this.callGateway = opts -> {
            // Default implementation - in production this would delegate to gateway call service
            throw new UnsupportedOperationException("Gateway call not configured");
        };
        this.configSupplier = () -> new OpenClawConfig() {};
        this.workspaceDirResolver = sessionKey -> System.getProperty("user.home") + "/.openclaw/workspace";
        this.sandboxEnabledSupplier = () -> false;
        this.toolEnabledChecker = toolName -> true;
        this.pluginToolAllowlist = () -> new String[0];
    }

    // Getters
    public Function<CallGatewayOptions, Object> getCallGateway() {
        return callGateway;
    }

    public Supplier<OpenClawConfig> getConfigSupplier() {
        return configSupplier;
    }

    public Function<String, String> getWorkspaceDirResolver() {
        return workspaceDirResolver;
    }

    public Supplier<Boolean> getSandboxEnabledSupplier() {
        return sandboxEnabledSupplier;
    }

    public Function<String, Boolean> getToolEnabledChecker() {
        return toolEnabledChecker;
    }

    public Supplier<String[]> getPluginToolAllowlist() {
        return pluginToolAllowlist;
    }

    // Setters for testing
    public void setCallGateway(Function<CallGatewayOptions, Object> callGateway) {
        this.callGateway = callGateway;
    }

    public void setConfigSupplier(Supplier<OpenClawConfig> configSupplier) {
        this.configSupplier = configSupplier;
    }

    public void setWorkspaceDirResolver(Function<String, String> workspaceDirResolver) {
        this.workspaceDirResolver = workspaceDirResolver;
    }

    public void setSandboxEnabledSupplier(Supplier<Boolean> sandboxEnabledSupplier) {
        this.sandboxEnabledSupplier = sandboxEnabledSupplier;
    }

    public void setToolEnabledChecker(Function<String, Boolean> toolEnabledChecker) {
        this.toolEnabledChecker = toolEnabledChecker;
    }

    public void setPluginToolAllowlist(Supplier<String[]> pluginToolAllowlist) {
        this.pluginToolAllowlist = pluginToolAllowlist;
    }

    /**
     * Resets all dependencies to defaults.
     */
    public void resetToDefaults() {
        this.callGateway = opts -> {
            throw new UnsupportedOperationException("Gateway call not configured");
        };
        this.configSupplier = () -> new OpenClawConfig() {};
        this.workspaceDirResolver = sessionKey -> System.getProperty("user.home") + "/.openclaw/workspace";
        this.sandboxEnabledSupplier = () -> false;
        this.toolEnabledChecker = toolName -> true;
        this.pluginToolAllowlist = () -> new String[0];
    }

    // Records

    /**
     * Call gateway options.
     */
    public record CallGatewayOptions(
            String url,
            String method,
            Object params,
            String[] scopes,
            Long timeoutMs
    ) {
        public static CallGatewayOptions of(String url, String method, Object params) {
            return new CallGatewayOptions(url, method, params, new String[]{"read"}, 10000L);
        }
    }

    /**
     * OpenClaw configuration interface.
     */
    public interface OpenClawConfig {
        default String getDefaultModel() {
            return "gpt-4";
        }
        
        default String getWorkspaceDir() {
            return System.getProperty("user.home") + "/.openclaw/workspace";
        }
        
        default boolean isSandboxEnabled() {
            return false;
        }
    }

    /**
     * Testing utilities for dependency injection.
     */
    public static class Testing {
        private static final ThreadLocal<OpenClawToolsDeps> testDeps = new ThreadLocal<>();

        /**
         * Sets dependencies for tests.
         *
         * @param deps the dependencies to use, or null to reset
         */
        public static void setDepsForTest(OpenClawToolsDeps deps) {
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
        public static OpenClawToolsDeps getTestDeps() {
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
