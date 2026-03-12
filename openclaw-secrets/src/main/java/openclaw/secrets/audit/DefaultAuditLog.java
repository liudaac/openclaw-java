package openclaw.secrets.audit;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

/**
 * Default audit log implementation with file persistence.
 *
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public class DefaultAuditLog implements AuditLog {

    private static final DateTimeFormatter TIMESTAMP_FORMAT = 
            DateTimeFormatter.ISO_INSTANT;

    private final Path logDir;
    private final int retentionDays;
    private final BlockingQueue<AuditEntry> queue;
    private final Thread writerThread;
    private volatile boolean running = true;

    public DefaultAuditLog(Path logDir, int retentionDays) {
        this.logDir = logDir;
        this.retentionDays = retentionDays;
        this.queue = new LinkedBlockingQueue<>();
        
        // Ensure log directory exists
        try {
            Files.createDirectories(logDir);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create audit log directory", e);
        }
        
        // Start writer thread
        this.writerThread = new Thread(this::writeLoop);
        this.writerThread.setDaemon(true);
        this.writerThread.setName("audit-log-writer");
        this.writerThread.start();
    }

    @Override
    public void log(Action action, String target, Map<String, Object> details) {
        AuditEntry entry = new AuditEntry(
                Instant.now(),
                action,
                target,
                details,
                true
        );
        queue.offer(entry);
    }

    @Override
    public List<AuditEntry> getEntries(int limit) {
        return readEntries().stream()
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public List<AuditEntry> getEntriesForTarget(String target, int limit) {
        return readEntries().stream()
                .filter(e -> e.target().equals(target))
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public List<AuditEntry> getEntriesSince(Instant since, int limit) {
        return readEntries().stream()
                .filter(e -> e.timestamp().isAfter(since))
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public void clearOldEntries(Instant olderThan) {
        List<AuditEntry> entries = readEntries();
        List<AuditEntry> filtered = entries.stream()
                .filter(e -> e.timestamp().isAfter(olderThan))
                .toList();
        
        // Rewrite log file with filtered entries
        Path logFile = getCurrentLogFile();
        try (BufferedWriter writer = Files.newBufferedWriter(logFile)) {
            for (AuditEntry entry : filtered) {
                writer.write(formatEntry(entry));
                writer.newLine();
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to clear old entries", e);
        }
    }

    @Override
    public void close() {
        running = false;
        writerThread.interrupt();
        try {
            writerThread.join(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        // Flush remaining entries
        flushQueue();
    }

    private void writeLoop() {
        while (running) {
            try {
                AuditEntry entry = queue.take();
                writeEntry(entry);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void writeEntry(AuditEntry entry) {
        Path logFile = getCurrentLogFile();
        try (BufferedWriter writer = Files.newBufferedWriter(logFile, 
                java.nio.file.StandardOpenOption.CREATE, 
                java.nio.file.StandardOpenOption.APPEND)) {
            writer.write(formatEntry(entry));
            writer.newLine();
        } catch (IOException e) {
            // Log error but don't throw
            System.err.println("Failed to write audit entry: " + e.getMessage());
        }
    }

    private void flushQueue() {
        AuditEntry entry;
        while ((entry = queue.poll()) != null) {
            writeEntry(entry);
        }
    }

    private String formatEntry(AuditEntry entry) {
        StringBuilder sb = new StringBuilder();
        sb.append(TIMESTAMP_FORMAT.format(entry.timestamp())).append("\t");
        sb.append(entry.action()).append("\t");
        sb.append(entry.target()).append("\t");
        sb.append(entry.success()).append("\t");
        sb.append(formatDetails(entry.details()));
        return sb.toString();
    }

    private String formatDetails(Map<String, Object> details) {
        if (details == null || details.isEmpty()) {
            return "{}";
        }
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : details.entrySet()) {
            if (!first) sb.append(",");
            sb.append(entry.getKey()).append("=").append(entry.getValue());
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }

    private List<AuditEntry> readEntries() {
        Path logFile = getCurrentLogFile();
        if (!Files.exists(logFile)) {
            return List.of();
        }
        
        try (BufferedReader reader = Files.newBufferedReader(logFile)) {
            return reader.lines()
                    .map(this::parseEntry)
                    .filter(e -> e != null)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            return List.of();
        }
    }

    private AuditEntry parseEntry(String line) {
        try {
            String[] parts = line.split("\t", 5);
            if (parts.length < 4) {
                return null;
            }
            
            Instant timestamp = Instant.parse(parts[0]);
            Action action = Action.valueOf(parts[1]);
            String target = parts[2];
            boolean success = Boolean.parseBoolean(parts[3]);
            Map<String, Object> details = parts.length > 4 ? 
                    parseDetails(parts[4]) : Map.of();
            
            return new AuditEntry(timestamp, action, target, details, success);
        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, Object> parseDetails(String detailsStr) {
        // Simple parsing - in production use proper JSON
        return Map.of();
    }

    private Path getCurrentLogFile() {
        String date = java.time.LocalDate.now().toString();
        return logDir.resolve("audit-" + date + ".log");
    }
}
