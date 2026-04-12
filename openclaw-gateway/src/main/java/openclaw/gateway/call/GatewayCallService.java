package openclaw.gateway.call;

import openclaw.gateway.auth.DeviceAuthV3;
import openclaw.gateway.client.GatewayClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Service for making gateway calls with dependency injection support.
 *
 * <p>This service provides a centralized way to make gateway calls with
 * configurable dependencies for testing and runtime flexibility. Following
 * the TypeScript call.ts pattern with comprehensive DI support.</p>
 *
 * @author OpenClaw Team
 * @version 2026.4.12
 */
public class GatewayCallService {

    private static final Logger logger = LoggerFactory.getLogger(GatewayCallService.class);

    // Thread-local storage for test isolation
    private static final ThreadLocal<GatewayCallDeps> depsHolder = ThreadLocal.withInitial(GatewayCallDeps::new);

    // Singleton instance for production use
    private static final GatewayCallService INSTANCE = new GatewayCallService();

    private GatewayCallService() {
        // Private constructor for singleton
    }

    /**
     * Gets the singleton instance.
     *
     * @return the gateway call service instance
     */
    public static GatewayCallService getInstance() {
        return INSTANCE;
    }

    /**
     * Gets the current dependencies.
     *
     * @return the current gateway call dependencies
     */
    public GatewayCallDeps getDeps() {
        return depsHolder.get();
    }

    /**
     * Sets dependencies for the current thread (for testing).
     *
     * @param deps the dependencies to use
     */
    public void setDeps(GatewayCallDeps deps) {
        depsHolder.set(deps);
    }

    /**
     * Resets dependencies to defaults for the current thread.
     */
    public void resetDeps() {
        GatewayCallDeps deps = depsHolder.get();
        if (deps != null) {
            deps.resetToDefaults();
        }
    }

    /**
     * Clears thread-local dependencies.
     */
    public void clearDeps() {
        depsHolder.remove();
    }

    /**
     * Makes a gateway call.
     *
     * @param options the call options
     * @return the call result
     */
    public CompletableFuture<GatewayCallResult> call(GatewayCallOptions options) {
        GatewayCallDeps deps = getDeps();

        try {
            // Create gateway client using injected dependency
            GatewayClient client = deps.getCreateGatewayClient().apply(
                    new GatewayCallDeps.GatewayClientOptions(options.url(), options.authRequest())
            );

            // Make the call
            return client.request(options.method(), options.params())
                    .thenApply(result -> new GatewayCallResult(true, result, null))
                    .exceptionally(e -> {
                        logger.error("Gateway call failed: {}", e.getMessage(), e);
                        return new GatewayCallResult(false, null, e.getMessage());
                    });

        } catch (Exception e) {
            logger.error("Failed to create gateway client: {}", e.getMessage(), e);
            return CompletableFuture.completedFuture(
                    new GatewayCallResult(false, null, e.getMessage())
            );
        }
    }

    /**
     * Makes a gateway call with automatic device identity resolution.
     *
     * @param options the call options with device identity
     * @return the call result
     */
    public CompletableFuture<GatewayCallResult> callWithIdentity(GatewayCallWithIdentityOptions options) {
        GatewayCallDeps deps = getDeps();

        try {
            // Load or create device identity
            Optional<DeviceAuthV3.DeviceCredentials> identity = deps.getDeviceIdentityLoader().loadOrCreate(
                deps.getConfigLoader().load()
            );

            if (identity.isEmpty()) {
                return CompletableFuture.completedFuture(
                        new GatewayCallResult(false, null, "Failed to load device identity")
                );
            }

            DeviceAuthV3.DeviceCredentials creds = identity.get();
            
            // Build auth request
            DeviceAuthV3.AuthRequest authRequest = deps.getAuthRequestBuilder().build(
                creds.deviceId(), 
                options.scopes()
            );

            // Create client and make call
            GatewayClient client = deps.getCreateGatewayClient().apply(
                    new GatewayCallDeps.GatewayClientOptions(options.url(), authRequest)
            );

            return client.request(options.method(), options.params())
                    .thenApply(result -> new GatewayCallResult(true, result, null))
                    .exceptionally(e -> {
                        logger.error("Gateway call failed: {}", e.getMessage(), e);
                        return new GatewayCallResult(false, null, e.getMessage());
                    });

        } catch (Exception e) {
            logger.error("Failed to make gateway call with identity: {}", e.getMessage(), e);
            return CompletableFuture.completedFuture(
                    new GatewayCallResult(false, null, e.getMessage())
            );
        }
    }

    /**
     * Resolves the gateway URL from configuration.
     *
     * @param config the OpenClaw configuration
     * @return the resolved gateway URL
     */
    public String resolveGatewayUrl(GatewayCallDeps.OpenClawConfig config) {
        GatewayCallDeps deps = getDeps();
        
        String host = config.getGatewayHost().orElse("localhost");
        int port = deps.getPortResolver().resolve(config);
        boolean tlsEnabled = config.isGatewayTlsEnabled().orElse(false);
        
        String protocol = tlsEnabled ? "wss" : "ws";
        return String.format("%s://%s:%d", protocol, host, port);
    }

    // Records

    /**
     * Gateway call options.
     *
     * @param url the gateway URL
     * @param authRequest the authentication request
     * @param method the method to call
     * @param params the method parameters
     */
    public record GatewayCallOptions(
            String url,
            DeviceAuthV3.AuthRequest authRequest,
            String method,
            Object params
    ) {}

    /**
     * Gateway call options with automatic identity resolution.
     *
     * @param url the gateway URL
     * @param scopes the requested scopes
     * @param method the method to call
     * @param params the method parameters
     */
    public record GatewayCallWithIdentityOptions(
            String url,
            String[] scopes,
            String method,
            Object params
    ) {}

    /**
     * Gateway call result.
     *
     * @param success whether the call succeeded
     * @param result the result data
     * @param error error message if failed
     */
    public record GatewayCallResult(
            boolean success,
            Object result,
            String error
    ) {}

    /**
     * Testing utilities for dependency injection.
     */
    public static class Testing {

        /**
         * Sets dependencies for tests.
         *
         * @param deps the dependencies to use, or null to reset
         */
        public static void setDepsForTests(GatewayCallDeps deps) {
            if (deps != null) {
                INSTANCE.setDeps(deps);
            } else {
                INSTANCE.resetDeps();
            }
        }

        /**
         * Resets all dependencies to defaults.
         */
        public static void resetDepsForTests() {
            INSTANCE.resetDeps();
        }

        /**
         * Clears thread-local state.
         */
        public static void clearThreadLocal() {
            INSTANCE.clearDeps();
        }
    }
}