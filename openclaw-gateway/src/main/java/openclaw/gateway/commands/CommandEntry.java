package openclaw.gateway.commands;

import java.util.List;
import java.util.Optional;

/**
 * Command entry for commands.list RPC response.
 *
 * <p>Following the TypeScript CommandEntry type from commands.ts</p>
 *
 * @author OpenClaw Team
 * @version 2026.4.12
 */
public record CommandEntry(
        String name,
        Optional<String> nativeName,
        List<String> textAliases,
        String description,
        Optional<String> category,
        String source, // "native", "skill", "plugin"
        String scope, // "native", "text", "both"
        boolean acceptsArgs,
        Optional<List<CommandArg>> args
) {
    
    /**
     * Creates a simple command entry.
     */
    public static CommandEntry of(String name, String description, String source, String scope) {
        return new CommandEntry(
            name,
            Optional.empty(),
            List.of(),
            description,
            Optional.empty(),
            source,
            scope,
            false,
            Optional.empty()
        );
    }
    
    /**
     * Creates a command entry with aliases.
     */
    public static CommandEntry withAliases(String name, List<String> aliases, String description, String source, String scope) {
        return new CommandEntry(
            name,
            Optional.empty(),
            aliases,
            description,
            Optional.empty(),
            source,
            scope,
            false,
            Optional.empty()
        );
    }
    
    /**
     * Command argument definition.
     */
    public record CommandArg(
            String name,
            Optional<String> description,
            String type,
            boolean required,
            Optional<List<CommandArgChoice>> choices,
            boolean dynamic
    ) {}
    
    /**
     * Command argument choice.
     */
    public record CommandArgChoice(
            String value,
            String label
    ) {}
}
