package openclaw.desktop.component;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import openclaw.desktop.service.ToolExecutorService;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignA;
import org.kordamp.ikonli.materialdesign2.MaterialDesignB;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;
import org.kordamp.ikonli.materialdesign2.MaterialDesignE;
import org.kordamp.ikonli.materialdesign2.MaterialDesignF;
import org.kordamp.ikonli.materialdesign2.MaterialDesignG;
import org.kordamp.ikonli.materialdesign2.MaterialDesignM;
import org.kordamp.ikonli.materialdesign2.MaterialDesignS;
import org.kordamp.ikonli.materialdesign2.MaterialDesignT;
import org.kordamp.ikonli.materialdesign2.MaterialDesignW;

import java.util.function.Consumer;

/**
 * Tool Card Component for displaying available tools.
 *
 * <p>Modern card design with hover effects and icon support.</p>
 */
public class ToolCard extends VBox {

    private final String toolName;
    private final String description;
    private final Consumer<String> onClick;

    public ToolCard(ToolExecutorService.ToolInfo toolInfo, Consumer<String> onClick) {
        this(toolInfo.name(), toolInfo.description(), onClick);
    }

    public ToolCard(String toolName, String description, Consumer<String> onClick) {
        this.toolName = toolName;
        this.description = description;
        this.onClick = onClick;

        initialize();
    }

    private void initialize() {
        // Card styling
        getStyleClass().add("tool-card");
        setPadding(new Insets(20));
        setSpacing(12);
        setAlignment(Pos.TOP_LEFT);
        setCursor(Cursor.HAND);
        setPrefWidth(280);
        setPrefHeight(160);

        // Icon
        FontIcon icon = new FontIcon(getIconForTool(toolName));
        icon.setIconSize(32);
        icon.getStyleClass().add("tool-card-icon");

        // Name
        Label nameLabel = new Label(capitalizeFirst(toolName));
        nameLabel.getStyleClass().add("tool-card-name");

        // Description
        Label descLabel = new Label(description);
        descLabel.getStyleClass().add("tool-card-description");
        descLabel.setWrapText(true);

        // Get children
        getChildren().addAll(icon, nameLabel, descLabel);

        // Hover effects
        setupHoverEffects();

        // Click handler
        setOnMouseClicked(e -> {
            if (onClick != null) {
                onClick.accept(toolName);
            }
        });
    }

    private void setupHoverEffects() {
        // Scale animation
        ScaleTransition scaleUp = new ScaleTransition(Duration.millis(200), this);
        scaleUp.setToX(1.02);
        scaleUp.setToY(1.02);

        ScaleTransition scaleDown = new ScaleTransition(Duration.millis(200), this);
        scaleDown.setToX(1.0);
        scaleDown.setToY(1.0);

        // Fade animation for shadow
        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), this);
        fadeIn.setToValue(1.0);

        FadeTransition fadeOut = new FadeTransition(Duration.millis(200), this);
        fadeOut.setToValue(0.9);

        setOnMouseEntered(e -> {
            scaleUp.play();
            getStyleClass().add("tool-card-hover");
        });

        setOnMouseExited(e -> {
            scaleDown.play();
            getStyleClass().remove("tool-card-hover");
        });
    }

    private String getIconForTool(String toolName) {
        String lower = toolName.toLowerCase();
        
        return switch (lower) {
            case "cron" -> MaterialDesignA.ALARM;
            case "browser" -> MaterialDesignG.GOOGLE_CHROME;
            case "session" -> MaterialDesignC.CHAT;
            case "web_search", "websearch" -> MaterialDesignS.SEARCH_WEB;
            case "file" -> MaterialDesignF.FOLDER;
            case "email" -> MaterialDesignE.EMAIL;
            case "exec", "shell", "command" -> MaterialDesignT.TERMINAL;
            case "python" -> MaterialDesignL.LANGUAGE_PYTHON; // Note: Need to import MaterialDesignL
            case "image" -> MaterialDesignI.IMAGE; // Note: Need to import MaterialDesignI
            case "calendar" -> MaterialDesignC.CALENDAR;
            case "weather" -> MaterialDesignW.WEATHER_PARTLY_CLOUDY;
            case "translate" -> MaterialDesignT.TRANSLATE;
            case "finance" -> MaterialDesignC.CASH;
            case "audit" -> MaterialDesignS.SHIELD_CHECK;
            case "fetch" -> MaterialDesignD.DOWNLOAD; // Note: Need to import MaterialDesignD
            case "db", "database" -> MaterialDesignD.DATABASE;
            default -> MaterialDesignM.MENU;
        };
    }

    private String capitalizeFirst(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    public String getToolName() {
        return toolName;
    }
}
