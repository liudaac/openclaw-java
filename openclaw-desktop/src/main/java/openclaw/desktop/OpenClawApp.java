package openclaw.desktop;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import openclaw.desktop.config.DesktopConfig;
import openclaw.desktop.controller.MainController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.IOException;
import java.util.Objects;

/**
 * OpenClaw Desktop Application Entry Point.
 *
 * <p>Combines Spring Boot with JavaFX for a modern desktop experience.
 * Directly integrates with OpenClaw services without REST API overhead.</p>
 *
 * @author OpenClaw Team
 * @version 2026.3.24
 */
@SpringBootApplication(scanBasePackages = {"openclaw.desktop", "openclaw.server"})
public class OpenClawApp extends Application {

    private static final Logger logger = LoggerFactory.getLogger(OpenClawApp.class);

    /** Spring Boot application context */
    private ConfigurableApplicationContext springContext;

    /** Primary stage reference */
    private static Stage primaryStage;

    /**
     * Main entry point.
     */
    public static void main(String[] args) {
        logger.info("Starting OpenClaw Desktop...");
        launch(args);
    }

    /**
     * Initialize Spring Boot context before JavaFX starts.
     */
    @Override
    public void init() {
        logger.info("Initializing Spring Boot context...");
        springContext = SpringApplication.run(OpenClawApp.class);
        logger.info("Spring Boot context initialized successfully");
    }

    /**
     * Start the JavaFX application.
     */
    @Override
    public void start(Stage stage) throws IOException {
        primaryStage = stage;

        // Load configuration
        DesktopConfig config = springContext.getBean(DesktopConfig.class);

        // Setup stage
        stage.setTitle("OpenClaw Desktop");
        stage.initStyle(StageStyle.DECORATED);

        // Load FXML with Spring controller factory
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
        loader.setControllerFactory(springContext::getBean);

        Parent root = loader.load();
        MainController controller = loader.getController();
        controller.setStage(stage);

        // Create scene with transparency support
        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);

        // Apply theme
        applyTheme(scene, config.getTheme());

        // Setup stage
        stage.setScene(scene);
        stage.setMinWidth(1200);
        stage.setMinHeight(800);
        stage.setWidth(1400);
        stage.setHeight(900);

        // Center on screen
        stage.centerOnScreen();

        // Show
        stage.show();

        logger.info("OpenClaw Desktop started successfully");
    }

    /**
     * Clean up resources on exit.
     */
    @Override
    public void stop() {
        logger.info("Shutting down OpenClaw Desktop...");
        if (springContext != null) {
            springContext.close();
        }
        Platform.exit();
        logger.info("OpenClaw Desktop stopped");
    }

    /**
     * Apply theme to scene.
     */
    private void applyTheme(Scene scene, DesktopConfig.UITheme theme) {
        String cssFile = switch (theme) {
            case DARK -> "/css/modern-dark.css";
            case LIGHT -> "/css/modern-light.css";
            case AUTO -> "/css/modern-dark.css"; // TODO: detect system theme
        };

        scene.getStylesheets().clear();
        scene.getStylesheets().add(Objects.requireNonNull(
            getClass().getResource(cssFile)).toExternalForm());
    }

    /**
     * Get the primary stage.
     */
    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    /**
     * Get the Spring application context.
     */
    public static ConfigurableApplicationContext getSpringContext() {
        OpenClawApp app = (OpenClawApp) primaryStage.getProperties().get("app");
        return app != null ? app.springContext : null;
    }
}
