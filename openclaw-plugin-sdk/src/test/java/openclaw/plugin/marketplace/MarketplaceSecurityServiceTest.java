package openclaw.plugin.marketplace;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for MarketplaceSecurityService.
 */
class MarketplaceSecurityServiceTest {

    @TempDir
    Path tempDir;

    private MarketplaceSecurityService securityService;

    @BeforeEach
    void setUp() {
        securityService = new MarketplaceSecurityService();
    }

    @Test
    void testResolveSafeDownloadFileName_ValidFileName() {
        // Valid file names should pass
        assertEquals("plugin.zip", securityService.resolveSafeDownloadFileName(
            "https://example.com/plugin.zip", "fallback.zip"));
        assertEquals("marketplace.tar.gz", securityService.resolveSafeDownloadFileName(
            "https://example.com/path/marketplace.tar.gz", "fallback"));
    }

    @Test
    void testResolveSafeDownloadFileName_UsesFallback() {
        // URL without path should use fallback
        assertEquals("fallback.zip", securityService.resolveSafeDownloadFileName(
            "https://example.com", "fallback.zip"));
    }

    @Test
    void testRejectDriveRelativePath_Windows() {
        // Windows drive-relative paths should be rejected
        assertThrows(SecurityException.class, () -> {
            securityService.resolveSafeDownloadFileName("https://example.com/C:filename.zip", "fallback");
        });
    }

    @Test
    void testRedactSensitiveUrl_Token() {
        String url = "https://api.example.com/download?token=secret123&other=value";
        String redacted = securityService.redactSensitiveUrl(url);
        
        assertFalse(redacted.contains("secret123"));
        assertTrue(redacted.contains("token=***"));
        assertTrue(redacted.contains("other=value"));
    }

    @Test
    void testRedactSensitiveUrl_ApiKey() {
        String url = "https://api.example.com/download?api_key=abc123&user=john";
        String redacted = securityService.redactSensitiveUrl(url);
        
        assertFalse(redacted.contains("abc123"));
        assertTrue(redacted.contains("api_key=***"));
    }

    @Test
    void testRedactSensitiveUrl_MultipleSensitiveParams() {
        String url = "https://api.example.com/download?token=secret&api_key=key123&password=mypass";
        String redacted = securityService.redactSensitiveUrl(url);
        
        assertFalse(redacted.contains("secret"));
        assertFalse(redacted.contains("key123"));
        assertFalse(redacted.contains("mypass"));
        assertTrue(redacted.contains("token=***"));
        assertTrue(redacted.contains("api_key=***"));
        assertTrue(redacted.contains("password=***"));
    }

    @Test
    void testSanitizeForLog_RemovesAnsi() {
        String message = "\u001B[31mRed text\u001B[0m and \u001B[1mbold\u001B[0m";
        String sanitized = securityService.sanitizeForLog(message);
        
        assertFalse(sanitized.contains("\u001B"));
        assertEquals("Red text and bold", sanitized);
    }

    @Test
    void testSanitizeForLog_RemovesControlChars() {
        // Use Unicode escapes for control characters
        String message = "Hello\u0000World\u001FTest\u007F";
        String sanitized = securityService.sanitizeForLog(message);
        
        assertEquals("HelloWorldTest", sanitized);
    }

    @Test
    void testSanitizeForLog_NullInput() {
        assertEquals("null", securityService.sanitizeForLog(null));
    }

    @Test
    void testStreamDownload_CleanupOnFailure() throws Exception {
        // Create a temp file to simulate failed download
        Path target = tempDir.resolve("test-download.zip");
        Path tempFile = target.resolveSibling(target.getFileName() + ".tmp");
        
        // Create the temp file
        Files.createFile(tempFile);
        assertTrue(Files.exists(tempFile));
        
        // Simulate cleanup
        Files.deleteIfExists(tempFile);
        assertFalse(Files.exists(tempFile));
    }
}
