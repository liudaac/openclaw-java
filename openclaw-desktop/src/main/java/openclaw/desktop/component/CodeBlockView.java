package openclaw.desktop.component;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Code Block View Component.
 *
 * <p>Displays code blocks with syntax highlighting.</p>
 */
public class CodeBlockView extends VBox {

    private static final Map<String, Color> KEYWORD_COLORS = new HashMap<>();
    private static final Map<String, Color> TYPE_COLORS = new HashMap<>();
    private static final Map<String, Color> STRING_COLORS = new HashMap<>();

    static {
        // Java keywords
        KEYWORD_COLORS.put("public", Color.web("#ff7b72"));
        KEYWORD_COLORS.put("private", Color.web("#ff7b72"));
        KEYWORD_COLORS.put("protected", Color.web("#ff7b72"));
        KEYWORD_COLORS.put("static", Color.web("#ff7b72"));
        KEYWORD_COLORS.put("final", Color.web("#ff7b72"));
        KEYWORD_COLORS.put("void", Color.web("#ff7b72"));
        KEYWORD_COLORS.put("return", Color.web("#ff7b72"));
        KEYWORD_COLORS.put("if", Color.web("#ff7b72"));
        KEYWORD_COLORS.put("else", Color.web("#ff7b72"));
        KEYWORD_COLORS.put("for", Color.web("#ff7b72"));
        KEYWORD_COLORS.put("while", Color.web("#ff7b72"));
        KEYWORD_COLORS.put("class", Color.web("#ff7b72"));
        KEYWORD_COLORS.put("interface", Color.web("#ff7b72"));
        KEYWORD_COLORS.put("extends", Color.web("#ff7b72"));
        KEYWORD_COLORS.put("implements", Color.web("#ff7b72"));
        KEYWORD_COLORS.put("new", Color.web("#ff7b72"));
        KEYWORD_COLORS.put("this", Color.web("#ff7b72"));
        KEYWORD_COLORS.put("super", Color.web("#ff7b72"));
        KEYWORD_COLORS.put("try", Color.web("#ff7b72"));
        KEYWORD_COLORS.put("catch", Color.web("#ff7b72"));
        KEYWORD_COLORS.put("throw", Color.web("#ff7b72"));
        KEYWORD_COLORS.put("import", Color.web("#ff7b72"));
        KEYWORD_COLORS.put("package", Color.web("#ff7b72"));

        // Types
        TYPE_COLORS.put("String", Color.web("#ffa657"));
        TYPE_COLORS.put("Integer", Color.web("#ffa657"));
        TYPE_COLORS.put("int", Color.web("#ffa657"));
        TYPE_COLORS.put("Boolean", Color.web("#ffa657"));
        TYPE_COLORS.put("boolean", Color.web("#ffa657"));
        TYPE_COLORS.put("Double", Color.web("#ffa657"));
        TYPE_COLORS.put("double", Color.web("#ffa657"));
        TYPE_COLORS.put("List", Color.web("#ffa657"));
        TYPE_COLORS.put("Map", Color.web("#ffa657"));
        TYPE_COLORS.put("Set", Color.web("#ffa657"));
        TYPE_COLORS.put("Object", Color.web("#ffa657"));
    }

    public CodeBlockView(String language, String code) {
        getStyleClass().add("code-block");
        setSpacing(0);
        setPadding(new Insets(0));

        // Header with language label
        HBox header = new HBox();
        header.getStyleClass().add("code-block-header");
        header.setPadding(new Insets(8, 12, 8, 12));

        Label langLabel = new Label(language != null ? language : "code");
        langLabel.getStyleClass().add("code-language");

        header.getChildren().add(langLabel);

        // Code content
        VBox codeContent = new VBox();
        codeContent.getStyleClass().add("code-content");
        codeContent.setPadding(new Insets(12));

        codeContent.setFillWidth(true);


        // Syntax highlight
        String[] lines = code.split("\\n");
        for (String line : lines) {
            HBox lineBox = highlightLine(line);
            codeContent.getChildren().add(lineBox);
        }

        getChildren().addAll(header, codeContent);
    }

    /**
     * Highlight a line of code.
     */
    private HBox highlightLine(String line) {
        HBox lineBox = new HBox();
        lineBox.getStyleClass().add("code-line");

        // Simple tokenization
        String[] tokens = line.split("(?<=\\s)|(?=\\s)|(?=[{}();,])|(?<=[{}();,])");

        for (String token : tokens) {
            if (token.isEmpty()) continue;

            Label tokenLabel = new Label(token);
            tokenLabel.setFont(Font.font("JetBrains Mono", 13));


            // Apply colors
            if (KEYWORD_COLORS.containsKey(token)) {
                tokenLabel.setTextFill(KEYWORD_COLORS.get(token));
            } else if (TYPE_COLORS.containsKey(token)) {
                tokenLabel.setTextFill(TYPE_COLORS.get(token));
            } else if (token.startsWith("\"") || token.startsWith("'")) {
                tokenLabel.setTextFill(Color.web("#a5d6"));
            } else if (token.matches("\\d+")) {
                tokenLabel.setTextFill(Color.web("#79c0ff"));
            } else if (token.startsWith("//")) {
                tokenLabel.setTextFill(Color.web("#8b949e"));
            } else {
                tokenLabel.setTextFill(Color.web("#e6edf3"));
            }

            lineBox.getChildren().add(tokenLabel);
        }

        return lineBox;
    }
}
