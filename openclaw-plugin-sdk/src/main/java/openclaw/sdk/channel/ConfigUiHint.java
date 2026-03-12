package openclaw.sdk.channel;

import java.util.List;
import java.util.Optional;

/**
 * UI hint for a configuration field.
 *
 * @param label the field label
 * @param help the help text
 * @param tags optional tags
 * @param advanced whether this is an advanced setting
 * @param sensitive whether this contains sensitive data
 * @param placeholder the placeholder text
 * @param itemTemplate optional template for array items
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public record ConfigUiHint(
        Optional<String> label,
        Optional<String> help,
        List<String> tags,
        boolean advanced,
        boolean sensitive,
        Optional<String> placeholder,
        Optional<Object> itemTemplate
) {

    /**
     * Creates a basic UI hint.
     *
     * @param label the label
     * @return the hint
     */
    public static ConfigUiHint basic(String label) {
        return new ConfigUiHint(
                Optional.of(label),
                Optional.empty(),
                List.of(),
                false,
                false,
                Optional.empty(),
                Optional.empty()
        );
    }

    /**
     * Creates a builder for ConfigUiHint.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for ConfigUiHint.
     */
    public static class Builder {
        private String label;
        private String help;
        private List<String> tags = List.of();
        private boolean advanced = false;
        private boolean sensitive = false;
        private String placeholder;
        private Object itemTemplate;

        public Builder label(String label) {
            this.label = label;
            return this;
        }

        public Builder help(String help) {
            this.help = help;
            return this;
        }

        public Builder tags(List<String> tags) {
            this.tags = tags != null ? tags : List.of();
            return this;
        }

        public Builder advanced(boolean advanced) {
            this.advanced = advanced;
            return this;
        }

        public Builder sensitive(boolean sensitive) {
            this.sensitive = sensitive;
            return this;
        }

        public Builder placeholder(String placeholder) {
            this.placeholder = placeholder;
            return this;
        }

        public Builder itemTemplate(Object itemTemplate) {
            this.itemTemplate = itemTemplate;
            return this;
        }

        public ConfigUiHint build() {
            return new ConfigUiHint(
                    Optional.ofNullable(label),
                    Optional.ofNullable(help),
                    tags,
                    advanced,
                    sensitive,
                    Optional.ofNullable(placeholder),
                    Optional.ofNullable(itemTemplate)
            );
        }
    }
}
