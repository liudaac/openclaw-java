package openclaw.plugin.manifest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Plugin manifest registry for managing plugin metadata.
 *
 * <p>Following the TypeScript manifest-registry.ts pattern with
 * caching and contract resolution support.</p>
 *
 * @author OpenClaw Team
 * @version 2026.4.12
 */
public class PluginManifestRegistry {
    
    private static final Logger logger = LoggerFactory.getLogger(PluginManifestRegistry.class);
    
    // Default cache TTL in milliseconds
    private static final long DEFAULT_CACHE_MS = 1000;
    
    // Registry cache
    private static final Map<String, CacheEntry> registryCache = new ConcurrentHashMap<>();
    
    private final List<PluginManifestRecord> plugins;
    private final List<PluginDiagnostic> diagnostics;
    
    public PluginManifestRegistry(List<PluginManifestRecord> plugins, List<PluginDiagnostic> diagnostics) {
        this.plugins = List.copyOf(plugins);
        this.diagnostics = List.copyOf(diagnostics);
    }
    
    /**
     * Gets all registered plugins.
     */
    public List<PluginManifestRecord> getPlugins() {
        return plugins;
    }
    
    /**
     * Gets all diagnostics.
     */
    public List<PluginDiagnostic> getDiagnostics() {
        return diagnostics;
    }
    
    /**
     * Finds a plugin by ID.
     */
    public Optional<PluginManifestRecord> findById(String id) {
        return plugins.stream()
            .filter(p -> p.id().equals(id))
            .findFirst();
    }
    
    /**
     * Finds plugins by kind.
     */
    public List<PluginManifestRecord> findByKind(PluginManifest.PluginKind kind) {
        return plugins.stream()
            .filter(p -> p.kind() == kind)
            .collect(Collectors.toList());
    }
    
    /**
     * Finds plugins by origin.
     */
    public List<PluginManifestRecord> findByOrigin(PluginManifestRecord.PluginOrigin origin) {
        return plugins.stream()
            .filter(p -> p.origin() == origin)
            .collect(Collectors.toList());
    }
    
    /**
     * Resolves plugin IDs by contract type.
     */
    public List<String> resolveContractPluginIds(ContractType contract) {
        return plugins.stream()
            .filter(p -> p.contracts().isPresent())
            .filter(p -> hasContract(p.contracts().get(), contract))
            .map(PluginManifestRecord::id)
            .sorted()
            .collect(Collectors.toList());
    }
    
    /**
     * Resolves plugin IDs by contract and origin.
     */
    public List<String> resolveContractPluginIds(ContractType contract, PluginManifestRecord.PluginOrigin origin) {
        return plugins.stream()
            .filter(p -> p.origin() == origin)
            .filter(p -> p.contracts().isPresent())
            .filter(p -> hasContract(p.contracts().get(), contract))
            .map(PluginManifestRecord::id)
            .sorted()
            .collect(Collectors.toList());
    }
    
    /**
     * Resolves the owner plugin ID for a contract value.
     */
    public Optional<String> resolveContractOwner(ContractType contract, String value) {
        String normalizedValue = value.toLowerCase();
        return plugins.stream()
            .filter(p -> p.contracts().isPresent())
            .filter(p -> hasContractValue(p.contracts().get(), contract, normalizedValue))
            .map(PluginManifestRecord::id)
            .findFirst();
    }
    
    /**
     * Clears the registry cache.
     */
    public static void clearCache() {
        registryCache.clear();
        logger.debug("Plugin manifest registry cache cleared");
    }
    
