package openclaw.channel.telegram.migration;

import openclaw.channel.telegram.config.TelegramAccountConfig;

import java.util.HashMap;
import java.util.Map;

/**
 * State migration for Telegram pairing configuration.
 * Migrates allowFrom to default account only.
 */
public class TelegramStateMigration {

    private static final String MIGRATION_VERSION = "2026-03-31";

    /**
     * Migrate account configuration.
     * Moves allowFrom setting to default account only.
     */
    public MigrationResult migrateAccountConfig(TelegramAccountConfig config) {
        if (config == null) {
            return new MigrationResult(false, "Config is null", null);
        }

        Map<String, Object> migrated = new HashMap<>();
        
        // Check if migration is needed
        if (config.getAllowFrom() == null || config.getAllowFrom().isEmpty()) {
            return new MigrationResult(false, "No allowFrom to migrate", config);
        }

        // Migrate allowFrom to default account
        String defaultAccount = config.getDefaultAccount();
        if (defaultAccount == null || defaultAccount.isBlank()) {
            defaultAccount = "default";
        }

        // Create migrated config
        TelegramAccountConfig migratedConfig = TelegramAccountConfig.builder()
            .accountId(config.getAccountId())
            .token(config.getToken())
            .defaultAccount(defaultAccount)
            .allowFrom(null) // Clear allowFrom, now uses default account
            .build();

        migrated.put("accountId", config.getAccountId());
        migrated.put("defaultAccount", defaultAccount);
        migrated.put("clearedFields", "allowFrom");

        return new MigrationResult(true, "Migrated successfully", migratedConfig);
    }

    /**
     * Check if config needs migration.
     */
    public boolean needsMigration(TelegramAccountConfig config) {
        if (config == null) {
            return false;
        }
        return config.getAllowFrom() != null && !config.getAllowFrom().isEmpty();
    }

    /**
     * Get migration version.
     */
    public String getMigrationVersion() {
        return MIGRATION_VERSION;
    }

    /**
     * Migration result record.
     */
    public record MigrationResult(
        boolean success,
        String message,
        TelegramAccountConfig migratedConfig
    ) {}
}
