package openclaw.channel.telegram;

import openclaw.sdk.channel.ChannelCommandAdapter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Telegram command adapter for slash commands.
 *
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public class TelegramCommandAdapter implements ChannelCommandAdapter {

    @Override
    public List<CommandDefinition> getCommands() {
        return List.of(
                new CommandDefinition(
                        "status",
                        "Check bot status",
                        "",
                        false,
                        Map.of("ru", "статус", "es", "estado")
                ),
                new CommandDefinition(
                        "help",
                        "Show help",
                        "",
                        false,
                        Map.of("ru", "помощь", "es", "ayuda")
                ),
                new CommandDefinition(
                        "settings",
                        "Open settings",
                        "",
                        false,
                        Map.of("ru", "настройки", "es", "configuración")
                )
        );
    }

    @Override
    public CompletableFuture<CommandResult> handleCommand(String command, CommandContext context) {
        return CompletableFuture.completedFuture(
                switch (command) {
                    case "status" -> CommandResult.text("✅ Bot is running");
                    case "help" -> CommandResult.text("Available commands: /status, /help, /settings");
                    case "settings" -> CommandResult.ephemeral("Settings panel (not implemented)");
                    default -> CommandResult.text("Unknown command: " + command);
                }
        );
    }
}
