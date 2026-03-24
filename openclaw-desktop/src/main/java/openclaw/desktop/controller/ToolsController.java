package openclaw.desktop.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import openclaw.desktop.component.ToolCard;
import openclaw.desktop.component.ToolResultView;
import openclaw.desktop.model.ToolExecutionResult;
import openclaw.desktop.service.ToolExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.net.URL;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * Tools Controller for managing tool execution UI.
 */
@Controller
public class ToolsController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(ToolsController.class);

    @Autowired
    private ToolExecutorService toolService;

    @FXML private VBox toolsContainer;
    @FXML private TextField searchField;
    @FXML private FlowPane toolsGrid;
    @FXML private TabPane toolTabs;
    @FXML private ListView<ToolExecutorService.CronJobInfo> cronJobList;
    @FXML private TextArea toolOutput;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("Initializing ToolsController...");
        setupSearch();
        loadTools();
        setupCronTab();
        logger.info("ToolsController initialized");
    }

    private void setupSearch() {
        searchField.setPromptText("Search tools...");
        searchField.textProperty().addListener((obs, oldVal, newVal) -> filterTools(newVal));
    }

    private void loadTools() {
        toolsGrid.getChildren().clear();
        var tools = toolService.getAvailableTools();
        for (var tool : tools) {
            ToolCard card = new ToolCard(tool, this::onToolClick);
            toolsGrid.getChildren().add(card);
        }
    }

    private void filterTools(String keyword) {
        toolsGrid.getChildren().clear();
        var tools = toolService.getAvailableTools();
        for (var tool : tools) {
            if (tool.name().toLowerCase().contains(keyword.toLowerCase()) ||
                tool.description().toLowerCase().contains(keyword.toLowerCase())) {
                ToolCard card = new ToolCard(tool, this::onToolClick);
                toolsGrid.getChildren().add(card);
            }
        }
    }

    private void onToolClick(String toolName) {
        showToolDialog(toolName);
    }

    private void showToolDialog(String toolName) {
        var tool = toolService.getTool(toolName);
        if (tool == null) return;

        Dialog<Map<String, Object>> dialog = new Dialog<>();
        dialog.setTitle("Execute: " + tool.name());
        dialog.setHeaderText(tool.description());

        VBox form = new VBox(10);
        form.setPadding(new javafx.geometry.Insets(20));

        Map<String, TextField> inputFields = new java.util.HashMap<>();
        if (tool.parameters() != null && tool.parameters().properties() != null) {
            for (var entry : tool.parameters().properties().entrySet()) {
                Label label = new Label(entry.getKey() + ":");
                TextField field = new TextField();
                field.setPromptText(entry.getValue().description());
                form.getChildren().addAll(label, field);
                inputFields.put(entry.getKey(), field);
            }
        }

        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(button -> {
            if (button == ButtonType.OK) {
                Map<String, Object> result = new java.util.HashMap<>();
                for (var entry : inputFields.entrySet()) {
                    if (!entry.getValue().getText().isEmpty()) {
                        result.put(entry.getKey(), entry.getValue().getText());
                    }
                }
                return result;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(args -> executeTool(toolName, args));
    }

    private void executeTool(String toolName, Map<String, Object> args) {
        resultContainer.getChildren().clear();
        
        Label loadingLabel = new Label("Executing " + toolName + "...");
        loadingLabel.getStyleClass().add("tool-loading");
        resultContainer.getChildren().add(loadingLabel);

        toolService.executeTool(toolName, args)
            .thenAccept(result -> Platform.runLater(() -> {
                resultContainer.getChildren().clear();
                ToolResultView resultView = new ToolResultView(new ToolExecutionResult(
                    result.success(),
                    result.output(),
                    result.error(),
                    result.metadata()
                ));
                resultContainer.getChildren().add(resultView);
            }))
            .exceptionally(ex -> {
                Platform.runLater(() -> {
                    resultContainer.getChildren().clear();
                    ToolResultView errorView = new ToolResultView(new ToolExecutionResult(
                        false,
                        null,
                        ex.getMessage(),
                        Map.of()
                    ));
                    resultContainer.getChildren().add(errorView);
                });
                return null;
            });
    }

    private void setupCronTab() {
        cronJobList.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(ToolExecutorService.CronJobInfo item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.name() + " (" + item.schedule() + ")");
                }
            }
        });
        refreshCronJobs();
    }

    @FXML
    private void refreshCronJobs() {
        toolService.listCronJobs()
            .thenAccept(jobs -> Platform.runLater(() -> {
                cronJobList.getItems().clear();
                cronJobList.getItems().addAll(jobs);
            }));
    }

    @FXML
    private void createCronJob() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Create Cron Job");

        VBox form = new VBox(10);
        form.setPadding(new javafx.geometry.Insets(20));

        TextField nameField = new TextField(); nameField.setPromptText("Job name");
        TextField scheduleField = new TextField(); scheduleField.setPromptText("Cron expression");
        TextField commandField = new TextField(); commandField.setPromptText("Command");

        form.getChildren().addAll(
            new Label("Name:"), nameField,
            new Label("Schedule:"), scheduleField,
            new Label("Command:"), commandField
        );

        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(result -> {
            toolService.createCronJob(nameField.getText(), scheduleField.getText(), commandField.getText(), null)
                .thenRun(this::refreshCronJobs);
        });
    }
}
