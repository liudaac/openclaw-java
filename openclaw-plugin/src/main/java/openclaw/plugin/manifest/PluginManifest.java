package openclaw.plugin.manifest;

import java.util.*;

/**
 * Plugin manifest definition.
 *
 * <p>Following the TypeScript manifest.ts pattern with comprehensive
 * plugin metadata support.</p>
 *
 * @author OpenClaw Team
 * @version 2026.4.12
 */
public record PluginManifest(
        // Core identification
        String id,
        String name,
        String description,
        String version,
        
        // Configuration
        Map<String, Object> configSchema,
        Optional<Map<String, Object>> uiHints,
        
        // Enablement
        boolean enabledByDefault,
        List<String> autoEnableWhenConfiguredProviders,
        List<String> legacyPluginIds,
        
        // Kind and capabilities
        PluginKind kind,
        List<String> channels,
        List<String> providers,
        Optional<String> providerDiscoveryEntry,
        
        // Model support
        Optional<ModelSupport> modelSupport,
        
        // CLI and commands
        List<String> cliBackends,
        List<CommandAlias> commandAliases,
        
        // Auth and environment
        Map<String, List<String>> providerAuthEnvVars,
        Map<String, String> providerAuthAliases,
        Map<String, List<String>> channelEnvVars,
        List<ProviderAuthChoice> providerAuthChoices,
        
        // Activation
        Optional<Activation> activation,
        
        // Setup
        Optional<Setup> setup,
        
        // Skills and contracts
        List<String> skills,
        Optional<Contracts> contracts,
        Optional<ConfigContracts> configContracts,
        
        // Channel configs
        Optional<Map<String, ChannelConfig>> channelConfigs
) {
    
    /**
     * Creates a minimal manifest with required fields.
     */
    public static PluginManifest minimal(String id, Map<String, Object> configSchema) {
        return new PluginManifest(
            id,
            id,
            null,
            "1.0.0",
            configSchema,
            Optional.empty(),
            false,
            List.of(),
            List.of(),
            PluginKind.TOOL,
            List.of(),
            List.of(),
            Optional.empty(),
            Optional.empty(),
            List.of(),
            List.of(),
            Map.of(),
            Map.of(),
            Map.of(),
            List.of(),
            Optional.empty(),
            Optional.empty(),
            List.of(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty()
        );
    }
    
    /**
     * Plugin kind enumeration.
     */
    public enum PluginKind {
        PROVIDER,
        CHANNEL,
        TOOL,
        HOOK,
        BUNDLE
    }
    
    /**
     * Model support configuration.
     */
    public record ModelSupport(
            List<String> modelPrefixes,
            List<String> modelPatterns
    ) {
        public static ModelSupport ofPrefixes(String... prefixes) {
            return new ModelSupport(Arrays.asList(prefixes), List.of());
        }
        
        public static ModelSupport ofPatterns(String... patterns) {
            return new ModelSupport(List.of(), Arrays.asList(patterns));
        }
    }
    
    /**
     * Command alias definition.
     */
    public record CommandAlias(
            String alias,
            String command,
            Optional<String> description
    ) {}
    
    /**
     * Provider auth choice.
     */
    public record ProviderAuthChoice(
            String provider,
            String method,
            String choiceId,
            Optional<String> choiceLabel,
            Optional<String> choiceHint,
            Optional<Integer> assistantPriority,
            Optional<String> assistantVisibility,
            List<String> deprecatedChoiceIds,
            Optional<String> groupId,
            Optional<String> groupLabel,
            Optional<String> groupHint,
            Optional<String> optionKey,
            Optional<String> cliFlag,
            Optional<String> cliOption,
            Optional<String> cliDescription,
            List<String> onboardingScopes
    ) {}
    
    /**
     * Activation configuration.
     */
    public record Activation(
            List<String> onProviders,
            List<String> onCommands,
            List<String> onChannels,
            List<String> onRoutes,
            List<ActivationCapability> onCapabilities
    ) {
        public enum ActivationCapability {
            PROVIDER, CHANNEL, TOOL, HOOK
        }
    }
    
    /**
     * Setup configuration.
     */
    public record Setup(
            List<SetupProvider> providers,
            List<String> cliBackends,
            List<String> configMigrations,
            boolean requiresRuntime
    ) {}
    
    /**
     * Setup provider.
     */
    public record SetupProvider(
            String id,
            List<String> authMethods,
            List<String> envVars
    ) {}
    
    /**
     * Contracts definition.
     */
    public record Contracts(
            List<String> memoryEmbeddingProviders,
            List<String> speechProviders,
            List<String> realtimeTranscriptionProviders,
            List<String> realtimeVoiceProviders,
            List<String> mediaUnderstandingProviders,
            List<String> imageGenerationProviders,
            List<String> videoGenerationProviders,
            List<String> musicGenerationProviders,
            List<String> webFetchProviders,
            List<String> webSearchProviders,
            List<String> tools
    ) {
        public static Contracts empty() {
            return new Contracts(
                List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of()
            );
        }
    }
    
    /**
     * Config contracts.
     */
    public record ConfigContracts(
            List<String> compatibilityMigrationPaths,
            List<String> compatibilityRuntimePaths,
            List<DangerousConfigFlag> dangerousFlags,
            Optional<SecretInputContracts> secretInputs
    ) {}
    
    /**
     * Dangerous config flag.
     */
    public record DangerousConfigFlag(
            String path,
            Object equals
    ) {}
    
    /**
     * Secret input contracts.
     */
    public record SecretInputContracts(
            boolean bundledDefaultEnabled,
            List<SecretInputPath> paths
    ) {}
    
    /**
     * Secret input path.
     */
    public record SecretInputPath(
            String path,
            Optional<String> expected
    ) {}
    
    /**
     * Channel config.
     */
    public record ChannelConfig(
            Map<String, Object> schema,
            Optional<Map<String, Object>> uiHints,
            Optional<String> label,
            Optional<String> description,
            List<String> preferOver
    ) {}
}
