package openclaw.desktop.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import openclaw.desktop.config.DesktopConfig;
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
