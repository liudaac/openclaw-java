package openclaw.desktop.component;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.commonmark.node.*;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Markdown Renderer for JavaFX.
 *
 * <p>Converts Markdown to JavaFX nodes with syntax highlighting.</p>
 */
public class MarkdownRenderer {

    private final Parser parser;
    private final HtmlRenderer htmlRenderer;

    // Code block pattern
    private static final Pattern CODE_BLOCK_PATTERN = Pattern.compile(
        "```(\\w+)?\\n(.*?)\\n```", Pattern.DOTALL);

    // Inline code pattern
    private static final Pattern INLINE_CODE_PATTERN = Pattern.compile("`([^`]+)`");

    // Bold pattern
    private static final Pattern BOLD_PATTERN = Pattern.compile("\\*\\*([^*]+)\\*\\*");

    // Italic pattern
    private static final Pattern ITALIC_PATTERN = Pattern.compile("\\*([^*]+)\\*");

    // Link pattern
    private static final Pattern LINK_PATTERN = Pattern.compile("\\[([^\\]]+)\\]\\(([^)]+)\\)");

    public MarkdownRenderer() {
        this.parser = Parser.builder().build();
        this.htmlRenderer = HtmlRenderer.builder().build();
    }

    /**
     * Render markdown text to JavaFX nodes.
     */
    public List<Node> render(String markdown) {
        List<Node> nodes = new ArrayList<>();

        if (markdown == null || markdown.isEmpty()) {
            return nodes;
        }

        // Split by code blocks first
        String[] parts = markdown.split("(?=```)");

        for (String part : parts) {
            if (part.startsWith("```")) {
                // Code block
                nodes.add(renderCodeBlock(part));
            } else {
                // Regular text - process inline formatting
                nodes.addAll(renderTextBlocks(part));
            }
        }

        return nodes;
    }

    /**
     * Render a code block.
     */
    private Node renderCodeBlock(String codeBlock) {
        Matcher matcher = CODE_BLOCK_PATTERN.matcher(codeBlock);
        if (matcher.find()) {
            String language = matcher.group(1);
            String code = matcher.group(2);
            return new CodeBlockView(language, code);
        }
        return new Label(codeBlock);
    }

    /**
     * Render text blocks with inline formatting.
     */
    private List<Node> renderTextBlocks(String text) {
        List<Node> nodes = new ArrayList<>();

        // Split by paragraphs
        String[] paragraphs = text.split("\\n\\n");

        for (String paragraph : paragraphs) {
            if (paragraph.trim().isEmpty()) {
                continue;
            }


            // Check for headers
            if (paragraph.startsWith("# ")) {
                nodes.add(createHeader(paragraph.substring(2), 1));
            } else if (paragraph.startsWith("## ")) {
                nodes.add(createHeader(paragraph.substring(3), 2));
            } else if (paragraph.startsWith("### ")) {
                nodes.add(createHeader(paragraph.substring(4), 3));
            } else if (paragraph.startsWith("- ") || paragraph.startsWith("* ")) {
                // List item
                nodes.add(createListItem(paragraph.substring(2)));
            } else {
                // Regular paragraph with inline formatting
                nodes.add(createParagraph(paragraph));
            }
        }

        return nodes;
    }

    /**
     * Create a header node.
     */
    private Node createHeader(String text, int level) {
        Label label = new Label(text);
        label.getStyleClass().add("markdown-h" + level);
        return label;
    }

    /**
     * Create a list item node.
     */
    private Node createListItem(String text) {
        HBoxWithStyle box = new HBoxWithStyle();
        box.getStyleClass().add("markdown-list-item");

        Label bullet = new Label("• ");
        bullet.getStyleClass().add("markdown-bullet");

        TextFlow content = createInlineText(text);

        box.addChildren(bullet, content);
        return box;
    }

    /**
     * Create a paragraph with inline formatting.
     */
    private Node createParagraph(String text) {
        return createInlineText(text);
    }

    /**
     * Create inline text with formatting.
     */
    private TextFlow createInlineText(String text) {
        TextFlow flow = new TextFlow();
        flow.getStyleClass().add("markdown-paragraph");

        // Process inline code
        text = processInlinePatterns(text);

        // Create text segments
        List<TextSegment> segments = parseInlineFormatting(text);

        for (TextSegment segment : segments) {
            Text t = new Text(segment.text);
            t.getStyleClass().addAll(segment.styles);
            flow.getChildren().add(t);
        }

        return flow;
    }

    /**
     * Process inline patterns and mark them.
     */
    private String processInlinePatterns(String text) {
        // This is a simplified approach
        // In production, use a proper parser
        return text;
    }

    /**
     * Parse inline formatting.
     */
    private List<TextSegment> parseInlineFormatting(String text) {
        List<TextSegment> segments = new ArrayList<>();

        // Simple parsing - split by patterns
        int lastEnd = 0;

        // Process inline code
        Matcher codeMatcher = INLINE_CODE_PATTERN.matcher(text);
        while (codeMatcher.find()) {
            if (codeMatcher.start() > lastEnd) {
                segments.add(new TextSegment(text.substring(lastEnd, codeMatcher.start()), List.of()));
            }
            segments.add(new TextSegment(codeMatcher.group(1), List.of("markdown-code-inline")));
            lastEnd = codeMatcher.end();
        }

        if (lastEnd < text.length()) {
            segments.add(new TextSegment(text.substring(lastEnd), List.of()));
        }

        // If no code found, just add the whole text
        if (segments.isEmpty()) {
            segments.add(new TextSegment(text, List.of()));
        }

        return segments;
    }

    /**
     * Text segment with styles.
     */
    private record TextSegment(String text, List<String> styles) {}

    /**
     * Simple HBox wrapper.
     */
    private static class HBoxWithStyle extends javafx.scene.layout.HBox {
        public HBoxWithStyle() {
            super(4);
        }

        public void addChildren(Node... nodes) {
            getChildren().addAll(nodes);
        }
    }
}
