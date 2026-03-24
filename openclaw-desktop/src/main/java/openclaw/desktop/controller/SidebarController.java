package openclaw.desktop.controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import openclaw.desktop.service.ChatService;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignA;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;
import org.kordamp.ikonli.materialdesign2.MaterialDesignH;
import org.kordamp.ikonli.materialdesign2.MaterialDesignM;
import org.kordamp.ikonli.materialdesign2.MaterialDesignP;
import org.kordamp.ikonli.materialdesign2.MaterialDesignS;
import org.kordamp.ikonli.materialdesign2.MaterialDesignT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

/**
 * Sidebar Controller for navigation and conversation list.
 *
 * <p>Manages the left sidebar with navigation and conversation history.</p>
 */
@Controller
public class SidebarController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(SidebarController.class);

    @Autowired
    private ChatService chatService;

    @FXML
    private VBox sidebarRoot;

    @FXML
    private Button newChatButton;

    @FXML
    private TextField searchField;

    @FXML
    private ListView<ChatService.ConversationSession> conversationList;

    @FXML
    private VBox navButtons;

    @FXML
    private Button chatNavButton;

    @FXML
    private Button toolsNavButton;

    @FXML
    private Button settingsNavButton;

    @FXML
    private Label versionLabel;

    private MainController mainController;
    private ObservableList<ChatService.ConversationSession> conversations;
    private String activeTab = "chat";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("Initializing SidebarController...");

        // Setup new chat button
        setupNewChatButton();

        // Setup search
        setupSearch();

        // Setup conversation list
        setupConversationList();

        // Setup navigation
        setupNavigation();

        // Load conversations
        loadConversations();

        logger.info("SidebarController initialized");
    }

    private void setupNewChatButton() {
        FontIcon icon = new FontIcon(MaterialDesignP.PLUS);
        icon.setIconSize(18);
        newChatButton.setGraphic(icon);
        newChatButton.setOnAction(e -> {
            if (mainController != null) {
                mainController.createNewConversation();
            }
        });
    }

    private void setupSearch() {
        searchField.setPromptText("Search conversations...");
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.isEmpty()) {
                loadConversations();
            } else {
                searchConversations(newVal);
            }
        });
    }

    private void setupConversationList() {
        conversations = FXCollections.observableArrayList();
        conversationList.setItems(conversations);
        conversationList.setCellFactory(param -> new ConversationCell());

        // Handle selection
        conversationList.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldVal, newVal) -> {
                if (newVal != null && mainController != null) {
                    mainController.switchToConversation(newVal.getSessionKey());
                }
            }
        );

        // Context menu
        ContextMenu contextMenu = new ContextMenu();
        MenuItem renameItem = new MenuItem("Rename");
        MenuItem deleteItem = new MenuItem("Delete");

        renameItem.setOnAction(e -> {
            ChatService.ConversationSession session = conversationList.getSelectionModel().getSelectedItem();
            if (session != null) {
                renameConversation(session);
            }
        });

        deleteItem.setOnAction(e -> {
            ChatService.ConversationSession session = conversationList.getSelectionModel().getSelectedItem();
            if (session != null) {
                deleteConversation(session);
            }
        });

        contextMenu.getItems().addAll(renameItem, deleteItem);
        conversationList.setContextMenu(contextMenu);
    }

    private void setupNavigation() {
        // Chat button
        FontIcon chatIcon = new FontIcon(MaterialDesignM.MESSAGE_TEXT);
        chatIcon.setIconSize(20);
        chatNavButton.setGraphic(chatIcon);
        chatNavButton.setOnAction(e -> {
            setActiveTab("chat");
            if (mainController != null) {
                mainController.showChatView();
            }
        });

        // Tools button
        FontIcon toolsIcon = new FontIcon(MaterialDesignT.TOOLS);
        toolsIcon.setIconSize(20);
        toolsNavButton.setGraphic(toolsIcon);
        toolsNavButton.setOnAction(e -> {
            setActiveTab("tools");
            if (mainController != null) {
                mainController.showToolsView();
            }
        });

        // Settings button
        FontIcon settingsIcon = new FontIcon(MaterialDesignC.COG);
        settingsIcon.setIconSize(20);
        settingsNavButton.setGraphic(settingsIcon);
        settingsNavButton.setOnAction(e -> {
            setActiveTab("settings");
            if (mainController != null) {
                mainController.showSettingsView();
            }
        });

        // Set initial active
        setActiveTab("chat");
    }

    private void loadConversations() {
        conversations.clear();
        conversations.addAll(chatService.getActiveSessions().values());
    }

    private void searchConversations(String keyword) {
        chatService.searchConversations(keyword)
            .thenAccept(results -> {
                Platform.runLater(() -> {
                    conversations.clear();
                    conversations.addAll(results);
                });
            });
    }

    private void renameConversation(ChatService.ConversationSession session) {
        TextInputDialog dialog = new TextInputDialog(session.getTitle());
        dialog.setTitle("Rename Conversation");
        dialog.setHeaderText("Enter new name:");
        dialog.setContentText("Name:");

        dialog.showAndWait().ifPresent(newName -> {
            if (!newName.isEmpty()) {
                chatService.renameConversation(session.getSessionKey(), newName);
                loadConversations();
            }
        });
    }

    private void deleteConversation(ChatService.ConversationSession session) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Conversation");
        alert.setHeaderText("Delete \"" + session.getTitle() + "\"?");
        alert.setContentText("This action cannot be undone.");

        alert.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                chatService.deleteConversation(session.getSessionKey())
                    .thenRun(() -> Platform.runLater(this::loadConversations));
            }
        });
    }

    public void setMainController(MainController controller) {
        this.mainController = controller;
    }

    public void setActiveTab(String tab) {
        this.activeTab = tab;
        updateNavStyles();
    }

    private void updateNavStyles() {
        // Reset all
        chatNavButton.getStyleClass().remove("nav-button-active");
        toolsNavButton.getStyleClass().remove("nav-button-active");
        settingsNavButton.getStyleClass().remove("nav-button-active");

        // Set active
        switch (activeTab) {
            case "chat" -> chatNavButton.getStyleClass().add("nav-button-active");
            case "tools" -> toolsNavButton.getStyleClass().add("nav-button-active");
            case "settings" -> settingsNavButton.getStyleClass().add("nav-button-active");
        }
    }

    /**
     * Custom cell for conversation list.
     */
    private class ConversationCell extends ListCell<ChatService.ConversationSession> {
        private final HBox container;
        private final Label titleLabel;
        private final Label metaLabel;

        public ConversationCell() {
            container = new HBox(8);
            container.setPadding(new javafx.geometry.Insets(8, 12, 8, 12));
            container.getStyleClass().add("conversation-item");

            titleLabel = new Label();
            titleLabel.getStyleClass().add("conversation-title");

            metaLabel = new Label();
            metaLabel.getStyleClass().add("conversation-meta");

            VBox textBox = new VBox(2, titleLabel, metaLabel);
            container.getChildren().add(textBox);
        }

        @Override
        protected void updateItem(ChatService.ConversationSession session, boolean empty) {
            super.updateItem(session, empty);

            if (empty || session == null) {
                setGraphic(null);
                return;
            }

            titleLabel.setText(session.getTitle());
            metaLabel.setText(session.getLastActivity().format(
                DateTimeFormatter.ofPattern("MMM d, HH:mm")));

            setGraphic(container);
        }
    }
}
