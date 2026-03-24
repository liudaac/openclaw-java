package openclaw.desktop.controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import openclaw.desktop.component.MessageCell;
import openclaw.desktop.model.UIMessage;
import openclaw.desktop.service.ChatService;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignA;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;
import org.kordamp.ikonli.materialdesign2.MaterialDesignM;
import org.kordamp.ikonli.materialdesign2.MaterialDesignP;
import org.kordamp.ikonli.materialdesign2.MaterialDesignS;
import org.kordamp.ikonli.materialdesign2.MaterialDesignT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.UUID;

/**
 * Chat Controller for managing conversation UI.
 *
 * <p>Handles message display, input, and streaming updates.</p>
 */
@Controller
public class ChatController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

    @Autowired
    private ChatService chatService;

    @FXML
    private VBox chatContainer;

    @FXML
    private ListView<UIMessage> messageList;

    @FXML
    private TextArea inputArea;

    @FXML
    private Button sendButton;

    @FXML
    private Button abortButton;

    @FXML
    private ComboBox<String> modelSelector;

    @FXML
    private Label tokenUsageLabel;

    @FXML
    private HBox inputToolbar;

    @FXML
    private ProgressBar streamingProgress;

    private ObservableList<UIMessage> messages;
    private String currentSessionKey;
    private boolean isStreaming = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("Initializing ChatController...");

        // Initialize message list
        messages = FXCollections.observableArrayList();
        messageList.setItems(messages);
        messageList.setCellFactory(param -> new MessageCell());

        // Setup model selector
        setupModelSelector();

        // Setup input area
        setupInputArea();

        // Setup buttons
        setupButtons();

        // Create initial conversation
        createNewConversation();

        logger.info("ChatController initialized");
    }

    private void setupModelSelector() {
        modelSelector.getItems().addAll(
            "gpt-4",
            "gpt-4-turbo",
            "gpt-3.5-turbo",
            "claude-3-opus",
            "claude-3-sonnet",
            "gemini-pro"
        );
        modelSelector.getSelectionModel().selectFirst();
    }

    private void setupInputArea() {
        inputArea.setWrapText(true);
        inputArea.setPromptText("Type your message here... (Shift+Enter for new line)");

        // Handle Enter key (send) vs Shift+Enter (new line)
        inputArea.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER && !event.isShiftDown()) {
                event.consume();
                sendMessage();
            }
        });
    }

    private void setupButtons() {
        // Send button
        FontIcon sendIcon = new FontIcon(MaterialDesignS.SEND);
        sendIcon.setIconSize(18);
        sendButton.setGraphic(sendIcon);
        sendButton.setOnAction(e -> sendMessage());

        // Abort button
        FontIcon abortIcon = new FontIcon(MaterialDesignS.STOP);
        abortIcon.setIconSize(18);
        abortButton.setGraphic(abortIcon);
        abortButton.setOnAction(e -> abortGeneration());
        abortButton.setVisible(false);
    }

    /**
     * Create a new conversation.
     */
    public void createNewConversation() {
        String title = "New Chat " + System.currentTimeMillis() % 10000;
        String model = modelSelector.getSelectionModel().getSelectedItem();

        chatService.createConversation(title, model)
            .thenAccept(sessionKey -> {
                Platform.runLater(() -> {
                    this.currentSessionKey = sessionKey;
                    messages.clear();
                    inputArea.clear();
                    logger.info("Created new conversation: {}", sessionKey);
                });
            })
            .exceptionally(ex -> {
                logger.error("Failed to create conversation", ex);
                return null;
            });
    }

    /**
     * Load an existing conversation.
     */
    public void loadConversation(String sessionKey) {
        this.currentSessionKey = sessionKey;
        messages.clear();

        chatService.getHistory(sessionKey)
            .thenAccept(history -> {
                Platform.runLater(() -> {
                    messages.addAll(history);
                    scrollToBottom();
                });
            })
            .exceptionally(ex -> {
                logger.error("Failed to load conversation", ex);
                return null;
            });
    }

    /**
     * Send a message.
     */
    @FXML
    private void sendMessage() {
        if (currentSessionKey == null) {
            createNewConversation();
            return;
        }

        String message = inputArea.getText().trim();
        if (message.isEmpty()) {
            return;
        }

        // Disable input during streaming
        inputArea.setDisable(true);
        sendButton.setDisable(true);
        abortButton.setVisible(true);
        isStreaming = true;

        // Clear input
        inputArea.clear();

        // Send message
        chatService.sendMessage(
            currentSessionKey,
            message,
            this::onMessageUpdate,
            this::onComplete,
            this::onError
        );
    }

    /**
     * Handle message update (streaming).
     */
    private void onMessageUpdate(UIMessage message) {
        Platform.runLater(() -> {
            // Find existing message or add new
            boolean found = false;
            for (int i = 0; i < messages.size(); i++) {
                if (messages.get(i).getId().equals(message.getId())) {
                    messages.set(i, message);
                    found = true;
                    break;
                }
            }

            if (!found) {
                messages.add(message);
            }

            // Update token usage
            updateTokenUsage();

            // Scroll to bottom for streaming
            if (message.isStreaming()) {
                scrollToBottom();
            }
        });
    }

    /**
     * Handle completion.
     */
    private void onComplete(String result) {
        Platform.runLater(() -> {
            isStreaming = false;
            inputArea.setDisable(false);
            sendButton.setDisable(false);
            abortButton.setVisible(false);
            streamingProgress.setVisible(false);

            // Focus input
            inputArea.requestFocus();
        });
    }

    /**
     * Handle error.
     */
    private void onError(Throwable error) {
        Platform.runLater(() -> {
            isStreaming = false;
            inputArea.setDisable(false);
            sendButton.setDisable(false);
            abortButton.setVisible(false);
            streamingProgress.setVisible(false);

            // Show error
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Failed to send message");
            alert.setContentText(error.getMessage());
            alert.showAndWait();
        });
    }

    /**
     * Abort generation.
     */
    @FXML
    private void abortGeneration() {
        if (currentSessionKey != null && isStreaming) {
            chatService.abortGeneration(currentSessionKey)
                .thenRun(() -> {
                    Platform.runLater(() -> {
                        isStreaming = false;
                        inputArea.setDisable(false);
                        sendButton.setDisable(false);
                        abortButton.setVisible(false);
                    });
                });
        }
    }

    /**
     * Scroll to bottom of message list.
     */
    private void scrollToBottom() {
        if (!messages.isEmpty()) {
            messageList.scrollTo(messages.size() - 1);
        }
    }

    /**
     * Update token usage display.
     */
    private void updateTokenUsage() {
        int totalTokens = messages.stream()
            .mapToInt(UIMessage::getTokenCount)
            .sum();

        if (totalTokens > 0) {
            tokenUsageLabel.setText(String.format("%d tokens", totalTokens));
        } else {
            tokenUsageLabel.setText("");
        }
    }

    /**
     * Get current session key.
     */
    public String getCurrentSessionKey() {
        return currentSessionKey;
    }

    /**
     * Check if currently streaming.
     */
    public boolean isStreaming() {
        return isStreaming;
    }
}
