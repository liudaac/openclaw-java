package openclaw.cli.command;

import openclaw.cli.service.BackupService;
import picocli.CommandLine;

import java.util.concurrent.Callable;

/**
 * Backup 命令
 * 
 * 功能:
 * - openclaw backup create - 创建备份
 * - openclaw backup verify - 验证备份
 * 
 * 对应 Node.js: openclaw backup create/verify
 */
@CommandLine.Command(
    name = "backup",
    description = "Create and verify local state archives",
    subcommands = {
        BackupCommand.CreateCommand.class,
        BackupCommand.VerifyCommand.class
    }
)
public class BackupCommand implements Callable<Integer> {
    
    @Override
    public Integer call() {
        System.out.println("Usage: openclaw backup <create|verify> [options]");
        System.out.println();
        System.out.println("Commands:");
        System.out.println("  create   Create a backup archive");
        System.out.println("  verify   Verify a backup archive");
        return 0;
    }
    
    /**
     * backup create 子命令
     */
    @CommandLine.Command(
        name = "create",
        description = "Create a backup archive of OpenClaw state"
    )
    public static class CreateCommand implements Callable<Integer> {
        
        @CommandLine.Option(
            names = {"--only-config"},
            description = "Backup only configuration files"
        )
        private boolean onlyConfig = false;
        
        @CommandLine.Option(
            names = {"--no-include-workspace"},
            description = "Exclude workspace files from backup"
        )
        private boolean noIncludeWorkspace = false;
        
        @CommandLine.Option(
            names = {"--output", "-o"},
            description = "Output file path"
        )
        private String outputPath;
        
        @CommandLine.Option(
            names = {"--name", "-n"},
            description = "Backup name"
        )
        private String backupName;
        
        @CommandLine.ParentCommand
        private BackupCommand parent;
        
        @Override
        public Integer call() {
            try {
                BackupService backupService = new BackupService();
                
                System.out.println("Creating backup...");
                
                BackupService.BackupResult result = backupService.createBackup(
                    onlyConfig,
                    !noIncludeWorkspace,
                    outputPath,
                    backupName
                );
                
                if (result.isSuccess()) {
                    System.out.println("✓ Backup created successfully");
                    System.out.println();
                    System.out.println("Archive: " + result.getArchivePath());
                    System.out.println("Size: " + formatSize(result.getSize()));
                    System.out.println("Files: " + result.getFileCount());
                    System.out.println();
                    System.out.println("Manifest:");
                    System.out.println("  Config: " + (result.hasConfig() ? "✓" : "✗"));
                    System.out.println("  Workspace: " + (result.hasWorkspace() ? "✓" : "✗"));
                    System.out.println("  Memory: " + (result.hasMemory() ? "✓" : "✗"));
                    System.out.println("  Secrets: " + (result.hasSecrets() ? "✓" : "✗"));
                    return 0;
                } else {
                    System.err.println("✗ Backup failed: " + result.getErrorMessage());
                    return 1;
                }
                
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
                return 1;
            }
        }
        
        private String formatSize(long bytes) {
            if (bytes < 1024) return bytes + " B";
            if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
            if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
            return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
        }
    }
    
    /**
     * backup verify 子命令
     */
    @CommandLine.Command(
        name = "verify",
        description = "Verify a backup archive"
    )
    public static class VerifyCommand implements Callable<Integer> {
        
        @CommandLine.Parameters(
            description = "Path to backup archive",
            arity = "1"
        )
        private String archivePath;
        
        @CommandLine.Option(
            names = {"--verbose", "-v"},
            description = "Verbose output"
        )
        private boolean verbose = false;
        
        @CommandLine.ParentCommand
        private BackupCommand parent;
        
        @Override
        public Integer call() {
            try {
                BackupService backupService = new BackupService();
                
                System.out.println("Verifying backup: " + archivePath);
                System.out.println();
                
                BackupService.VerifyResult result = backupService.verifyBackup(archivePath);
                
                if (result.isValid()) {
                    System.out.println("✓ Backup is valid");
                    System.out.println();
                    System.out.println("Archive Info:");
                    System.out.println("  Created: " + result.getCreatedAt());
                    System.out.println("  Version: " + result.getVersion());
                    System.out.println("  Size: " + formatSize(result.getSize()));
                    System.out.println();
                    System.out.println("Contents:");
                    System.out.println("  Config: " + (result.hasConfig() ? "✓" : "✗"));
                    System.out.println("  Workspace: " + (result.hasWorkspace() ? "✓" : "✗"));
                    System.out.println("  Memory: " + (result.hasMemory() ? "✓" : "✗"));
                    System.out.println("  Secrets: " + (result.hasSecrets() ? "✓" : "✗"));
                    
                    if (verbose) {
                        System.out.println();
                        System.out.println("Files:");
                        for (String file : result.getFiles()) {
                            System.out.println("  - " + file);
                        }
                    }
                    
                    return 0;
                } else {
                    System.err.println("✗ Backup verification failed");
                    System.err.println();
                    System.err.println("Error: " + result.getErrorMessage());
                    return 1;
                }
                
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
                return 1;
            }
        }
        
        private String formatSize(long bytes) {
            if (bytes < 1024) return bytes + " B";
            if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
            if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
            return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
        }
    }
}
