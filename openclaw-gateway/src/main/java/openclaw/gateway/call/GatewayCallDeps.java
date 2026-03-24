package openclaw.gateway.call;

import openclaw.gateway.client.GatewayClient;

import java.util.function.Function;

/**
 * Gateway call dependencies for dependency injection.
 *
 * <p>This class provides a container for gateway call dependencies,
 * enabling test isolation and dependency mocking.</p>
 *
 * @author OpenClaw Team
 * @version 2026.3.24
 */
public class GatewayCallDeps {

    private Function<GatewayClientOptions, GatewayClient> createGatewayClient;
    private ConfigLoader configLoader;
    private GatewayPortResolver portResolver;
    private ConfigPathResolver configPathResolver;
    private StateDirResolver stateDirResolver;

    /**
     * Creates default dependencies.
     */
    public GatewayCallDeps() {
        this.createGatewayClient = opts -> new GatewayClient(opts.url(), opts.authRequest());
        this.configLoader = () -> new OpenClawConfig() {};
        this.portResolver = config -> 8080;
        this.configPathResolver = () -> System.getProperty("user.home") + "/.openclaw/config.json";
        this.stateDirResolver = config -> System.getProperty("user.home") + "/.openclaw/state";
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

    /**
     * Resets all dependencies to default implementations.
     */
    public void resetToDefaults() {
        this.createGatewayClient = opts -> new GatewayClient(opts.url(), opts.authRequest());
        this.configLoader = () -> new OpenClawConfig() {};
        this.portResolver = config -> 8080;
        this.configPathResolver = () -> System.getProperty("user.home") + "/.openclaw/config.json";
        this.stateDirResolver = config -> System.getProperty("user.home") + "/.openclaw/state";
    }

    // Inner classes for dependency interfaces

    /**
     * Gateway client options.
     */
    public record GatewayClientOptions(
            String url,
            openclaw.gateway.auth.DeviceAuthV3.AuthRequest authRequest
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
     * OpenClaw config placeholder.
     */
    public interface OpenClawConfig {
        // Config methods
    }
}
