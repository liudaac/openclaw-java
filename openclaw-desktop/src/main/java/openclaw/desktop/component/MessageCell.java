package openclaw.desktop.component;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import openclaw.desktop.model.UIMessage;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignA;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;
import org.kordamp.ikonli.materialdesign2.MaterialDesignR;
import org.kordamp.ikonli.materialdesign2.MaterialDesignT;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Custom ListCell for displaying chat messages with Markdown support.
 */
public class MessageCell extends ListCell<UIMessage> {

    private final HBox container;
    private final VBox bubble;
    private final VBox contentContainer;
    private final Label headerLabel;
    private final Label footerLabel;
    private final HBox statusBox;
    private final FontIcon statusIcon;

    private static final Pattern CODE_BLOCK_PATTERN = Pattern.compile("```(\\w*)\\n?(.*?)\\n?```", Pattern.DOTALL);

    public MessageCell() {
        container = new HBox();
        container.setPadding(new Insets(8, 16, 8, 16));

        bubble = new VBox(4);
        bubble.setPadding(new Insets(12, 16, 12, 16));
        bubble.getStyleClass().add("message-bubble");

        headerLabel = new Label();
        headerLabel.getStyleClass().add("message-header");

        contentContainer = new VBox(4);
        contentContainer.getStyleClass().add("message-content");

        statusIcon = new FontIcon();
        statusIcon.setIconSize(14);

        statusBox = new HBox(4, statusIcon);
        statusBox.setAlignment(Pos.CENTER_RIGHT);
        statusBox.getStyleClass().add("message-status");

        footerLabel = new Label();
        footerLabel.getStyleClass().add("message-footer");

        bubble.getChildren().addAll(headerLabel, contentContainer, footerLabel, statusBox);
        container.getChildren().add(bubble);
    }

    @Override
    protected void updateItem(UIMessage message, boolean empty) {
        super.updateItem(message, empty);

        if (empty || message == null) {
            setGraphic(null);
            return;
        }

        headerLabel.setText(getRoleDisplayText(message) + "  ·  " + message.getFormattedTimestamp());
        renderContent(message.getContent());

        String footer = message.isAssistant() && message.getTokenCount() > 0 
            ? message.getTokenCount() + " tokens" : "";
        footerLabel.setText(footer);
        footerLabel.setVisible(!footer.isEmpty());

        updateStatus(message);
        applyRoleStyle(message);
        positionBubble(message);

        setGraphic(container);
    }

    private void renderContent(String content) {
        contentContainer.getChildren().clear();
        if (content == null || content.isEmpty()) return;

        String[] parts = content.split("(?=```)");
        for (String part : parts) {
            if (part.startsWith("```")) {
                Matcher m = CODE_BLOCK_PATTERN.matcher(part);
                if (m.find()) {
                    contentContainer.getChildren().add(new CodeBlockView(m.group(1), m.group(2)));
                }
            } else {
                renderTextBlock(part);
            }
        }
    }

    private void renderTextBlock(String text) {
        for (String para : text.split("\\n\\n")) {
            if (para.trim().isEmpty()) continue;
            
            if (para.startsWith("# ")) {
                Label h = new Label(para.substring(2)); h.getStyleClass().add("markdown-h1");
                contentContainer.getChildren().add(h);
            } else if (para.startsWith("## ")) {
                Label h = new Label(para.substring(3)); h.getStyleClass().add("markdown-h2");
                contentContainer.getChildren().add(h);
            } else if (para.startsWith("### ")) {
                Label h = new Label(para.substring(4)); h.getStyleClass().add("markdown-h3");
                contentContainer.getChildren().add(h);
            } else if (para.startsWith("- ") || para.startsWith("* ")) {
                HBox item = new HBox(8, new Label("•") {{ getStyleClass().add("markdown-bullet"); }}, 
                    new TextFlow(new Text(para.substring(2))));
                item.getStyleClass().add("markdown-list-item");
                contentContainer.getChildren().add(item);
            } else {
                TextFlow flow = new TextFlow(new Text(para));
                flow.getStyleClass().add("markdown-paragraph");
                contentContainer.getChildren().add(flow);
            }
        }
    }

    private String getRoleDisplayText(UIMessage m) {
        return m.isUser() ? "You" : m.isAssistant() ? (m.getModel() != null ? m.getModel() : "Assistant")
            : m.isSystem() ? "System" : m.isTool() ? "Tool" : m.getRole();
    }

    private void updateStatus(UIMessage m) {
        statusBox.setVisible(true);
        switch (m.getStatus()) {
            case PENDING -> { statusIcon.setIconCode(MaterialDesignC.CLOCK_OUTLINE); }
            case SENDING -> { statusIcon.setIconCode(MaterialDesignT.TELEGRAM); }
            case STREAMING -> { statusIcon.setIconCode(MaterialDesignR.RADIO_TOWER); }
            case COMPLETED -> { 
                if (m.isUser()) { statusBox.setVisible(false); return; }
                statusIcon.setIconCode(MaterialDesignC.CHECK_CIRCLE); 
            }
            case FAILED -> { statusIcon.setIconCode(MaterialDesignA.ALERT_CIRCLE); }
            case ABORTED -> { statusIcon.setIconCode(MaterialDesignC.CLOSE_CIRCLE); }
        }
    }

    private void applyRoleStyle(UIMessage m) {
        bubble.getStyleClass().removeAll("user-bubble", "assistant-bubble", "system-bubble", "tool-bubble");
        if (m.isUser()) bubble.getStyleClass().add("user-bubble");
        else if (m.isAssistant()) bubble.getStyleClass().add("assistant-bubble");
        else if (m.isSystem()) bubble.getStyleClass().add("system-bubble");
        else if (m.isTool()) bubble.getStyleClass().add("tool-bubble");
    }

    private void positionBubble(UIMessage m) {
        container.getChildren().clear();
        container.setAlignment(m.isUser() ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        container.getChildren().add(bubble);
    }
}
