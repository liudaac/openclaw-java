package openclaw.plugin.manifest;

import java.util.Map;
import java.util.Optional;

/**
 * Plugin manifest record for registry storage.
 *
 * <p>Following the TypeScript manifest-registry.ts PluginManifestRecord pattern.</p>
 *
 * @author OpenClaw Team
 * @version 2026.4.12
 */
public record PluginManifestRecord(
        // Identification
        String id,
        String name,
        Optional<String> description,
        Optional<String> version,
        
        // Enablement
        boolean enabledByDefault,
        java.util.List<String> autoEnableWhenConfiguredProviders,
        java.util.List<String> legacyPluginIds,
        
        // Format and origin
        PluginFormat format,
        Optional<BundleFormat> bundleFormat,
        java.util.List<String> bundleCapabilities,
        
        // Kind and capabilities
        PluginManifest.PluginKind kind,
        java.util.List<String> channels,
        java.util.List<String> providers,
        Optional<String> providerDiscoverySource,
        
        // Model and CLI
        Optional<PluginManifest.ModelSupport> modelSupport,
        java.util.List<String> cliBackends,
        java.util.List<PluginManifest.CommandAlias> commandAliases,
        
        // Auth
        Map<String, java.util.List<String>> providerAuthEnvVars,
        Map<String, String> providerAuthAliases,
        Map<String, java.util.List<String>> channelEnvVars,
        java.util.List<PluginManifest.ProviderAuthChoice> providerAuthChoices,
        
        // Activation and setup
        Optional<PluginManifest.Activation> activation,
        Optional<PluginManifest.Setup> setup,
        
        // Skills and hooks
        java.util.List<String> skills,
        java.util.List<String> settingsFiles,
        java.util.List<String> hooks,
        
        // Origin and source
        PluginOrigin origin,
        Optional<String> workspaceDir,
        String rootDir,
        String source,
        Optional<String> setupSource,
        
        // Startup behavior
        boolean startupDeferConfiguredChannelFullLoadUntilAfterListen,
        
        // Paths and caching
        String manifestPath,
        Optional<String> schemaCacheKey,
        Optional<Map<String, Object>> configSchema,
        Optional<Map<String, Object>> configUiHints,
        
        // Contracts
        Optional<PluginManifest.Contracts> contracts,
        Optional<PluginManifest.ConfigContracts> configContracts,
        Optional<Map<String, PluginManifest.ChannelConfig>> channelConfigs,
        
        // Catalog metadata
        Optional<ChannelCatalogMeta> channelCatalogMeta
) {
    
    /**
     * Plugin format enumeration.
     */
    public enum PluginFormat {
        OPENCLAW,
        BUNDLE
    }
    
    /**
     * Bundle format enumeration.
     */
    public enum BundleFormat {
        TARBALL,
        ZIP
    }
    
    /**
     * Plugin origin enumeration.
     */
    public enum PluginOrigin {
        CONFIG,
        WORKSPACE,
        GLOBAL,
        BUNDLED
    }
    
    /**
     * Channel catalog metadata.
     */
    public record ChannelCatalogMeta(
            String id,
            Optional<String> label,
            Optional<String> blurb,
            java.util.List<String> preferOver
    ) {}
    
    /**
     * Creates a builder for fluent construction.
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Builder for PluginManifestRecord.
     */
    public static class Builder {
        private String id;
        private String name;
        private String description;
        private String version;
        private boolean enabledByDefault = false;
        private java.util.List<String> autoEnableWhenConfiguredProviders = new java.util.ArrayList<>();
        private java.util.List<String> legacyPluginIds = new java.util.ArrayList<>();
        private PluginFormat format = PluginFormat.OPENCLAW;
        private BundleFormat bundleFormat;
        private java.util.List<String> bundleCapabilities = new java.util.ArrayList<>();
        private PluginManifest.PluginKind kind = PluginManifest.PluginKind.TOOL;
        private java.util.List<String> channels = new java.util.ArrayList<>();
        private java.util.List<String> providers = new java.util.ArrayList<>();
        private String providerDiscoverySource;
        private PluginManifest.ModelSupport modelSupport;
        private java.util.List<String> cliBackends = new java.util.ArrayList<>();
        private java.util.List<PluginManifest.CommandAlias> commandAliases = new java.util.ArrayList<>();
        private Map<String, java.util.List<String>> providerAuthEnvVars = new java.util.HashMap<>();
        private Map<String, String> providerAuthAliases = new java.util.HashMap<>();
        private Map<String, java.util.List<String>> channelEnvVars = new java.util.HashMap<>();
        private java.util.List<PluginManifest.ProviderAuthChoice> providerAuthChoices = new java.util.ArrayList<>();
        private PluginManifest.Activation activation;
        private PluginManifest.Setup setup;
        private java.util.List<String> skills = new java.util.ArrayList<>();
        private java.util.List<String> settingsFiles = new java.util.ArrayList<>();
        private java.util.List<String> hooks = new java.util.ArrayList<>();
        private PluginOrigin origin = PluginOrigin.GLOBAL;
        private String workspaceDir;
        private String rootDir;
        private String source;
        private String setupSource;
        private boolean startupDeferConfiguredChannelFullLoadUntilAfterListen = false;
        private String manifestPath;
        private String schemaCacheKey;
        private Map<String, Object> configSchema;
        private Map<String, Object> configUiHints;
        private PluginManifest.Contracts contracts;
        private PluginManifest.ConfigContracts configContracts;
        private Map<String, PluginManifest.ChannelConfig> channelConfigs;
        private ChannelCatalogMeta channelCatalogMeta;
        
        public Builder id(String id) {
            this.id = id;
            return this;
        }
        
        public Builder name(String name) {
            this.name = name;
            return this;
        }
        
        public Builder description(String description) {
            this.description = description;
            return this;
        }
        
        public Builder version(String version) {
            this.version = version;
            return this;
        }
        
        public Builder enabledByDefault(boolean enabled) {
            this.enabledByDefault = enabled;
            return this;
        }
        
        public Builder rootDir(String rootDir) {
            this.rootDir = rootDir;
            return this;
        }
        
        public Builder source(String source) {
            this.source = source;
            return this;
        }
        
        public Builder manifestPath(String manifestPath) {
            this.manifestPath = manifestPath;
            return this;
        }
        
        public Builder origin(PluginOrigin origin) {
            this.origin = origin;
            return this;
        }
        
        public Builder kind(PluginManifest.PluginKind kind) {
            this.kind = kind;
            return this;
        }
        
        public Builder format(PluginFormat format) {
            this.format = format;
            return this;
        }
        
        public Builder skills(java.util.List<String> skills) {
            this.skills = skills;
            return this;
        }
        
        public Builder channels(java.util.List<String> channels) {
            this.channels = channels;
            return this;
        }
        
        public Builder providers(java.util.List<String> providers) {
            this.providers = providers;
            return this;
        }
        
        public PluginManifestRecord build() {
            return new PluginManifestRecord(
                id,
                name,
                Optional.ofNullable(description),
                Optional.ofNullable(version),
                enabledByDefault,
                autoEnableWhenConfiguredProviders,
                legacyPluginIds,
                format,
                Optional.ofNullable(bundleFormat),
                bundleCapabilities,
                kind,
                channels,
                providers,
                Optional.ofNullable(providerDiscoverySource),
                Optional.ofNullable(modelSupport),
                cliBackends,
                commandAliases,
                providerAuthEnvVars,
                providerAuthAliases,
                channelEnvVars,
                providerAuthChoices,
                Optional.ofNullable(activation),
                Optional.ofNullable(setup),
                skills,
                settingsFiles,
                hooks,
                origin,
                Optional.ofNullable(workspaceDir),
                rootDir,
                source,
                Optional.ofNullable(setupSource),
                startupDeferConfiguredChannelFullLoadUntilAfterListen,
                manifestPath,
                Optional.ofNullable(schemaCacheKey),
                Optional.ofNullable(configSchema),
                Optional.ofNullable(configUiHints),
                Optional.ofNullable(contracts),
                Optional.ofNullable(configContracts),
                Optional.ofNullable(channelConfigs),
                Optional.ofNullable(channelCatalogMeta)
            );
        }
    }
}