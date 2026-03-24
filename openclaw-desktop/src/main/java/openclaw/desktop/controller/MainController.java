package openclaw.desktop.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import openclaw.desktop.config.DesktopConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * Main Controller for OpenClaw Desktop.
 *
 * <p>Manages the main window layout and coordinates between child controllers.</p>
 */
@Controller
public class MainController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(MainController.class);

    @Autowired
    private DesktopConfig config;

    @FXML
    private BorderPane rootPane;

    @FXML
    private SplitPane splitPane;

    @FXML
    private StackPane sidebarContainer;

    @FXML
    private StackPane contentContainer;

    private Stage stage;
    private SidebarController sidebarController;
    private ChatController chatController;
    private GatewayController gatewayController;
    private ToolsController toolsController;
    private SettingsController settingsController;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("Initializing MainController...");

        // Setup split pane
        splitPane.setDividerPositions(0.22);

        // Load child views
        loadSidebar();
        loadChatView();

        logger.info("MainController initialized");
    }

    /**
     * Set the primary stage.
     */
    public void setStage(Stage stage) {
        this.stage = stage;
        setupStage();
    }

    /**
     * Setup stage properties.
     */
    private void setupStage() {
        if (stage == null) return;

        // Set minimum size
        stage.setMinWidth(1000);
        stage.setMinHeight(700);

        // Restore window size from config
        stage.setWidth(config.getWindowWidth());
        stage.setHeight(config.getWindowHeight());

        // Save window size on close
        stage.setOnCloseRequest(e -> {
            config.setWindowWidth((int) stage.getWidth());
            config.setWindowHeight((int) stage.getHeight());
        });
    }

    /**
     * Load sidebar view.
     */
    private void loadSidebar() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/sidebar.fxml"));
            Parent sidebar = loader.load();
            sidebarController = loader.getController();
            sidebarController.setMainController(this);

            sidebarContainer.getChildren().add(sidebar);
            logger.debug("Sidebar loaded");
        } catch (IOException e) {
            logger.error("Failed to load sidebar", e);
        }
    }

    /**
     * Load chat view.
     */
    private void loadChatView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/chat.fxml"));
            Parent chatView = loader.load();
            chatController = loader.getController();

            contentContainer.getChildren().clear();
            contentContainer.getChildren().add(chatView);
            logger.debug("Chat view loaded");
        } catch (IOException e) {
            logger.error("Failed to load chat view", e);
        }
    }

    /**
     * Load Gateway view.
     */
    @FXML
    public void showGatewayView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/gateway.fxml"));
            Parent gatewayView = loader.load();
            gatewayController = loader.getController();

            contentContainer.getChildren().clear();
            contentContainer.getChildren().add(gatewayView);
            logger.debug("Gateway view loaded");
        } catch (IOException e) {
            logger.error("Failed to load gateway view", e);
        }

        if (sidebarController != null) {
            sidebarController.setActiveTab("gateway");
        }
    }

    /**
     * Load tools view.
     */
    @FXML
    public void showToolsView() {
        if (toolsController == null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/tools.fxml"));
                Parent toolsView = loader.load();
                toolsController = loader.getController();

                contentContainer.getChildren().clear();
                contentContainer.getChildren().add(toolsView);
                logger.debug("Tools view loaded");
            } catch (IOException e) {
                logger.error("Failed to load tools view", e);
            }
        } else {
            // Switch to existing view
            contentContainer.getChildren().clear();
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/tools.fxml"));
                Parent toolsView = loader.load();
                contentContainer.getChildren().add(toolsView);
            } catch (IOException e) {
                logger.error("Failed to reload tools view", e);
            }
        }

        if (sidebarController != null) {
            sidebarController.setActiveTab("tools");
        }
    }

    /**
     * Load settings view.
     */
    @FXML
    public void showSettingsView() {
        if (settingsController == null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/settings.fxml"));
                Parent settingsView = loader.load();
                settingsController = loader.getController();

                contentContainer.getChildren().clear();
                contentContainer.getChildren().add(settingsView);
                logger.debug("Settings view loaded");
            } catch (IOException e) {
                logger.error("Failed to load settings view", e);
            }
        } else {
            contentContainer.getChildren().clear();
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/settings.fxml"));
                Parent settingsView = loader.load();
                contentContainer.getChildren().add(settingsView);
            } catch (IOException e) {
                logger.error("Failed to reload settings view", e);
            }
        }

        if (sidebarController != null) {
            sidebarController.setActiveTab("settings");
        }
    }

    /**
     * Show chat view.
     */
    @FXML
    public void showChatView() {
        loadChatView();
        if (sidebarController != null) {
            sidebarController.setActiveTab("chat");
        }
    }

    /**
     * Create a new conversation.
     */
    public void createNewConversation() {
        if (chatController != null) {
            chatController.createNewConversation();
        }
        showChatView();
    }

    /**
     * Switch to conversation.
     */
    public void switchToConversation(String sessionKey) {
        if (chatController != null) {
            chatController.loadConversation(sessionKey);
        }
        showChatView();
    }

    /**
     * Get the sidebar controller.
     */
    public SidebarController getSidebarController() {
        return sidebarController;
    }

    /**
     * Get the chat controller.
     */
    public ChatController getChatController() {
        return chatController;
    }

    /**
     * Get the stage.
     */
    public Stage getStage() {
        return stage;
    }
}