    /**
     * Loads the plugin manifest registry.
     */
    public static PluginManifestRegistry load(RegistryLoadOptions options) {
        String cacheKey = buildCacheKey(options);
        
        if (options.useCache()) {
            CacheEntry cached = registryCache.get(cacheKey);
            if (cached != null && !cached.isExpired()) {
                logger.debug("Returning cached plugin manifest registry");
                return cached.registry();
            }
        }
        
        // Perform discovery and loading
        List<PluginManifestRecord> plugins = new ArrayList<>();
        List<PluginDiagnostic> diagnostics = new ArrayList<>();
        
        // Load from workspace if specified
        if (options.workspaceDir().isPresent()) {
            loadFromWorkspace(options.workspaceDir().get(), plugins, diagnostics);
        }
        
        // Load from configured paths
        for (String path : options.loadPaths()) {
            loadFromPath(path, plugins, diagnostics);
        }
        
        // Load bundled plugins
        loadBundledPlugins(plugins, diagnostics);
        
        // Resolve duplicates
        List<PluginManifestRecord> deduplicated = resolveDuplicates(plugins, diagnostics);
        
        PluginManifestRegistry registry = new PluginManifestRegistry(deduplicated, diagnostics);
        
        // Cache if enabled
        if (options.useCache()) {
            long ttl = options.cacheTtlMs().orElse(DEFAULT_CACHE_MS);
            registryCache.put(cacheKey, new CacheEntry(registry, System.currentTimeMillis() + ttl));
        }
        
        return registry;
    }
    
    // Private helper methods
    
    private static boolean hasContract(PluginManifest.Contracts contracts, ContractType type) {
        return switch (type) {
            case MEMORY_EMBEDDING -> contracts.memoryEmbeddingProviders() != null && !contracts.memoryEmbeddingProviders().isEmpty();
            case SPEECH -> contracts.speechProviders() != null && !contracts.speechProviders().isEmpty();
            case REALTIME_TRANSCRIPTION -> contracts.realtimeTranscriptionProviders() != null && !contracts.realtimeTranscriptionProviders().isEmpty();
            case REALTIME_VOICE -> contracts.realtimeVoiceProviders() != null && !contracts.realtimeVoiceProviders().isEmpty();
            case MEDIA_UNDERSTANDING -> contracts.mediaUnderstandingProviders() != null && !contracts.mediaUnderstandingProviders().isEmpty();
            case IMAGE_GENERATION -> contracts.imageGenerationProviders() != null && !contracts.imageGenerationProviders().isEmpty();
            case VIDEO_GENERATION -> contracts.videoGenerationProviders() != null && !contracts.videoGenerationProviders().isEmpty();
            case MUSIC_GENERATION -> contracts.musicGenerationProviders() != null && !contracts.musicGenerationProviders().isEmpty();
            case WEB_FETCH -> contracts.webFetchProviders() != null && !contracts.webFetchProviders().isEmpty();
            case WEB_SEARCH -> contracts.webSearchProviders() != null && !contracts.webSearchProviders().isEmpty();
            case TOOLS -> contracts.tools() != null && !contracts.tools().isEmpty();
        };
    }
    
    private static boolean hasContractValue(PluginManifest.Contracts contracts, ContractType type, String value) {
        List<String> values = switch (type) {
            case MEMORY_EMBEDDING -> contracts.memoryEmbeddingProviders();
            case SPEECH -> contracts.speechProviders();
            case REALTIME_TRANSCRIPTION -> contracts.realtimeTranscriptionProviders();
            case REALTIME_VOICE -> contracts.realtimeVoiceProviders();
            case MEDIA_UNDERSTANDING -> contracts.mediaUnderstandingProviders();
            case IMAGE_GENERATION -> contracts.imageGenerationProviders();
            case VIDEO_GENERATION -> contracts.videoGenerationProviders();
            case MUSIC_GENERATION -> contracts.musicGenerationProviders();
            case WEB_FETCH -> contracts.webFetchProviders();
            case WEB_SEARCH -> contracts.webSearchProviders();
            case TOOLS -> contracts.tools();
        };
        
        if (values == null) return false;
        return values.stream()
            .map(String::toLowerCase)
            .anyMatch(v -> v.equals(value));
    }
    
