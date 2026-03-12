package openclaw.secrets.credential;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Credential matrix for managing multiple credentials.
 *
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public class CredentialMatrix {

    private final Map<String, Credential> credentials = new ConcurrentHashMap<>();

    /**
     * Adds a credential.
     *
     * @param credential the credential
     */
    public void add(Credential credential) {
        credentials.put(credential.profileId(), credential);
    }

    /**
     * Gets a credential by profile ID.
     *
     * @param profileId the profile ID
     * @return the credential if found
     */
    public Optional<Credential> get(String profileId) {
        return Optional.ofNullable(credentials.get(profileId));
    }

    /**
     * Removes a credential.
     *
     * @param profileId the profile ID
     * @return the removed credential if present
     */
    public Optional<Credential> remove(String profileId) {
        return Optional.ofNullable(credentials.remove(profileId));
    }

    /**
     * Lists all credentials.
     *
     * @return list of credentials
     */
    public List<Credential> list() {
        return List.copyOf(credentials.values());
    }

    /**
     * Lists credentials by type.
     *
     * @param type the credential type
     * @return list of credentials
     */
    public List<Credential> listByType(Credential.CredentialType type) {
        return credentials.values().stream()
                .filter(c -> c.type() == type)
                .toList();
    }

    /**
     * Checks if a credential exists.
     *
     * @param profileId the profile ID
     * @return true if exists
     */
    public boolean has(String profileId) {
        return credentials.containsKey(profileId);
    }

    /**
     * Gets the count of credentials.
     *
     * @return the count
     */
    public int size() {
        return credentials.size();
    }

    /**
     * Clears all credentials.
     */
    public void clear() {
        credentials.clear();
    }

    /**
     * Gets credentials that are expired.
     *
     * @return list of expired credentials
     */
    public List<Credential> getExpired() {
        return credentials.values().stream()
                .filter(Credential::isExpired)
                .toList();
    }

    /**
     * Gets credentials that will expire soon.
     *
     * @param days number of days
     * @return list of expiring credentials
     */
    public List<Credential> getExpiringSoon(int days) {
        java.time.Instant threshold = java.time.Instant.now()
                .plusSeconds(days * 24 * 60 * 60);
        
        return credentials.values().stream()
                .filter(c -> c.expiresAt()
                        .map(exp -> exp.isBefore(threshold) && !c.isExpired())
                        .orElse(false))
                .toList();
    }
}
