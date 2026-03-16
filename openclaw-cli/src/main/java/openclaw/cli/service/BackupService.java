package openclaw.cli.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * 备份服务
 * 
 * 功能:
 * - 创建备份归档
 * - 验证备份完整性
 * - 支持配置/工作区/记忆/Secrets 备份
 */
public class BackupService {
    
    private static final String BACKUP_DIR = System.getProperty("user.home") + "/.openclaw/backups";
    private static final String CONFIG_FILE = System.getProperty("user.home") + "/.openclaw/openclaw.json";
    private static final String WORKSPACE_DIR = System.getProperty("user.home") + "/.openclaw/workspace";
    private static final String MEMORY_DIR = System.getProperty("user.home") + "/.openclaw/memory";
    private static final String SECRETS_FILE = System.getProperty("user.home") + "/.openclaw/secrets.json";
    
    private final ObjectMapper objectMapper;
    
    public BackupService() {
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * 创建备份
     */
    public BackupResult createBackup(boolean onlyConfig, boolean includeWorkspace,
                                     String outputPath, String backupName) throws IOException {
        
        // 生成备份文件名
        String timestamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
            .format(java.time.LocalDateTime.now());
        String name = backupName != null ? backupName : "openclaw-backup-" + timestamp;
        String archiveName = name + ".zip";
        
        Path archivePath;
        if (outputPath != null) {
            archivePath = Paths.get(outputPath);
        } else {
            Files.createDirectories(Paths.get(BACKUP_DIR));
            archivePath = Paths.get(BACKUP_DIR, archiveName);
        }
        
        // 创建 manifest
        ObjectNode manifest = objectMapper.createObjectNode();
        manifest.put("version", "2026.3.9");
        manifest.put("createdAt", Instant.now().toString());
        manifest.put("onlyConfig", onlyConfig);
        manifest.put("includeWorkspace", includeWorkspace);
        
        List<String> files = new ArrayList<>();
        long totalSize = 0;
        int fileCount = 0;
        
        try (ZipOutputStream zos = new ZipOutputStream(
                new BufferedOutputStream(Files.newOutputStream(archivePath)))) {
            
            // 添加 manifest
            ZipEntry manifestEntry = new ZipEntry("manifest.json");
            zos.putNextEntry(manifestEntry);
            zos.write(manifest.toString().getBytes());
            zos.closeEntry();
            
            // 备份配置
            Path configPath = Paths.get(CONFIG_FILE);
            if (Files.exists(configPath)) {
                ZipEntry configEntry = new ZipEntry("config/openclaw.json");
                zos.putNextEntry(configEntry);
                byte[] configData = Files.readAllBytes(configPath);
                zos.write(configData);
                zos.closeEntry();
                files.add("config/openclaw.json");
                totalSize += configData.length;
                fileCount++;
            }
            
            // 备份工作区
            if (!onlyConfig && includeWorkspace) {
                Path workspacePath = Paths.get(WORKSPACE_DIR);
                if (Files.exists(workspacePath)) {
                    fileCount += addDirectoryToZip(zos, workspacePath, "workspace", files);
                }
            }
            
            // 备份记忆
            if (!onlyConfig) {
                Path memoryPath = Paths.get(MEMORY_DIR);
                if (Files.exists(memoryPath)) {
                    fileCount += addDirectoryToZip(zos, memoryPath, "memory", files);
                }
            }
            
            // 备份 Secrets
            if (!onlyConfig) {
                Path secretsPath = Paths.get(SECRETS_FILE);
                if (Files.exists(secretsPath)) {
                    ZipEntry secretsEntry = new ZipEntry("secrets/secrets.json");
                    zos.putNextEntry(secretsEntry);
                    byte[] secretsData = Files.readAllBytes(secretsPath);
                    zos.write(secretsData);
                    zos.closeEntry();
                    files.add("secrets/secrets.json");
                    totalSize += secretsData.length;
                    fileCount++;
                }
            }
        }
        
        long archiveSize = Files.size(archivePath);
        Path configPath = Paths.get(CONFIG_FILE);
        
        return new BackupResult(
            true,
            archivePath.toString(),
            archiveSize,
            fileCount,
            Files.exists(configPath),
            !onlyConfig && includeWorkspace && Files.exists(Paths.get(WORKSPACE_DIR)),
            !onlyConfig && Files.exists(Paths.get(MEMORY_DIR)),
            !onlyConfig && Files.exists(Paths.get(SECRETS_FILE)),
            null
        );
    }
    
    /**
     * 验证备份
     */
    public VerifyResult verifyBackup(String archivePath) throws IOException {
        Path path = Paths.get(archivePath);
        
        if (!Files.exists(path)) {
            return new VerifyResult(false, "Archive not found: " + archivePath, null, null, 0, false, false, false, false, null);
        }
        
        long archiveSize = Files.size(path);
        List<String> files = new ArrayList<>();
        
        try (ZipInputStream zis = new ZipInputStream(
                new BufferedInputStream(Files.newInputStream(path)))) {
            
            ZipEntry entry;
            boolean hasManifest = false;
            boolean hasConfig = false;
            boolean hasWorkspace = false;
            boolean hasMemory = false;
            boolean hasSecrets = false;
            String version = null;
            String createdAt = null;
            
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();
                files.add(name);
                
                if (name.equals("manifest.json")) {
                    hasManifest = true;
                    byte[] data = zis.readAllBytes();
                    JsonNode manifest = objectMapper.readTree(data);
                    version = manifest.path("version").asText();
                    createdAt = manifest.path("createdAt").asText();
                }
                
                if (name.startsWith("config/")) hasConfig = true;
                if (name.startsWith("workspace/")) hasWorkspace = true;
                if (name.startsWith("memory/")) hasMemory = true;
                if (name.startsWith("secrets/")) hasSecrets = true;
            }
            
            if (!hasManifest) {
                return new VerifyResult(false, "Missing manifest.json", null, null, archiveSize, false, false, false, false, files);
            }
            
            return new VerifyResult(
                true,
                null,
                version,
                createdAt,
                archiveSize,
                hasConfig,
                hasWorkspace,
                hasMemory,
                hasSecrets,
                files
            );
            
        } catch (Exception e) {
            return new VerifyResult(false, "Invalid archive: " + e.getMessage(), null, null, archiveSize, false, false, false, false, files);
        }
    }
    
