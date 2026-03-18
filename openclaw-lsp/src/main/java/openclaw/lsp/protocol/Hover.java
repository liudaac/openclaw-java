package openclaw.lsp.protocol;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Hover result.
 *
 * @author OpenClaw Team
 * @version 2026.3.18
 * @since 2026.3.0
 */
public class Hover {

    @JsonProperty("contents")
    private Object contents;

    @JsonProperty("range")
    private Range range;

    public Hover() {
    }

    public Object getContents() {
        return contents;
    }

    public void setContents(Object contents) {
        this.contents = contents;
    }

    public Range getRange() {
        return range;
    }

    public void setRange(Range range) {
        this.range = range;
    }

    /**
     * Get contents as string.
     *
     * @return the contents string
     */
    public String getContentsAsString() {
        if (contents == null) {
            return "";
        }
        if (contents instanceof String) {
            return (String) contents;
        }
        if (contents instanceof List) {
            StringBuilder sb = new StringBuilder();
            for (Object item : (List<?>) contents) {
                if (item instanceof String) {
                    sb.append(item).append("\n");
                } else if (item instanceof MarkedString) {
                    sb.append(((MarkedString) item).getValue()).append("\n");
                }
            }
            return sb.toString().trim();
        }
        if (contents instanceof MarkupContent) {
            return ((MarkupContent) contents).getValue();
        }
        return contents.toString();
    }

    /**
     * Marked string.
     */
    public static class MarkedString {
        @JsonProperty("language")
        private String language;

        @JsonProperty("value")
        private String value;

        public String getLanguage() {
            return language;
        }

        public void setLanguage(String language) {
            this.language = language;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    /**
     * Markup content.
     */
    public static class MarkupContent {
        @JsonProperty("kind")
        private String kind;

        @JsonProperty("value")
        private String value;

        public String getKind() {
            return kind;
        }

        public void setKind(String kind) {
            this.kind = kind;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    /**
     * Range.
     */
    public static class Range {
        @JsonProperty("start")
        private Position start;

        @JsonProperty("end")
        private Position end;

        public Position getStart() {
            return start;
        }

        public void setStart(Position start) {
            this.start = start;
        }

        public Position getEnd() {
            return end;
        }

        public void setEnd(Position end) {
            this.end = end;
        }
    }
}
