package openclaw.gateway.commands;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Handler for commands.list RPC method.
 *
 * <p>Following the TypeScript commands.ts pattern with comprehensive
 * command listing support for agents.</p>
 *
 * @author OpenClaw Team
 * @version 2026.4.12
 */
public class CommandsListHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(CommandsListHandler.class);
    
    private final CommandRegistry commandRegistry;
    private final SkillCommandResolver skillCommandResolver;
    private final PluginCommandResolver pluginCommandResolver;
    
    public CommandsListHandler(
            CommandRegistry commandRegistry,
            SkillCommandResolver skillCommandResolver,
            PluginCommandResolver pluginCommandResolver) {
        this.commandRegistry = commandRegistry;
        this.skillCommandResolver = skillCommandResolver;
        this.pluginCommandResolver = pluginCommandResolver;
    }
    
    /**
     * Handles commands.list request.
     *
     * @param request the list request
     * @return the list result
     */
    public CommandsListResult handle(CommandsListRequest request) {
        logger.debug("Handling commands.list for agent: {}, provider: {}, scope: {}",
            request.agentId(), request.provider(), request.scope());
        
        // Resolve agent ID
        String agentId = request.agentId();
        if (agentId == null || agentId.isBlank()) {
            agentId = commandRegistry.resolveDefaultAgentId();
        }
        
        // Validate agent ID
        if (!commandRegistry.isValidAgentId(agentId)) {
            throw new IllegalArgumentException("Unknown agent id: " + request.agentId());
        }
        
        // Build result
        return buildCommandsListResult(agentId, request);
    }
    
    /**
     * Builds the commands list result.
     */
    private CommandsListResult buildCommandsListResult(String agentId, CommandsListRequest request) {
        String scopeFilter = request.scope() != null ? request.scope() : "both";
        String nameSurface = scopeFilter.equals("text") ? "text" : "native";
        String provider = normalizeProvider(request.provider());
        boolean includeArgs = request.includeArgs() != null ? request.includeArgs() : true;
        
        List<CommandEntry> commands = new ArrayList<>();
        
        // Get skill commands
        Set<String> skillKeys = new HashSet<>();
        List<SkillCommand> skillCommands = skillCommandResolver.listForAgent(agentId);
        for (SkillCommand cmd : skillCommands) {
            skillKeys.add("skill:" + cmd.skillName());
        }
        
        // Get chat commands
        List<ChatCommand> chatCommands = commandRegistry.listChatCommands(agentId);
        for (ChatCommand cmd : chatCommands) {
            // Filter by scope
            if (!scopeFilter.equals("both") && 
                !cmd.scope().equals("both") && 
                !cmd.scope().equals(scopeFilter)) {
                continue;
            }
            
            String source = skillKeys.contains(cmd.key()) ? "skill" : "native";
            CommandEntry entry = mapCommand(cmd, source, includeArgs, nameSurface, provider);
            commands.add(entry);
        }
        
        // Add plugin commands
        List<CommandEntry> pluginCommands = buildPluginCommandEntries(provider, nameSurface);
        commands.addAll(pluginCommands);
        
        return new CommandsListResult(commands);
    }
    
    /**
     * Maps a chat command to command entry.
     */
    private CommandEntry mapCommand(
            ChatCommand cmd,
            String source,
            boolean includeArgs,
            String nameSurface,
            String provider) {
        
        boolean shouldIncludeArgs = includeArgs && cmd.acceptsArgs() && cmd.args() != null && !cmd.args().isEmpty();
        String nativeName = cmd.scope().equals("text") ? null : resolveNativeName(cmd, provider);
        
        String name = nameSurface.equals("text") 
            ? resolvePrimaryTextName(cmd) 
            : (nativeName != null ? nativeName : cmd.key());
        
        return new CommandEntry(
            name,
            Optional.ofNullable(nativeName),
            cmd.scope().equals("native") ? List.of() : resolveTextAliases(cmd),
            cmd.description(),
            Optional.ofNullable(cmd.category()),
            source,
            cmd.scope(),
            cmd.acceptsArgs(),
            shouldIncludeArgs ? Optional.of(mapArgs(cmd.args())) : Optional.empty()
        );
    }
    
    /**
     * Resolves native name for a command.
     */
    private String resolveNativeName(ChatCommand cmd, String provider) {
        String baseName = cmd.nativeName() != null ? cmd.nativeName() : cmd.key();
        if (provider == null || cmd.nativeName() == null) {
            return baseName;
        }
        // In real implementation, delegate to channel plugin
        return baseName;
    }
    
    /**
     * Resolves primary text name.
     */
    private String resolvePrimaryTextName(ChatCommand cmd) {
        List<String> aliases = resolveTextAliases(cmd);
        String firstAlias = aliases.isEmpty() ? "/" + cmd.key() : aliases.get(0);
        return stripLeadingSlash(firstAlias);
    }
    
    /**
     * Resolves text aliases.
     */
    private List<String> resolveTextAliases(ChatCommand cmd) {
        Set<String> seen = new HashSet<>();
        List<String> aliases = new ArrayList<>();
        
        for (String alias : cmd.textAliases()) {
            String trimmed = alias.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            String exactAlias = trimmed.startsWith("/") ? trimmed : "/" + trimmed;
            if (seen.contains(exactAlias)) {
                continue;
            }
            seen.add(exactAlias);
            aliases.add(exactAlias);
        }
        
        if (aliases.isEmpty()) {
            return List.of("/" + cmd.key());
        }
        return aliases;
    }
    
    /**
     * Strips leading slash.
     */
    private String stripLeadingSlash(String value) {
        return value.startsWith("/") ? value.substring(1) : value;
    }
    
    /**
     * Maps command arguments.
     */
    private List<CommandEntry.CommandArg> mapArgs(List<CommandArg> args) {
        return args.stream()
            .map(this::mapArg)
            .collect(Collectors.toList());
    }
    
    /**
     * Maps a single command argument.
     */
    private CommandEntry.CommandArg mapArg(CommandArg arg) {
        boolean isDynamic = arg.choices() instanceof java.util.function.Supplier;
        List<CommandEntry.CommandArgChoice> staticChoices = null;
        
        if (arg.choices() instanceof List) {
            @SuppressWarnings("unchecked")
            List<CommandArgChoice> choices = (List<CommandArgChoice>) arg.choices();
            staticChoices = choices.stream()
                .map(c -> new CommandEntry.CommandArgChoice(c.value(), c.label()))
                .collect(Collectors.toList());
        }
        
        return new CommandEntry.CommandArg(
            arg.name(),
            Optional.ofNullable(arg.description()),
            arg.type(),
            arg.required(),
            Optional.ofNullable(staticChoices),
            isDynamic
        );
    }
    
    /**
     * Builds plugin command entries.
     */
    private List<CommandEntry> buildPluginCommandEntries(String provider, String nameSurface) {
        List<CommandEntry> entries = new ArrayList<>();
        List<PluginCommandSpec> pluginSpecs = pluginCommandResolver.listCommands();
        List<PluginCommandSpec> pluginNativeSpecs = pluginCommandResolver.getSpecs(provider);
        
        for (int i = 0; i < pluginSpecs.size(); i++) {
            PluginCommandSpec textSpec = pluginSpecs.get(i);
            PluginCommandSpec nativeSpec = i < pluginNativeSpecs.size() ? pluginNativeSpecs.get(i) : null;
            String nativeName = nativeSpec != null ? nativeSpec.name() : null;
            
            String name = nameSurface.equals("text") ? textSpec.name() : (nativeName != null ? nativeName : textSpec.name());
            
            CommandEntry entry = new CommandEntry(
                name,
                Optional.ofNullable(nativeName),
                List.of("/" + textSpec.name()),
                textSpec.description(),
                Optional.empty(),
                "plugin",
                "both",
                textSpec.acceptsArgs(),
                Optional.empty()
            );
            entries.add(entry);
        }
        
        if (nameSurface.equals("native")) {
            return entries.stream()
                .filter(e -> e.nativeName().isPresent())
                .collect(Collectors.toList());
        }
        return entries;
    }
    
    /**
     * Normalizes provider string.
     */
    private String normalizeProvider(String provider) {
        if (provider == null) {
            return null;
        }
        return provider.toLowerCase().trim();
    }
    
    // Records for request/response
    
    /**
     * Commands list request.
     */
    public record CommandsListRequest(
            String agentId,
            String provider,
            String scope, // "native", "text", "both"
            Boolean includeArgs
    ) {
        public static CommandsListRequest defaults() {
            return new CommandsListRequest(null, null, "both", true);
        }
    }
    
    /**
     * Commands list result.
     */
    public record CommandsListResult(
            List<CommandEntry> commands
    ) {}
    
    // Interfaces for dependencies
    
    /**
     * Command registry interface.
     */
    public interface CommandRegistry {
        List<ChatCommand> listChatCommands(String agentId);
        String resolveDefaultAgentId();
        boolean isValidAgentId(String agentId);
    }
    
    /**
     * Skill command resolver interface.
     */
    public interface SkillCommandResolver {
        List<SkillCommand> listForAgent(String agentId);
    }
    
    /**
     * Plugin command resolver interface.
     */
    public interface PluginCommandResolver {
        List<PluginCommandSpec> listCommands();
        List<PluginCommandSpec> getSpecs(String provider);
    }
    
    // Command type records
    
    /**
     * Chat command definition.
     */
    public record ChatCommand(
            String key,
            String description,
            String scope, // "native", "text", "both"
            boolean acceptsArgs,
            List<String> textAliases,
            String nativeName,
            String category,
            List<CommandArg> args
    ) {}
    
    /**
     * Command argument definition.
     */
    public record CommandArg(
            String name,
            String description,
            String type,
            boolean required,
            Object choices // List<CommandArgChoice> or Supplier<List<CommandArgChoice>>
    ) {}
    
    /**
     * Command argument choice.
     */
    public record CommandArgChoice(
            String value,
            String label
    ) {}
    
    /**
     * Skill command definition.
     */
    public record SkillCommand(
            String skillName,
            String command,
            String description
    ) {}
    
    /**
     * Plugin command specification.
     */
    public record PluginCommandSpec(
            String name,
            String description,
            boolean acceptsArgs
    ) {}
}