package openclaw.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SshSandboxSecurityTest {

    private final SshSandboxSecurity security = new SshSandboxSecurity();

    @Test
    void testValidPathWithinSandbox() {
        assertDoesNotThrow(() -> {
            security.validateUploadPath("file.txt", "/sandbox");
        });
    }

    @Test
    void testPathEscapesSandbox() {
        assertThrows(SecurityException.class, () -> {
            security.validateUploadPath("../etc/passwd", "/sandbox");
        });
    }

    @Test
    void testPathWithTilde() {
        assertThrows(SecurityException.class, () -> {
            security.validateUploadPath("~/.ssh/id_rsa", "/sandbox");
        });
    }

    @Test
    void testIsWithinSandbox() {
        assertTrue(security.isWithinSandbox("subdir/file.txt", "/sandbox"));
        assertFalse(security.isWithinSandbox("../outside", "/sandbox"));
    }
}
