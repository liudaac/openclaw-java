package openclaw.sdk.channel;

import java.util.List;
import java.util.Optional;

/**
 * Channel metadata.
 *
 * @param name the human-readable name
 * @param description the channel description
 * @param icon optional icon URL or identifier
 * @param color optional brand color
 * @param docsUrl optional documentation URL
 * @param tags optional tags for categorization
 * @author OpenClaw Team
 * @version 2026.3.9
 */
public record ChannelMeta(
        String name,
        String description,
        Optional<String> icon,
        Optional<String> color,
        Optional<String> docsUrl,
        List<String> tags
) {

    /**
     * Creates a builder for ChannelMeta.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for ChannelMeta.
     */
    public static class Builder {
        private String name;
        private String description;
        private String icon;
        private String color;
        private String docsUrl;
        private List<String> tags = List.of();

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder icon(String icon) {
            this.icon = icon;
            return this;
        }

        public Builder color(String color) {
            this.color = color;
            return this;
        }

        public Builder docsUrl(String docsUrl) {
            this.docsUrl = docsUrl;
            return this;
        }

        public Builder tags(List<String> tags) {
            this.tags = tags != null ? tags : List.of();
            return this;
        }

        public ChannelMeta build() {
            return new ChannelMeta(
                    name,
                    description,
                    Optional.ofNullable(icon),
                    Optional.ofNullable(color),
                    Optional.ofNullable(docsUrl),
                    tags
            );
        }
    }
}
