package openclaw.gateway.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

/**
 * Device authentication V3 with signature verification.
 *
 * @author OpenClaw Team
 * @version 2026.3.13
 */
public class DeviceAuthV3 {
    
    private static final Logger logger = LoggerFactory.getLogger(DeviceAuthV3.class);
    
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final long MAX_TIMESTAMP_DIFF_MS = 5 * 60 * 1000; // 5 minutes
    
    private final String serverPublicKey;
    private final Map<String, DeviceCredentials> registeredDevices;
    
    public DeviceAuthV3(String serverPublicKey) {
        this.serverPublicKey = serverPublicKey;
        this.registeredDevices = new java.util.concurrent.ConcurrentHashMap<>();
    }
    
    /**
     * Register a new device.
     */
    public DeviceCredentials registerDevice(String deviceId, String devicePublicKey) {
        String apiKey = generateApiKey();
        DeviceCredentials credentials = new DeviceCredentials(
            deviceId,
            devicePublicKey,
            apiKey,
            Instant.now()
        );
        registeredDevices.put(deviceId, credentials);
        logger.info("Registered device: {}", deviceId);
        return credentials;
    }
    
    /**
     * Authenticate a device connection request.
     */
    public AuthResult authenticate(AuthRequest request) {
        try {
            // 1. Verify timestamp
            long timestamp = request.timestamp();
            long now = System.currentTimeMillis();
            if (Math.abs(now - timestamp) > MAX_TIMESTAMP_DIFF_MS) {
                return AuthResult.failed("Timestamp too old");
            }
            
            // 2. Get device credentials
            DeviceCredentials credentials = registeredDevices.get(request.deviceId());
            if (credentials == null) {
                return AuthResult.failed("Device not registered");
            }
            
            // 3. Build payload
            String payload = buildPayload(request);
            
            // 4. Verify signature
            if (!verifySignature(payload, request.signature(), credentials.devicePublicKey())) {
                return AuthResult.failed("Invalid signature");
            }
            
            // 5. Verify nonce (prevent replay)
            if (!verifyNonce(request.nonce())) {
                return AuthResult.failed("Invalid nonce");
            }
            
            // 6. Check permissions
            if (!checkPermissions(request.deviceId(), request.requestedScopes())) {
                return AuthResult.failed("Insufficient permissions");
            }
            
            logger.info("Device authenticated: {}", request.deviceId());
            return AuthResult.success(request.deviceId(), credentials.apiKey());
            
        } catch (Exception e) {
            logger.error("Authentication error", e);
            return AuthResult.failed("Authentication error: " + e.getMessage());
        }
    }
    
    /**
     * Build authentication payload.
     */
    private String buildPayload(AuthRequest request) {
        return String.format(
            "%s:%s:%s:%s:%d:%s",
            request.deviceId(),
            request.clientId(),
            String.join(",", request.requestedScopes()),
            request.nonce(),
            request.timestamp(),
            request.token()
        );
    }
    
    /**
     * Verify HMAC signature.
     */
    private boolean verifySignature(String payload, String signature, String publicKey) {
        try {
            // In real implementation, use proper signature verification
            // This is a simplified version
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(
                publicKey.getBytes(StandardCharsets.UTF_8),
                HMAC_ALGORITHM
            );
            mac.init(keySpec);
            byte[] computed = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String computedBase64 = Base64.getEncoder().encodeToString(computed);
            return computedBase64.equals(signature);
        } catch (Exception e) {
            logger.error("Signature verification failed", e);
            return false;
        }
    }
    
    /**
     * Verify nonce (prevent replay attacks).
     */
    private boolean verifyNonce(String nonce) {
        // In real implementation, store and check nonces
        // For now, just check format
        return nonce != null && nonce.length() >= 16;
    }
    
    /**
     * Check device permissions.
     */
    private boolean checkPermissions(String deviceId, String[] scopes) {
        // In real implementation, check against device permissions
        return true;
    }
    
    /**
     * Generate API key.
     */
    private String generateApiKey() {
        return "ak_" + UUID.randomUUID().toString().replace("-", "");
    }
    
    /**
     * Revoke device.
     */
    public boolean revokeDevice(String deviceId) {
        DeviceCredentials removed = registeredDevices.remove(deviceId);
        if (removed != null) {
            logger.info("Revoked device: {}", deviceId);
            return true;
        }
        return false;
    }
    
    // Records
    
    public record DeviceCredentials(
        String deviceId,
        String devicePublicKey,
        String apiKey,
        Instant registeredAt
    ) {}
    
    public record AuthRequest(
        String deviceId,
        String clientId,
        String[] requestedScopes,
        String nonce,
        long timestamp,
        String token,
        String signature
    ) {
        public static AuthRequest of(
            String deviceId,
            String clientId,
            String[] scopes,
            String nonce,
            String token,
            String signature
        ) {
            return new AuthRequest(
                deviceId, clientId, scopes, nonce,
                System.currentTimeMillis(), token, signature
            );
        }
    }
    
    public record AuthResult(
        boolean success,
        String deviceId,
        String apiKey,
        String error
    ) {
        public static AuthResult success(String deviceId, String apiKey) {
            return new AuthResult(true, deviceId, apiKey, null);
        }
        
        public static AuthResult failed(String error) {
            return new AuthResult(false, null, null, error);
        }
    }
}
