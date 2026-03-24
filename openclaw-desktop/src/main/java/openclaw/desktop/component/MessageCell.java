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

/**
 * Custom ListCell for displaying chat messages.
 *
 * <p>Modern bubble-style message display with role-based styling.</p>
 */
public class MessageCell extends ListCell<UIMessage> {

    private final HBox container;
    private final VBox bubble;
    private final TextFlow contentFlow;
    private final Text contentText;
    private final Label headerLabel;
    private final Label footerLabel;
    private final HBox statusBox;
    private final FontIcon statusIcon;

    public MessageCell() {
        // Container
        container = new HBox();
        container.setPadding(new Insets(8, 16, 8, 16));

        // Bubble
        bubble = new VBox(4);
        bubble.setPadding(new Insets(12, 16, 12, 16));
        bubble.getStyleClass().add("message-bubble");

        // Header (role + timestamp)
        headerLabel = new Label();
        headerLabel.getStyleClass().add("message-header");

        // Content
        contentText = new Text();
        contentText.getStyleClass().add("message-content");
        contentFlow = new TextFlow(contentText);
        contentFlow.getStyleClass().add("message-content-flow");

        // Status indicator
        statusIcon = new FontIcon();
        statusIcon.setIconSize(14);
        
        statusBox = new HBox(4, statusIcon);
        statusBox.setAlignment(Pos.CENTER_RIGHT);
        statusBox.getStyleClass().add("message-status");

        // Footer (model + tokens)
        footerLabel = new Label();
        footerLabel.getStyleClass().add("message-footer");

        // Assemble bubble
        bubble.getChildren().addAll(headerLabel, contentFlow, footerLabel, statusBox);
        container.getChildren().add(bubble);

        // Bind width for text wrapping
        contentFlow.maxWidthProperty().bind(widthProperty().subtract(120));
    }

    @Override
    protected void updateItem(UIMessage message, boolean empty) {
        super.updateItem(message, empty);

        if (empty || message == null) {
            setGraphic(null);
            setText(null);
            return;
        }

        // Update content
        contentText.setText(message.getContent());

        // Update header based on role
        String roleText = getRoleDisplayText(message);
        String timestamp = message.getFormattedTimestamp();
        headerLabel.setText(roleText + "  ·  " + timestamp);

        // Update footer
        String footer = buildFooter(message);
        footerLabel.setText(footer);
        footerLabel.setVisible(!footer.isEmpty());

        // Update status
        updateStatus(message);

        // Apply role-based styling
        applyRoleStyle(message);

        // Position bubble based on role
        positionBubble(message);

        setGraphic(container);
    }

    private String getRoleDisplayText(UIMessage message) {
        if (message.isUser()) {
            return "You";
        } else if (message.isAssistant()) {
            return message.getModel() != null && !message.getModel().isEmpty() 
                ? message.getModel() 
                : "Assistant";
        } else if (message.isSystem()) {
            return "System";
        } else if (message.isTool()) {
            return "Tool";
        }
        return message.getRole();
    }

    private String buildFooter(UIMessage message) {
        if (!message.isAssistant()) {
            return "";
        }

        StringBuilder footer = new StringBuilder();
        if (message.getTokenCount() > 0) {
            footer.append(message.getTokenCount()).append(" tokens");
        }
        return footer.toString();
    }

    private void updateStatus(UIMessage message) {
        statusBox.setVisible(true);

        switch (message.getStatus()) {
            case PENDING -> {
                statusIcon.setIconCode(MaterialDesignC.CLOCK_OUTLINE);
                statusBox.getStyleClass().setAll("message-status", "status-pending");
            }
            case SENDING -> {
                statusIcon.setIconCode(MaterialDesignT.TELEGRAM);
                statusBox.getStyleClass().setAll("message-status", "status-sending");
            }
            case STREAMING -> {
                statusIcon.setIconCode(MaterialDesignR.RADIO_TOWER);
                statusBox.getStyleClass().setAll("message-status", "status-streaming");
            }
            case COMPLETED -> {
                if (message.isUser()) {
                    statusBox.setVisible(false);
                } else {
                    statusIcon.setIconCode(MaterialDesignC.CHECK_CIRCLE);
                    statusBox.getStyleClass().setAll("message-status", "status-completed");
                }
            }
            case FAILED -> {
                statusIcon.setIconCode(MaterialDesignA.ALERT_CIRCLE);
                statusBox.getStyleClass().setAll("message-status", "status-failed");
            }
            case ABORTED -> {
                statusIcon.setIconCode(MaterialDesignC.CLOSE_CIRCLE);
                statusBox.getStyleClass().setAll("message-status", "status-aborted");
            }
        }
    }

    private void applyRoleStyle(UIMessage message) {
        bubble.getStyleClass().removeAll(
            "user-bubble", "assistant-bubble", "system-bubble", "tool-bubble"
        );

        if (message.isUser()) {
            bubble.getStyleClass().add("user-bubble");
        } else if (message.isAssistant()) {
            bubble.getStyleClass().add("assistant-bubble");
        } else if (message.isSystem()) {
            bubble.getStyleClass().add("system-bubble");
        } else if (message.isTool()) {
            bubble.getStyleClass().add("tool-bubble");
        }
    }

    private void positionBubble(UIMessage message) {
        container.getChildren().clear();

        if (message.isUser()) {
            // User messages on right
            container.setAlignment(Pos.CENTER_RIGHT);
            container.getChildren().add(bubble);
        } else if (message.isAssistant()) {
            // Assistant messages on left
            container.setAlignment(Pos.CENTER_LEFT);
            container.getChildren().add(bubble);
        } else {
            // System/tool messages centered
            container.setAlignment(Pos.CENTER);
            container.getChildren().add(bubble);
        }
    }
}
