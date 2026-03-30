package openclaw.desktop.component;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import openclaw.desktop.model.ToolExecutionResult;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;
import org.kordamp.ikonli.materialdesign2.MaterialDesignE;

import java.io.ByteArrayInputStream;
import java.util.Map;

/**
 * Tool Result View Component.
 *
 * <p>Displays tool execution results with multiple output formats.</p>
 */
public class ToolResultView extends VBox {

    private final ToolExecutionResult result;

    public ToolResultView(ToolExecutionResult result) {
        this.result = result;
        initialize();
    }

    private void initialize() {
        getStyleClass().add("tool-result-view");
        setSpacing(12);
        setPadding(new Insets(16));

        // Header
        HBox header = new HBox(12);
        header.getStyleClass().add("tool-result-header");

        FontIcon statusIcon = new FontIcon(result.success() ? MaterialDesignC.CHECK_CIRCLE : MaterialDesignC.CLOSE_CIRCLE);
        statusIcon.setIconSize(24);
        statusIcon.getStyleClass().add(result.success() ? "status-success" : "status-error");

        Label statusLabel = new Label(result.success() ? "Success" : "Failed");
        statusLabel.getStyleClass().add("tool-result-status");

        header.getChildren().addAll(statusIcon, statusLabel);

        // Content tabs
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        VBox.setVgrow(tabPane, Priority.ALWAYS);

        // Output tab
        if (result.output() != null && !result.output().isEmpty()) {
            Tab outputTab = new Tab("Output", createOutputView(result.output()));
            tabPane.getTabs().add(outputTab);
        }

        // Error tab
        if (result.error() != null && !result.error().isEmpty()) {
            Tab errorTab = new Tab("Error", createErrorView(result.error()));
            tabPane.getTabs().add(errorTab);
        }

        // Metadata tab
        if (result.metadata() != null && !result.metadata().isEmpty()) {
            Tab metaTab = new Tab("Metadata", createMetadataView(result.metadata()));
            tabPane.getTabs().add(metaTab);
        }

        getChildren().addAll(header, tabPane);
    }

    private ScrollPane createOutputView(String output) {
        TextFlow flow = new TextFlow(new Text(output));
        flow.getStyleClass().add("tool-output-text");

        ScrollPane scroll = new ScrollPane(flow);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("tool-output-scroll");

        return scroll;
    }

    private ScrollPane createErrorView(String error) {
        TextFlow flow = new TextFlow(new Text(error));
        flow.getStyleClass().add("tool-error-text");

        ScrollPane scroll = new ScrollPane(flow);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("tool-error-scroll");

        return scroll;
    }

    private ScrollPane createMetadataView(Map<String, Object> metadata) {
        VBox metaBox = new VBox(8);
        metaBox.getStyleClass().add("tool-metadata-box");

        for (Map.Entry<String, Object> entry : metadata.entrySet()) {
            HBox row = new HBox(8);
            row.getStyleClass().add("tool-metadata-row");

            Label keyLabel = new Label(entry.getKey() + ":");
            keyLabel.getStyleClass().add("tool-metadata-key");

            Label valueLabel = new Label(String.valueOf(entry.getValue()));
            valueLabel.getStyleClass().add("tool-metadata-value");

            row.getChildren().addAll(keyLabel, valueLabel);
            metaBox.getChildren().add(row);
        }

        ScrollPane scroll = new ScrollPane(metaBox);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("tool-metadata-scroll");

        return scroll;
    }

    /**
     * Create image result view.
     */
    public static ToolResultView createImageResult(byte[] imageData) {
        ToolExecutionResult result = new ToolExecutionResult(
            true,
            "Image captured successfully",
            null,
            Map.of("size", imageData.length)
        );

        ToolResultView view = new ToolResultView(result);

        // Add image tab
        Image image = new Image(new ByteArrayInputStream(imageData));
        ImageView imageView = new ImageView(image);
        imageView.setPreserveRatio(true);
        imageView.setFitWidth(600);

        ScrollPane scroll = new ScrollPane(imageView);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);

        TabPane tabPane = (TabPane) view.getChildren().get(1);
        Tab imageTab = new Tab("Image", scroll);
        tabPane.getTabs().add(0, imageTab);

        return view;
    }
}
