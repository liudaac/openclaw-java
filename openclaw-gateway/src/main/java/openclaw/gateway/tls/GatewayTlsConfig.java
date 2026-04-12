package openclaw.gateway.tls;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Gateway TLS configuration.
 *
 * <p>Represents TLS/SSL configuration for secure gateway connections.</p>
 *
 * @author OpenClaw Team
 * @version 2026.4.12
 */
public record GatewayTlsConfig(
        boolean enabled,
        Optional<Path> certificatePath,
        Optional<Path> privateKeyPath,
        Optional<Path> caCertificatePath,
        boolean verifyClientCert,
        Optional<String> tlsVersion
) {
    
    /**
     * Creates a TLS config with default settings (disabled).
     */
    public GatewayTlsConfig() {
        this(false, Optional.empty(), Optional.empty(), Optional.empty(), false, Optional.of("TLSv1.3"));
    }
    
    /**
     * Creates an enabled TLS config with certificate paths.
     */
    public static GatewayTlsConfig enabled(Path certPath, Path keyPath) {
        return new GatewayTlsConfig(
            true, 
            Optional.of(certPath), 
            Optional.of(keyPath), 
            Optional.empty(), 
            false, 
            Optional.of("TLSv1.3")
        );
    }
    
    /**
     * Creates an enabled TLS config with CA verification.
     */
    public static GatewayTlsConfig withCaVerification(Path certPath, Path keyPath, Path caPath) {
        return new GatewayTlsConfig(
            true, 
            Optional.of(certPath), 
            Optional.of(keyPath), 
            Optional.of(caPath), 
            true, 
            Optional.of("TLSv1.3")
        );
    }
}
