package openclaw.gateway.call;

import openclaw.gateway.auth.DeviceAuthV3;
import openclaw.gateway.client.GatewayClient;
import openclaw.gateway.tls.GatewayTlsConfig;

import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Gateway call dependencies for dependency injection.
 *
 * <p>This class provides a container for gateway call dependencies,
 * enabling test isolation and dependency mocking. Following the pattern
 * from TypeScript call.ts with comprehensive dependency injection support.</p>
 *
 * @author OpenClaw Team
 * @version 2026.4.12
 */
public class GatewayCallDeps {

    // Core dependencies
    private Function<GatewayClientOptions, GatewayClient> createGatewayClient;
    private ConfigLoader configLoader;
    private GatewayPortResolver portResolver;
    private ConfigPathResolver configPathResolver;
    private StateDirResolver stateDirResolver;
    
    // Additional dependencies from TypeScript call.ts
    private DeviceIdentityLoader deviceIdentityLoader;
    private GatewayTlsConfigLoader tlsConfigLoader;
    private AuthRequestBuilder authRequestBuilder;

    /**
     * Creates default dependencies.
     */
    public GatewayCallDeps() {
        this.createGatewayClient = opts -> new GatewayClient(opts.url(), opts.authRequest());
        this.configLoader = () -> new OpenClawConfig() {};
        this.portResolver = config -> 8080;
        this.configPathResolver = () -> System.getProperty("user.home") + "/.openclaw/config.json";
        this.stateDirResolver = config -> System.getProperty("user.home") + "/.openclaw/state";
        this.deviceIdentityLoader = config -> Optional.empty();
        this.tlsConfigLoader = config -> Optional.empty();
        this.authRequestBuilder = (deviceId, scopes) -> DeviceAuthV3.AuthRequest.of(
            deviceId, "openclaw-java", scopes, 
            generateNonce(), "token", "signature"
        );
    }

    // Getters
    public Function<GatewayClientOptions, GatewayClient> getCreateGatewayClient() {
        return createGatewayClient;
    }

    public ConfigLoader getConfigLoader() {
        return configLoader;
    }

    public GatewayPortResolver getPortResolver() {
        return portResolver;
    }

    public ConfigPathResolver getConfigPathResolver() {
        return configPathResolver;
    }

    public StateDirResolver getStateDirResolver() {
        return stateDirResolver;
    }

    public DeviceIdentityLoader getDeviceIdentityLoader() {
        return deviceIdentityLoader;
    }

    public GatewayTlsConfigLoader getTlsConfigLoader() {
        return tlsConfigLoader;
    }

    public AuthRequestBuilder getAuthRequestBuilder() {
        return authRequestBuilder;
    }

    // Setters for testing
    public void setCreateGatewayClient(Function<GatewayClientOptions, GatewayClient> createGatewayClient) {
        this.createGatewayClient = createGatewayClient;
    }

    public void setConfigLoader(ConfigLoader configLoader) {
        this.configLoader = configLoader;
    }

    public void setPortResolver(GatewayPortResolver portResolver) {
        this.portResolver = portResolver;
    }

    public void setConfigPathResolver(ConfigPathResolver configPathResolver) {
        this.configPathResolver = configPathResolver;
    }

    public void setStateDirResolver(StateDirResolver stateDirResolver) {
        this.stateDirResolver = stateDirResolver;
    }

    public void setDeviceIdentityLoader(DeviceIdentityLoader deviceIdentityLoader) {
        this.deviceIdentityLoader = deviceIdentityLoader;
    }

    public void setTlsConfigLoader(GatewayTlsConfigLoader tlsConfigLoader) {
        this.tlsConfigLoader = tlsConfigLoader;
    }

    public void setAuthRequestBuilder(AuthRequestBuilder authRequestBuilder) {
        this.authRequestBuilder = authRequestBuilder;
    }

    /**
     * Resets all dependencies to default implementations.
     */
    public void resetToDefaults() {
        this.createGatewayClient = opts -> new GatewayClient(opts.url(), opts.authRequest());
        this.configLoader = () -> new OpenClawConfig() {};
        this.portResolver = config -> 8080;
        this.configPathResolver = () -> System.getProperty("user.home") + "/.openclaw/config.json";
        this.stateDirResolver = config -> System.getProperty("user.home") + "/.openclaw/state";
        this.deviceIdentityLoader = config -> Optional.empty();
        this.tlsConfigLoader = config -> Optional.empty();
        this.authRequestBuilder = (deviceId, scopes) -> DeviceAuthV3.AuthRequest.of(
            deviceId, "openclaw-java", scopes, 
            generateNonce(), "token", "signature"
        );
    }

    // Inner classes for dependency interfaces

    /**
     * Gateway client options.
     */
    public record GatewayClientOptions(
            String url,
            DeviceAuthV3.AuthRequest authRequest
    ) {}

    /**
     * Config loader interface.
     */
    @FunctionalInterface
    public interface ConfigLoader {
        OpenClawConfig load();
    }

    /**
     * Gateway port resolver.
     */
    @FunctionalInterface
    public interface GatewayPortResolver {
        int resolve(OpenClawConfig config);
    }

    /**
     * Config path resolver.
     */
    @FunctionalInterface
    public interface ConfigPathResolver {
        String resolve();
    }

    /**
     * State directory resolver.
     */
    @FunctionalInterface
    public interface StateDirResolver {
        String resolve(OpenClawConfig config);
    }

    /**
     * Device identity loader - loads or creates device identity.
     */
    @FunctionalInterface
    public interface DeviceIdentityLoader {
        Optional<DeviceAuthV3.DeviceCredentials> loadOrCreate(OpenClawConfig config);
    }

    /**
     * Gateway TLS config loader.
     */
    @FunctionalInterface
    public interface GatewayTlsConfigLoader {
        Optional<GatewayTlsConfig> load(OpenClawConfig config);
    }

    /**
     * Auth request builder.
     */
    @FunctionalInterface
    public interface AuthRequestBuilder {
        DeviceAuthV3.AuthRequest build(String deviceId, String[] scopes);
    }

    /**
     * OpenClaw config interface.
     */
    public interface OpenClawConfig {
        default Optional<String> getGatewayHost() {
            return Optional.of("localhost");
        }
        
        default Optional<Integer> getGatewayPort() {
            return Optional.of(8080);
        }
        
        default Optional<Boolean> isGatewayTlsEnabled() {
            return Optional.of(false);
        }
    }

    // Helper methods
    
    private static String generateNonce() {
        return java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}
