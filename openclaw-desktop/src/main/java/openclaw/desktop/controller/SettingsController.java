package openclaw.desktop.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import openclaw.desktop.config.DesktopConfig;
import openclaw.desktop.service.SecureStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * Settings Controller for application preferences.
 */
@Controller
public class SettingsController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(SettingsController.class);

    @Autowired
    private DesktopConfig config;

    @Autowired
    private SecureStorageService secureStorage;

    @FXML private VBox settingsContainer;
    @FXML private ComboBox<String> themeSelector;
    @FXML private Slider fontScaleSlider;
    @FXML private Label fontScaleLabel;
    @FXML private CheckBox animationsCheck;
    @FXML private CheckBox transparencyCheck;
    @FXML private CheckBox autoSaveCheck;
    @FXML private CheckBox streamingCheck;
    @FXML private CheckBox tokenUsageCheck;
    @FXML private TextField defaultModelField;
    @FXML private TextField exportPathField;
    @FXML private Button browseExportPathButton;
    @FXML private Button saveButton;
    @FXML private Button resetButton;
    @FXML private ComboBox<String> providerSelector;
    @FXML private Label apiKeyStatusLabel;
    @FXML private Button saveApiKeyButton;
    @FXML private Button deleteApiKeyButton;
    @FXML private TextField apiKeyField;
    @FXML private PasswordField masterPasswordField;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("Initializing SettingsController...");
        loadSettings();
        setupListeners();
        logger.info("SettingsController initialized");
    }

    private void loadSettings() {
        // Theme
        themeSelector.getItems().addAll("Dark", "Light", "Auto");
        themeSelector.getSelectionModel().select(config.getTheme().name());

        // Font scale
        fontScaleSlider.setValue(config.getFontScale());
        fontScaleLabel.setText(String.format("%.1fx", config.getFontScale()));

        // Checkboxes
        animationsCheck.setSelected(config.isAnimationsEnabled());
        transparencyCheck.setSelected(config.isTransparencyEnabled());
        autoSaveCheck.setSelected(config.isAutoSave());
        streamingCheck.setSelected(config.isStreamingEnabled());
        tokenUsageCheck.setSelected(config.isShowTokenUsage());

        // Text fields
        defaultModelField.setText(config.getDefaultModel());
        exportPathField.setText(config.getExportPath().toString());
    }

    private void setupListeners() {
        fontScaleSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            fontScaleLabel.setText(String.format("%.1fx", newVal.doubleValue()));
        });

        browseExportPathButton.setOnAction(e -> browseExportPath());
        saveButton.setOnAction(e -> saveSettings());
        resetButton.setOnAction(e -> resetSettings());

        // API Key setup
        setupApiKeySection();
    }

    private void setupApiKeySection() {
        if (providerSelector != null) {
            providerSelector.getItems().addAll("openai", "anthropic", "google", "azure");
            providerSelector.getSelectionModel().selectFirst();
            providerSelector.setOnAction(e -> updateApiKeyStatus());
        }
        if (saveApiKeyButton != null) {
            saveApiKeyButton.setOnAction(e -> saveApiKey());
        }
        if (deleteApiKeyButton != null) {
            deleteApiKeyButton.setOnAction(e -> deleteApiKey());
        }
        updateApiKeyStatus();
    }

    private void updateApiKeyStatus() {
        if (providerSelector == null || apiKeyStatusLabel == null || deleteApiKeyButton == null) return;
        
        String provider = providerSelector.getValue();
        if (provider == null) return;

        boolean hasKey = secureStorage.hasApiKey(provider);
        apiKeyStatusLabel.setText(hasKey ? "API Key stored" : "No API Key");
        apiKeyStatusLabel.getStyleClass().setAll(hasKey ? "status-stored" : "status-missing");
        deleteApiKeyButton.setDisable(!hasKey);
    }

    @FXML
    private void saveApiKey() {
        if (providerSelector == null || apiKeyField == null || masterPasswordField == null) return;
        
        String provider = providerSelector.getValue();
        String apiKey = apiKeyField.getText().trim();
        String password = masterPasswordField.getText();

        if (provider == null || apiKey.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Missing Information", 
                "Please fill in all fields");
            return;
        }

        try {
            secureStorage.storeApiKey(provider, apiKey, password);
            apiKeyField.clear();
            masterPasswordField.clear();
            updateApiKeyStatus();
            showAlert(Alert.AlertType.INFORMATION, "Success", 
                "API Key stored securely");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", 
                "Failed to store API key: " + e.getMessage());
        }
    }

    @FXML
    private void deleteApiKey() {
        if (providerSelector == null) return;
        
        String provider = providerSelector.getValue();
        if (provider == null) return;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete API Key");
        alert.setHeaderText("Delete API Key for " + provider + "?");
        alert.setContentText("This action cannot be undone.");

        alert.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                secureStorage.deleteApiKey(provider);
                updateApiKeyStatus();
            }
        });
    }

    @FXML
    private void browseExportPath() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Export Directory");
        File current = config.getExportPath().toFile();
        if (current.exists()) {
            chooser.setInitialDirectory(current);
        }

        File selected = chooser.showDialog(settingsContainer.getScene().getWindow());
        if (selected != null) {
            exportPathField.setText(selected.getAbsolutePath());
        }
    }

    @FXML
    private void saveSettings() {
        try {
            // Apply settings
            config.setTheme(DesktopConfig.UITheme.valueOf(themeSelector.getValue().toUpperCase()));
            config.setFontScale(fontScaleSlider.getValue());
            config.setAnimationsEnabled(animationsCheck.isSelected());
            config.setTransparencyEnabled(transparencyCheck.isSelected());
            config.setAutoSave(autoSaveCheck.isSelected());
            config.setStreamingEnabled(streamingCheck.isSelected());
            config.setShowTokenUsage(tokenUsageCheck.isSelected());
            config.setDefaultModel(defaultModelField.getText());
            config.setExportPath(java.nio.file.Path.of(exportPathField.getText()));

            // Show success
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Settings Saved");
            alert.setHeaderText(null);
            alert.setContentText("Settings have been saved successfully.");
            alert.showAndWait();

            logger.info("Settings saved");
        } catch (Exception e) {
            logger.error("Failed to save settings", e);
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Failed to save settings");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    private void resetSettings() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Reset Settings");
        alert.setHeaderText("Reset all settings to default?");
        alert.setContentText("This action cannot be undone.");

        alert.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                config.setTheme(DesktopConfig.UITheme.DARK);
                config.setFontScale(1.0);
                config.setAnimationsEnabled(true);
                config.setTransparencyEnabled(true);
                config.setAutoSave(true);
                config.setStreamingEnabled(true);
                config.setShowTokenUsage(true);
                config.setDefaultModel("gpt-4");

                loadSettings();
                logger.info("Settings reset to default");
            }
        });
    }
}
