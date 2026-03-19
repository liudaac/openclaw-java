package openclaw.gateway.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Device pairing service with shared auth support.
 *
 * <p>Ported from original: 1d3e596021 - fix(pairing): include shared auth in setup codes</p>
 *
 * @author OpenClaw Team
 * @version 2026.3.19
 */
@Service
public class DevicePairingService {

    private static final Logger logger = LoggerFactory.getLogger(DevicePairingService.class);
    private static final Duration SETUP_CODE_TTL = Duration.ofMinutes(5);
    private static final int CODE_LENGTH = 6;

    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<String, SetupCode> activeCodes = new ConcurrentHashMap<>();
    private final Set<String> usedTokens = ConcurrentHashMap.newKeySet();

    /**
     * Generates a setup code for device pairing.
     *
     * @param device the device to pair
     * @return the setup code
     */
    public SetupCode generateSetupCode(Device device) {
        String code = generateNumericCode();
        String sharedAuthToken = generateSharedAuthToken(device);

        SetupCode setupCode = new SetupCode(
                code,
                sharedAuthToken,
                Instant.now().plus(SETUP_CODE_TTL)
        );

        activeCodes.put(code, setupCode);
        logger.info("Generated setup code for device: {}", device.id());

        return setupCode;
    }

    /**
     * Verifies a setup code.
     *
     * @param code the code to verify
     * @return the setup code if valid, null otherwise
     */
    public SetupCode verifySetupCode(String code) {
        SetupCode setupCode = activeCodes.get(code);

        if (setupCode == null) {
            logger.warn("Setup code not found: {}", code);
            return null;
        }

        // Check if already used
        if (usedTokens.contains(setupCode.sharedAuthToken())) {
            logger.warn("Setup code already used: {}", code);
            return null;
        }

        // Check expiration
        if (setupCode.expiresAt().isBefore(Instant.now())) {
            logger.warn("Setup code expired: {}", code);
            activeCodes.remove(code);
            return null;
        }

        // Mark as used
        usedTokens.add(setupCode.sharedAuthToken());
        activeCodes.remove(code);

        logger.info("Setup code verified successfully: {}", code);
        return setupCode;
    }

    /**
     * Generates a numeric setup code.
     *
     * @return the code
     */
    private String generateNumericCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(secureRandom.nextInt(10));
        }
        return sb.toString();
    }

    /**
     * Generates a shared auth token for the device.
     *
     * @param device the device
     * @return the token
     */
    private String generateSharedAuthToken(Device device) {
        byte[] tokenBytes = new byte[32];
        secureRandom.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    /**
     * Cleans up expired setup codes.
     */
    public void cleanupExpiredCodes() {
        Instant now = Instant.now();
        activeCodes.entrySet().removeIf(entry -> {
            if (entry.getValue().expiresAt().isBefore(now)) {
                logger.debug("Cleaned up expired setup code: {}", entry.getKey());
                return true;
            }
            return false;
        });
    }

    /**
     * Setup code record.
     *
     * @param code the display code
     * @param sharedAuthToken the shared auth token
     * @param expiresAt expiration time
     */
    public record SetupCode(
            String code,
            String sharedAuthToken,
            Instant expiresAt
    ) {
    }

    /**
     * Device record.
     *
     * @param id device ID
     * @param name device name
     */
    public record Device(
            String id,
            String name
    ) {
    }
}
