package openclaw.agent.workspace;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for WorkspaceFileService.
 *
 * @author OpenClaw Team
 * @version 2026.3.17
 */
class WorkspaceFileServiceTest {

    @TempDir
    Path tempDir;

    private WorkspaceFileService service;

    @BeforeEach
    void setUp() {
        service = new WorkspaceFileService(tempDir.toString());
    }

    @Test
    void testReadFile_Exists() throws IOException {
        Path file = tempDir.resolve("test.txt");
        Files.writeString(file, "Hello World");

        Optional<String> content = service.readFile("test.txt");
        assertTrue(content.isPresent());
        assertEquals("Hello World", content.get());
    }

    @Test
    void testReadFile_NotExists() {
        Optional<String> content = service.readFile("nonexistent.txt");
        assertFalse(content.isPresent());
    }

    @Test
    void testReadFile_Null() {
        Optional<String> content = service.readFile(null);
        assertFalse(content.isPresent());
    }

    @Test
    void testExists_True() throws IOException {
        Path file = tempDir.resolve("exists.txt");
        Files.createFile(file);
        assertTrue(service.exists("exists.txt"));
    }

    @Test
    void testExists_False() {
        assertFalse(service.exists("notexists.txt"));
    }

    @Test
    void testReadHeartbeatMd() throws IOException {
        Path file = tempDir.resolve("HEARTBEAT.md");
        Files.writeString(file, "# Tasks\n- Check email");

        Optional<String> content = service.readHeartbeatMd();
        assertTrue(content.isPresent());
        assertTrue(content.get().contains("Check email"));
    }

    @Test
    void testAssembleContext() throws IOException {
        // Create test files
        Files.writeString(tempDir.resolve("AGENTS.md"), "# Agents");
        Files.writeString(tempDir.resolve("SOUL.md"), "# Soul");
        Files.writeString(tempDir.resolve("HEARTBEAT.md"), "# Heartbeat");

        String context = service.assembleContext(true);
        assertTrue(context.contains("AGENTS.md"));
        assertTrue(context.contains("SOUL.md"));
        assertTrue(context.contains("HEARTBEAT.md"));
    }

    @Test
    void testAssembleContext_WithoutHeartbeat() throws IOException {
        Files.writeString(tempDir.resolve("HEARTBEAT.md"), "# Heartbeat");

        String context = service.assembleContext(false);
        assertFalse(context.contains("HEARTBEAT.md"));
    }
}