    private static String buildCacheKey(RegistryLoadOptions options) {
        return String.format("%s::%s::%s",
            options.workspaceDir().orElse(""),
            String.join(",", options.loadPaths()),
            options.env().hashCode()
        );
    }
    
    private static void loadFromWorkspace(String workspaceDir, List<PluginManifestRecord> plugins, List<PluginDiagnostic> diagnostics) {
        // In real implementation, scan workspace directory
        logger.debug("Loading plugins from workspace: {}", workspaceDir);
    }
    
    private static void loadFromPath(String path, List<PluginManifestRecord> plugins, List<PluginDiagnostic> diagnostics) {
        // In real implementation, load from specific path
        logger.debug("Loading plugins from path: {}", path);
    }
    
    private static void loadBundledPlugins(List<PluginManifestRecord> plugins, List<PluginDiagnostic> diagnostics) {
        // In real implementation, load bundled plugins
        logger.debug("Loading bundled plugins");
    }
    
    private static List<PluginManifestRecord> resolveDuplicates(
            List<PluginManifestRecord> plugins, 
            List<PluginDiagnostic> diagnostics) {
        
        Map<String, PluginManifestRecord> seen = new HashMap<>();
        List<PluginManifestRecord> result = new ArrayList<>();
        
        for (PluginManifestRecord plugin : plugins) {
            PluginManifestRecord existing = seen.get(plugin.id());
            if (existing != null) {
                // Check precedence
                int existingRank = getOriginRank(existing.origin());
                int newRank = getOriginRank(plugin.origin());
                
                if (newRank < existingRank) {
                    diagnostics.add(new PluginDiagnostic(
                        PluginDiagnostic.Level.WARN,
                        "Duplicate plugin id detected; " + existing.origin() + 
                        " plugin will be overridden by " + plugin.origin() + " plugin (" + plugin.source() + ")",
                        plugin.id(),
                        plugin.source()
                    ));
                    seen.put(plugin.id(), plugin);
                } else {
                    diagnostics.add(new PluginDiagnostic(
                        PluginDiagnostic.Level.WARN,
                        "Duplicate plugin id detected; " + plugin.origin() + 
                        " plugin will be overridden by " + existing.origin() + " plugin (" + existing.source() + ")",
                        plugin.id(),
                        plugin.source()
                    ));
                }
            } else {
                seen.put(plugin.id(), plugin);
                result.add(plugin);
            }
        }
        
        return result;
    }
    
    private static int getOriginRank(PluginManifestRecord.PluginOrigin origin) {
        return switch (origin) {
            case CONFIG -> 0;
            case GLOBAL -> 1;
            case BUNDLED -> 2;
            case WORKSPACE -> 3;
        };
    }
    
    // Records
    
    private record CacheEntry(PluginManifestRegistry registry, long expiresAt) {
        boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }
    }
    
    /**
     * Contract type enumeration.
     */
    public enum ContractType {
        MEMORY_EMBEDDING,
        SPEECH,
        REALTIME_TRANSCRIPTION,
        REALTIME_VOICE,
        MEDIA_UNDERSTANDING,
        IMAGE_GENERATION,
        VIDEO_GENERATION,
        MUSIC_GENERATION,
        WEB_FETCH,
        WEB_SEARCH,
        TOOLS
    }
    
    /**
     * Registry load options.
     */
    public record RegistryLoadOptions(
            Optional<String> workspaceDir,
            List<String> loadPaths,
            Map<String, String> env,
            boolean useCache,
            Optional<Long> cacheTtlMs
    ) {
        public static RegistryLoadOptions defaults() {
            return new RegistryLoadOptions(
                Optional.empty(),
                List.of(),
                System.getenv(),
                true,
                Optional.of(DEFAULT_CACHE_MS)
            );
        }
        
        public static RegistryLoadOptions noCache() {
            return new RegistryLoadOptions(
                Optional.empty(),
                List.of(),
                System.getenv(),
                false,
                Optional.empty()
            );
        }
    }
}