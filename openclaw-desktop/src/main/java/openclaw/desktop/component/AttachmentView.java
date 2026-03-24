package openclaw.desktop.component;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.Node;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignF;
import org.kordamp.ikonli.materialdesign2.MaterialDesignI;
import org.kordamp.ikonli.materialdesign2.MaterialDesignM;
import org.kordamp.ikonli.materialdesign2.MaterialDesignV;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Attachment View Component.
 *
 * <p>Displays file attachments with preview support.</p>
 */
public class AttachmentView extends HBox {

    private final File file;
    private final String fileType;
    private final long fileSize;

    public AttachmentView(File file) {
        this.file = file;
        this.fileType = detectFileType(file);
        this.fileSize = file.length();
        initialize();
    }


    private void initialize() {
        getStyleClass().add("attachment-view");
        setSpacing(12);
        setAlignment(Pos.CENTER_LEFT);
        setPadding(new Insets(12, 16, 12, 16));
        getStyleClass().add("attachment-view");

        // Icon or preview
        Node preview = createPreview();
        // Info
        VBox infoBox = new VBox(4);
        Label nameLabel = new Label(file.getName());
        nameLabel.getStyleClass().add("attachment-name");
        nameLabel.setWrapText(true);
        nameLabel.setMaxWidth(200);

        Label sizeLabel = new Label(formatFileSize(fileSize));
        sizeLabel.getStyleClass().add("attachment-size");

        infoBox.getChildren().addAll(nameLabel, sizeLabel);
        getChildren().addAll(preview, infoBox);
    }

    /**
     * Create preview based on file type.
     */
    private Node createPreview() {
        switch (fileType) {
            case "image":
                return createImagePreview();
            case "pdf":
                return createIconPreview(MaterialDesignF.FILE);
            case "document":
                return createIconPreview(MaterialDesignF.FILE);
            case "video":
                return createIconPreview(MaterialDesignV.VIDEO);
            case "audio":
                return createIconPreview(MaterialDesignM.MUSIC);
            default:
                return createIconPreview(MaterialDesignF.FILE);
        }
    }

    /**
     * Create image preview.
     */
    private Node createImagePreview() {
        try {
            Image image = new Image(new FileInputStream(file), 80, 80, true, true);
            ImageView imageView = new ImageView(image);
            imageView.setFitWidth(80);
            imageView.setFitHeight(80);
            imageView.setPreserveRatio(true);
            return imageView;
        } catch (IOException e) {
            // Fallback to icon
            return createIconPreview(MaterialDesignI.IMAGE);
        }
    }

    /**
     * Create icon preview.
     */
    private Node createIconPreview(String iconCode) {
        VBox iconBox = new VBox();
        iconBox.setAlignment(Pos.CENTER);
        iconBox.setPrefSize(64, 64);
        iconBox.getStyleClass().add("attachment-icon-box");

        FontIcon icon = new FontIcon(iconCode);
        icon.setIconSize(32);
        icon.getStyleClass().add("attachment-icon");

        iconBox.getChildren().add(icon);
        return iconBox;
    }

    /**
     * Detect file type.
     */
    private String detectFileType(File file) {
        try {
            String mimeType = Files.probeContentType(file.toPath());
            if (mimeType == null) {
                return "unknown";
            }
            if (mimeType.startsWith("image/")) {
                return "image";
            }
            if (mimeType.equals("application/pdf")) {
                return "pdf";
            }
            if (mimeType.startsWith("video/")) {
                return "video";
            }
            if (mimeType.startsWith("audio/")) {
                return "audio";
            }
            if (mimeType.startsWith("text/") || 
                mimeType.contains("document") ||
                mimeType.contains("msword") ||
                mimeType.contains("excel") ||
                mimeType.contains("powerpoint")) {
                return "document";
            }
            return "unknown";
        } catch (IOException e) {
            return "unknown";
        }
    }

    /**
     * Format file size.
     */
    private String formatFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        }
        if (size < 1024 * 1024) {
            return String.format("%.1f KB", size / 1024.0);
        }
        if (size < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", size / (1024.0 * 1024));
        }
        return String.format("%.1f GB", size / (1024.0 * 1024 * 1024));
    }

    public File getFile() {
        return file;
    }

    public String getFileType() {
        return fileType;
    }

    public long getFileSize() {
        return fileSize;
    }
}
