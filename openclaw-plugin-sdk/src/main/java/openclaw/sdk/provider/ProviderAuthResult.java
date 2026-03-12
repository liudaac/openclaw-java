package openclaw.sdk.provider;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Provider authentication result.
 *
 * @param profiles the auth profiles
 * @param configPatch optional config patch
 * @param defaultModel optional default model
 * @param notes optional notes
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public record ProviderAuthResult(
        List<AuthProfile> profiles,
        Optional<Map<String, Object>> configPatch,
        Optional<String> defaultModel,
        Optional<List<String>> notes
) {

    /**
     * Creates a builder for ProviderAuthResult.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for ProviderAuthResult.
     */
    public static class Builder {
        private List<AuthProfile> profiles = List.of();
        private Map<String, Object> configPatch;
        private String defaultModel;
        private List<String> notes;

        public Builder profiles(List<AuthProfile> profiles) {
            this.profiles = profiles != null ? profiles : List.of();
            return this;
        }

        public Builder configPatch(Map<String, Object> configPatch) {
            this.configPatch = configPatch;
            return this;
        }

        public Builder defaultModel(String defaultModel) {
            this.defaultModel = defaultModel;
            return this;
        }

        public Builder notes(List<String> notes) {
            this.notes = notes;
            return this;
        }

        public ProviderAuthResult build() {
            return new ProviderAuthResult(
                    profiles,
                    Optional.ofNullable(configPatch),
                    Optional.ofNullable(defaultModel),
                    Optional.ofNullable(notes)
            );
        }
    }

    /**
     * Authentication profile.
     *
     * @param profileId the profile ID
     * @param credential the credential
     */
    public record AuthProfile(
            String profileId,
            Map<String, Object> credential
    ) {
    }
}
