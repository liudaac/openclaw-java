package openclaw.agent.pi;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Provider runtime hooks for customizing provider behavior at runtime.
 *
 * <p>Enables dynamic customization of provider behavior without modifying
 * the provider implementation. Useful for testing, A/B testing, and
 * runtime configuration changes.</p>
 *
 * @author OpenClaw Team
 * @version 2026.3.23
 */
public interface ProviderRuntimeHooks {

    /**
     * Called before provider initialization.
     *
     * @param providerId the provider ID
     * @param config the provider configuration
     * @return the modified configuration, or null to use original
     */
    default CompletableFuture<Map<String, Object>> beforeInit(
            String providerId,
            Map<String, Object> config
    ) {
        return CompletableFuture.completedFuture(config);
    }

    /**
     * Called after provider initialization.
     *
     * @param providerId the provider ID
     * @param result the initialization result
     * @return the modified result, or null to use original
     */
    default CompletableFuture<Map<String, Object>> afterInit(
            String providerId,
            Map<String, Object> result
    ) {
        return CompletableFuture.completedFuture(result);
    }

    /**
     * Called before a provider method invocation.
     *
     * @param providerId the provider ID
     * @param method the method name
     * @param params the method parameters
     * @return the modified parameters, or null to use original
     */
    default CompletableFuture<Map<String, Object>> beforeInvoke(
            String providerId,
            String method,
            Map<String, Object> params
    ) {
        return CompletableFuture.completedFuture(params);
    }

    /**
     * Called after a provider method invocation.
     *
     * @param providerId the provider ID
     * @param method the method name
     * @param result the method result
     * @return the modified result, or null to use original
     */
    default CompletableFuture<Object> afterInvoke(
            String providerId,
            String method,
            Object result
    ) {
        return CompletableFuture.completedFuture(result);
    }

    /**
     * Called when a provider error occurs.
     *
     * @param providerId the provider ID
     * @param method the method name (may be null if not method-specific)
     * @param error the error that occurred
     * @return true if the error was handled, false to propagate
     */
    default CompletableFuture<Boolean> onError(
            String providerId,
            String method,
            Throwable error
    ) {
        return CompletableFuture.completedFuture(false);
    }

    /**
     * Gets a hook for model resolution.
     *
     * @param modelId the model ID
     * @return the resolved model ID, or null to use default resolution
     */
    default CompletableFuture<String> resolveModel(String modelId) {
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Gets a hook for model input normalization.
     *
     * @param modelId the model ID
     * @param input the input to normalize
     * @return the normalized input
     */
    default CompletableFuture<Map<String, Object>> normalizeInput(
            String modelId,
            Map<String, Object> input
    ) {
        return CompletableFuture.completedFuture(input);
    }

    /**
     * No-op implementation.
     */
    ProviderRuntimeHooks NOOP = new ProviderRuntimeHooks() {};

    /**
     * Composite implementation that chains multiple hooks.
     *
     * @param hooks the hooks to chain
     * @return a composite hook
     */
    static ProviderRuntimeHooks composite(ProviderRuntimeHooks... hooks) {
        return new CompositeProviderRuntimeHooks(hooks);
    }

    /**
     * Composite hook implementation.
     */
    class CompositeProviderRuntimeHooks implements ProviderRuntimeHooks {
        private final ProviderRuntimeHooks[] hooks;

        public CompositeProviderRuntimeHooks(ProviderRuntimeHooks[] hooks) {
            this.hooks = hooks;
        }

        @Override
        public CompletableFuture<Map<String, Object>> beforeInit(
                String providerId,
                Map<String, Object> config
        ) {
            CompletableFuture<Map<String, Object>> result = CompletableFuture.completedFuture(config);
            for (ProviderRuntimeHooks hook : hooks) {
                result = result.thenCompose(c -> hook.beforeInit(providerId, c));
            }
            return result;
        }

        @Override
        public CompletableFuture<Map<String, Object>> afterInit(
                String providerId,
                Map<String, Object> result
        ) {
            CompletableFuture<Map<String, Object>> future = CompletableFuture.completedFuture(result);
            for (ProviderRuntimeHooks hook : hooks) {
                future = future.thenCompose(r -> hook.afterInit(providerId, r));
            }
            return future;
        }

        @Override
        public CompletableFuture<Map<String, Object>> beforeInvoke(
                String providerId,
                String method,
                Map<String, Object> params
        ) {
            CompletableFuture<Map<String, Object>> result = CompletableFuture.completedFuture(params);
            for (ProviderRuntimeHooks hook : hooks) {
                result = result.thenCompose(p -> hook.beforeInvoke(providerId, method, p));
            }
            return result;
        }

        @Override
        public CompletableFuture<Object> afterInvoke(
                String providerId,
                String method,
                Object result
        ) {
            CompletableFuture<Object> future = CompletableFuture.completedFuture(result);
            for (ProviderRuntimeHooks hook : hooks) {
                future = future.thenCompose(r -> hook.afterInvoke(providerId, method, r));
            }
            return future;
        }

        @Override
        public CompletableFuture<Boolean> onError(
                String providerId,
                String method,
                Throwable error
        ) {
            for (ProviderRuntimeHooks hook : hooks) {
                CompletableFuture<Boolean> handled = hook.onError(providerId, method, error);
                if (handled.join()) {
                    return CompletableFuture.completedFuture(true);
                }
            }
            return CompletableFuture.completedFuture(false);
        }
    }
}
