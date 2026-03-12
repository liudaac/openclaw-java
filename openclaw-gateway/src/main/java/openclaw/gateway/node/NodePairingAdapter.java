package openclaw.gateway.node;

import java.util.concurrent.CompletableFuture;

/**
 * Node pairing adapter for device pairing operations.
 *
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public interface NodePairingAdapter {

    /**
     * Initiates a pairing challenge.
     *
     * @return the challenge
     */
    CompletableFuture<PairingChallenge> initiateChallenge();

    /**
     * Verifies a pairing response.
     *
     * @param challenge the challenge
     * @param response the response
     * @return verification result
     */
    CompletableFuture<PairingResult> verifyResponse(PairingChallenge challenge, String response);

    /**
     * Checks if a device is paired.
     *
     * @param deviceId the device ID
     * @return true if paired
     */
    CompletableFuture<Boolean> isPaired(String deviceId);

    /**
     * Unpairs a device.
     *
     * @param deviceId the device ID
     * @return completion future
     */
    CompletableFuture<Void> unpair(String deviceId);

    /**
     * Pairing challenge.
     *
     * @param challengeId the challenge ID
     * @param challengeText the challenge text to display
     * @param expiresAt expiration timestamp
     */
    record PairingChallenge(
            String challengeId,
            String challengeText,
            long expiresAt
    ) {
    }

    /**
     * Pairing result.
     *
     * @param success whether pairing succeeded
     * @param deviceId the paired device ID
     * @param message result message
     */
    record PairingResult(
            boolean success,
            String deviceId,
            String message
    ) {

        /**
         * Creates a successful pairing result.
         *
         * @param deviceId the device ID
         * @return the result
         */
        public static PairingResult success(String deviceId) {
            return new PairingResult(true, deviceId, "Paired successfully");
        }

        /**
         * Creates a failed pairing result.
         *
         * @param message the error message
         * @return the result
         */
        public static PairingResult failure(String message) {
            return new PairingResult(false, null, message);
        }
    }
}
