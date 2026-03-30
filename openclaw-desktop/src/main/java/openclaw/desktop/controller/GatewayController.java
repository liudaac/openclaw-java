package openclaw.desktop.controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import openclaw.desktop.service.GatewayService;
import openclaw.gateway.GatewayStatus;
import openclaw.gateway.NodeInfo;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;
import org.kordamp.ikonli.materialdesign2.MaterialDesignD;
import org.kordamp.ikonli.materialdesign2.MaterialDesignL;
import org.kordamp.ikonli.materialdesign2.MaterialDesignN;
import org.kordamp.ikonli.materialdesign2.MaterialDesignP;
import org.kordamp.ikonli.materialdesign2.MaterialDesignR;
import org.kordamp.ikonli.materialdesign2.MaterialDesignS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

/**
 * Gateway Controller for managing Gateway connection and nodes.
 *
 * <p>Provides UI for Gateway status, node management, and device pairing.</p>
 */
@Controller
public class GatewayController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(GatewayController.class);

    @Autowired
    private GatewayService gatewayService;

    @FXML
    private VBox gatewayContainer;

    @FXML
    private Label statusLabel;

    @FXML
    private Label urlLabel;

    @FXML
    private Label versionLabel;

    @FXML
    private Button connectButton;

    @FXML
    private Button disconnectButton;

    @FXML
    private Button refreshButton;

    @FXML
    private ListView<NodeInfo> nodeList;

    @FXML
    private TextField gatewayUrlField;

    @FXML
    private TextField authTokenField;

    @FXML
    private CheckBox autoReconnectCheck;

    @FXML
    private Label lastConnectedLabel;

    @FXML
    private Label reconnectAttemptsLabel;

    private ObservableList<NodeInfo> nodes;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("Initializing GatewayController...");

        setupButtons();
        setupNodeList();
        loadGatewayStatus();

        logger.info("GatewayController initialized");
    }

    private void setupButtons() {
        // Connect button
        FontIcon connectIcon = new FontIcon(MaterialDesignP.PLUG);
        connectIcon.setIconSize(18);
        connectButton.setGraphic(connectIcon);
        connectButton.setOnAction(e -> connectGateway());

        // Disconnect button
        FontIcon disconnectIcon = new FontIcon(MaterialDesignP.PLUG_OFF);
        disconnectIcon.setIconSize(18);
        disconnectButton.setGraphic(disconnectIcon);
        disconnectButton.setOnAction(e -> disconnectGateway());

        // Refresh button
        FontIcon refreshIcon = new FontIcon(MaterialDesignR.REFRESH);
        refreshIcon.setIconSize(18);
        refreshButton.setGraphic(refreshIcon);
        refreshButton.setOnAction(e -> refreshStatus());
    }

    private void setupNodeList() {
        nodes = FXCollections.observableArrayList();
        nodeList.setItems(nodes);
        nodeList.setCellFactory(param -> new NodeCell());
    }

    private void loadGatewayStatus() {
        gatewayService.getStatus()
            .thenAccept(status -> Platform.runLater(() -> updateStatusUI(status)))
            .exceptionally(ex -> {
                logger.error("Failed to load gateway status", ex);
                return null;
            });
    }

    private void updateStatusUI(GatewayStatus status) {
        statusLabel.setText(status.isConnected() ? "Connected" : "Disconnected");
        statusLabel.getStyleClass().setAll(status.isConnected() ? "status-connected" : "status-disconnected");

        urlLabel.setText(status.getUrl() != null ? status.getUrl() : "Not configured");
        versionLabel.setText(status.getVersion() != null ? status.getVersion() : "Unknown");

        if (status.getLastConnectedAt() != null) {
            lastConnectedLabel.setText("Last connected: " +
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(status.getLastConnectedAt()));
        } else {
            lastConnectedLabel.setText("Never connected");
        }

        reconnectAttemptsLabel.setText("Reconnect attempts: " + status.getReconnectAttempts());

        // Update buttons
        connectButton.setDisable(status.isConnected());
        disconnectButton.setDisable(!status.isConnected());

        // Load nodes
        if (status.isConnected()) {
            loadNodes();
        }
    }

    private void loadNodes() {
        gatewayService.getNodes()
            .thenAccept(nodeList -> Platform.runLater(() -> {
                nodes.clear();
                nodes.addAll(nodeList);
            }))
            .exceptionally(ex -> {
                logger.error("Failed to load nodes", ex);
                return null;
            });
    }

    @FXML
    private void connectGateway() {
        String url = gatewayUrlField.getText().trim();
        String token = authTokenField.getText().trim();

        if (url.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Configuration Required", "Please enter Gateway URL");
            return;
        }

        connectButton.setDisable(true);
        statusLabel.setText("Connecting...");

        gatewayService.connect(url, token, autoReconnectCheck.isSelected())
            .thenAccept(success -> Platform.runLater(() -> {
                if (success) {
                    showAlert(Alert.AlertType.INFORMATION, "Success", "Connected to Gateway");
                    loadGatewayStatus();
                } else {
                    showAlert(Alert.AlertType.ERROR, "Error", "Failed to connect to Gateway");
                    connectButton.setDisable(false);
                }
            }))
            .exceptionally(ex -> {
                Platform.runLater(() -> {
                    showAlert(Alert.AlertType.ERROR, "Error", "Connection failed: " + ex.getMessage());
                    connectButton.setDisable(false);
                });
                return null;
            });
    }

    @FXML
    private void disconnectGateway() {
        gatewayService.disconnect()
            .thenRun(() -> Platform.runLater(() -> {
                showAlert(Alert.AlertType.INFORMATION, "Disconnected", "Gateway connection closed");
                loadGatewayStatus();
            }));
    }

    @FXML
    private void refreshStatus() {
        loadGatewayStatus();
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    /**
     * Custom cell for node list.
     */
    private class NodeCell extends ListCell<NodeInfo> {
        private final HBox container;
        private final Label nameLabel;
        private final Label statusLabel;
        private final Label versionLabel;

        public NodeCell() {
            container = new HBox(12);
            container.setPadding(new javafx.geometry.Insets(8, 12, 8, 12));
            container.getStyleClass().add("node-item");

            VBox infoBox = new VBox(4);
            nameLabel = new Label();
            nameLabel.getStyleClass().add("node-name");

            HBox metaBox = new HBox(8);
            statusLabel = new Label();
            statusLabel.getStyleClass().add("node-status");
            versionLabel = new Label();
            versionLabel.getStyleClass().add("node-version");
            metaBox.getChildren().addAll(statusLabel, versionLabel);

            infoBox.getChildren().addAll(nameLabel, metaBox);
            container.getChildren().add(infoBox);
        }

        @Override
        protected void updateItem(NodeInfo item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
            } else {
                nameLabel.setText(item.name());
                statusLabel.setText(item.status());
                versionLabel.setText(item.version());
                setGraphic(container);
            }
        }
    }

    /**
     * Node info record.
     */
    private record NodeInfo(String name, String status, String version) {
    }
}
