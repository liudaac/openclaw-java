package openclaw.desktop.controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import openclaw.desktop.model.AgentInfo;
import openclaw.desktop.service.AgentService;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignD;
import org.kordamp.ikonli.materialdesign2.MaterialDesignP;
import org.kordamp.ikonli.materialdesign2.MaterialDesignR;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Agent Controller for managing AI agents.
 */
@Controller
public class AgentController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(AgentController.class);

    @Autowired
    private AgentService agentService;

    @FXML
    private VBox agentContainer;

    @FXML
    private ListView<AgentInfo> agentList;

    @FXML
    private TextField searchField;

    @FXML
    private Label selectedAgentLabel;

    @FXML
    private TextArea agentDescription;

    @FXML
    private ListView<String> skillsList;

    @FXML
    private Button createButton;

    @FXML
    private Button editButton;

    @FXML
    private Button deleteButton;

    @FXML
    private Button refreshButton;

    private ObservableList<AgentInfo> agents;
    private AgentInfo selectedAgent;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("Initializing AgentController...");

        setupAgentList();
        setupButtons();
        setupSearch();
        loadAgents();

        logger.info("AgentController initialized");
    }

    private void setupAgentList() {
        agents = FXCollections.observableArrayList();
        agentList.setItems(agents);
        agentList.setCellFactory(param -> new AgentCell());

        agentList.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldVal, newVal) -> {
                selectedAgent = newVal;
                updateAgentDetails();
            }
        );
    }

    private void setupButtons() {
        FontIcon createIcon = new FontIcon(MaterialDesignP.PLUS);
        createIcon.setIconSize(18);
        createButton.setGraphic(createIcon);
        createButton.setOnAction(e -> createAgent());

        FontIcon editIcon = new FontIcon(MaterialDesignP.PENCIL);
        editIcon.setIconSize(18);
        editButton.setGraphic(editIcon);
        editButton.setOnAction(e -> editAgent());

        FontIcon deleteIcon = new FontIcon(MaterialDesignD.DELETE);
        deleteIcon.setIconSize(18);
        deleteButton.setGraphic(deleteIcon);
        deleteButton.setOnAction(e -> deleteAgent());

        FontIcon refreshIcon = new FontIcon(MaterialDesignR.REFRESH);
        refreshIcon.setIconSize(18);
        refreshButton.setGraphic(refreshIcon);
        refreshButton.setOnAction(e -> loadAgents());
    }

    private void setupSearch() {
        searchField.setPromptText("Search agents...");
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            filterAgents(newVal);
        });
    }

    private void loadAgents() {
        agentService.getAgents()
            .thenAccept(list -> Platform.runLater(() -> {
                agents.clear();
                agents.addAll(list);
            }))
            .exceptionally(ex -> {
                logger.error("Failed to load agents", ex);
                return null;
            });
    }

    private void filterAgents(String keyword) {
        if (keyword.isEmpty()) {
            loadAgents();
            return;
        }

        agentService.searchAgents(keyword)
            .thenAccept(list -> Platform.runLater(() -> {
                agents.clear();
                agents.addAll(list);
            }));
    }

    private void updateAgentDetails() {
        if (selectedAgent == null) {
            selectedAgentLabel.setText("No agent selected");
            agentDescription.clear();
            skillsList.getItems().clear();
            editButton.setDisable(true);
            deleteButton.setDisable(true);
            return;
        }

        selectedAgentLabel.setText(selectedAgent.name());
        agentDescription.setText(selectedAgent.description());
        skillsList.getItems().setAll(selectedAgent.skills());

        editButton.setDisable(false);
        deleteButton.setDisable(false);
    }

    @FXML
    private void createAgent() {
        Dialog<AgentInfo> dialog = new Dialog<>();
        dialog.setTitle("Create Agent");

        VBox form = new VBox(10);
        form.setPadding(new javafx.geometry.Insets(20));

        TextField nameField = new TextField();
        nameField.setPromptText("Agent name");

        TextArea descField = new TextArea();
        descField.setPromptText("Description");
        descField.setPrefRowCount(3);

        form.getChildren().addAll(
            new Label("Name:"), nameField,
            new Label("Description:"), descField
        );

        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(button -> {
            if (button == ButtonType.OK) {
                return new AgentInfo(
                    null,
                    nameField.getText(),
                    descField.getText(),
                    java.util.List.of(),
                    "active"
                );
            }
            return null;
        });

        dialog.showAndWait().ifPresent(agent -> {
            agentService.createAgent(agent)
                .thenRun(this::loadAgents);
        });
    }

    @FXML
    private void editAgent() {
        if (selectedAgent == null) return;

        Dialog<AgentInfo> dialog = new Dialog<>();
        dialog.setTitle("Edit Agent");

        VBox form = new VBox(10);
        form.setPadding(new javafx.geometry.Insets(20));

        TextField nameField = new TextField(selectedAgent.name());
        TextArea descField = new TextArea(selectedAgent.description());
        descField.setPrefRowCount(3);

        form.getChildren().addAll(
            new Label("Name:"), nameField,
            new Label("Description:"), descField
        );

        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(button -> {
            if (button == ButtonType.OK) {
                return new AgentInfo(
                    selectedAgent.id(),
                    nameField.getText(),
                    descField.getText(),
                    selectedAgent.skills(),
                    selectedAgent.status()
                );
            }
            return null;
        });

        dialog.showAndWait().ifPresent(agent -> {
            agentService.updateAgent(agent)
                .thenRun(this::loadAgents);
        });
    }

    @FXML
    private void deleteAgent() {
        if (selectedAgent == null) return;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Agent");
        alert.setHeaderText("Delete \"" + selectedAgent.name() + "\"?");
        alert.setContentText("This action cannot be undone.");

        alert.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                agentService.deleteAgent(selectedAgent.id())
                    .thenRun(this::loadAgents);
            }
        });
    }

    /**
     * Custom cell for agent list.
     */
    private class AgentCell extends ListCell<AgentInfo> {
        private final HBox container;
        private final Label nameLabel;
        private final Label statusLabel;

        public AgentCell() {
            container = new HBox(12);
            container.setPadding(new javafx.geometry.Insets(10, 12, 10, 12));
            container.getStyleClass().add("agent-item");

            VBox infoBox = new VBox(4);
            nameLabel = new Label();
            nameLabel.getStyleClass().add("agent-name");

            statusLabel = new Label();
            statusLabel.getStyleClass().add("agent-status");

            infoBox.getChildren().addAll(nameLabel, statusLabel);
            container.getChildren().add(infoBox);
        }

        @Override
        protected void updateItem(AgentInfo agent, boolean empty) {
            super.updateItem(agent, empty);

            if (empty || agent == null) {
                setGraphic(null);
                return;
            }

            nameLabel.setText(agent.name());
            statusLabel.setText(agent.status());
            statusLabel.getStyleClass().setAll("agent-status", agent.isActive() ? "status-active" : "status-paused");

            setGraphic(container);
        }
    }
}