    /**
     * 添加目录到 ZIP
     */
    private int addDirectoryToZip(ZipOutputStream zos, Path sourceDir, String zipPath,
                                  List<String> files) throws IOException {
        final int[] count = {0};
        
        Files.walkFileTree(sourceDir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                String zipEntryName = zipPath + "/" + sourceDir.relativize(file).toString();
                ZipEntry entry = new ZipEntry(zipEntryName);
                zos.putNextEntry(entry);
                Files.copy(file, zos);
                zos.closeEntry();
                files.add(zipEntryName);
                count[0]++;
                return FileVisitResult.CONTINUE;
            }
        });
        
        return count[0];
    }
    
    // Result classes
    
    public static class BackupResult {
        private final boolean success;
        private final String archivePath;
        private final long size;
        private final int fileCount;
        private final boolean hasConfig;
        private final boolean hasWorkspace;
        private final boolean hasMemory;
        private final boolean hasSecrets;
        private final String errorMessage;
        
        public BackupResult(boolean success, String archivePath, long size, int fileCount,
                           boolean hasConfig, boolean hasWorkspace, boolean hasMemory,
                           boolean hasSecrets, String errorMessage) {
            this.success = success;
            this.archivePath = archivePath;
            this.size = size;
            this.fileCount = fileCount;
            this.hasConfig = hasConfig;
            this.hasWorkspace = hasWorkspace;
            this.hasMemory = hasMemory;
            this.hasSecrets = hasSecrets;
            this.errorMessage = errorMessage;
        }
        
        public boolean isSuccess() { return success; }
        public String getArchivePath() { return archivePath; }
        public long getSize() { return size; }
        public int getFileCount() { return fileCount; }
        public boolean hasConfig() { return hasConfig; }
        public boolean hasWorkspace() { return hasWorkspace; }
        public boolean hasMemory() { return hasMemory; }
        public boolean hasSecrets() { return hasSecrets; }
        public String getErrorMessage() { return errorMessage; }
    }
    
    public static class VerifyResult {
        private final boolean valid;
        private final String errorMessage;
        private final String version;
        private final String createdAt;
        private final long size;
        private final boolean hasConfig;
        private final boolean hasWorkspace;
        private final boolean hasMemory;
        private final boolean hasSecrets;
        private final List<String> files;
        
        public VerifyResult(boolean valid, String errorMessage, String version, String createdAt,
                           long size, boolean hasConfig, boolean hasWorkspace, boolean hasMemory,
                           boolean hasSecrets, List<String> files) {
            this.valid = valid;
            this.errorMessage = errorMessage;
            this.version = version;
            this.createdAt = createdAt;
            this.size = size;
            this.hasConfig = hasConfig;
            this.hasWorkspace = hasWorkspace;
            this.hasMemory = hasMemory;
            this.hasSecrets = hasSecrets;
            this.files = files;
        }
        
        public boolean isValid() { return valid; }
        public String getErrorMessage() { return errorMessage; }
        public String getVersion() { return version; }
        public String getCreatedAt() { return createdAt; }
        public long getSize() { return size; }
        public boolean hasConfig() { return hasConfig; }
        public boolean hasWorkspace() { return hasWorkspace; }
        public boolean hasMemory() { return hasMemory; }
        public boolean hasSecrets() { return hasSecrets; }
        public List<String> getFiles() { return files; }
    }
}
