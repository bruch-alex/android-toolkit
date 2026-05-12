package app.androidtoolkit.controller;

import app.androidtoolkit.AppState;
import app.androidtoolkit.service.ADBService;
import app.androidtoolkit.utils.ADBLocator;
import atlantafx.base.controls.Card;
import com.android.ddmlib.AndroidDebugBridge;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.file.Path;
import java.util.Optional;

@Slf4j
public class SetupScreenController {
    private final AppState appState = AppState.getInstance();
    private final ADBService adb = ADBService.getInstance();

    public BorderPane rootPane;
    public Label adbStatusLabel;
    public TextField customAdbPathTextField;
    public Button setPathButton;
    public Button startButton;
    public Button scanDefaultAdbLocationsButton;
    public Card devicesCard;

    public void initialize() {
        log.debug("Starting SetupController initialization");

        setPathButton.setOnAction(_ -> onSetPathButton());
        startButton.setOnAction(_ -> onStartButton());
        scanDefaultAdbLocationsButton.setOnAction(_ -> scanDefaultAdbLocations());

        AndroidDebugBridge.addDebugBridgeChangeListener(bridge -> {
            if (bridge == null) return;

            log.debug("ADB bridge state changed");

            var statusThread = new Thread(() -> {

                while (!bridge.isConnected()) {

                    log.debug("ADB not ready yet");

                    javafx.application.Platform.runLater(() ->
                            adbStatusLabel.setText("ADB: Connecting...")
                    );

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }

                log.info("ADB ready");

                javafx.application.Platform.runLater(() -> {
                    adbStatusLabel.setText("ADB: Ready");
                    devicesCard.setVisible(true);
                });
            });

            statusThread.setDaemon(true);
            statusThread.start();
        });

        customAdbPathTextField.textProperty().addListener((_, _, newValue) -> {
            if (newValue == null || newValue.isBlank()) {
                adbStatusLabel.setText("Invalid ADB path");
                startButton.setDisable(true);
            } else {
                Optional<Path> adb = ADBLocator.resolveAdbPath(newValue);
                if (adb.isPresent()) {
                    adbStatusLabel.setText("ADB found, not started");
                    startButton.setDisable(false);
                } else {
                    adbStatusLabel.setText("Invalid ADB path");
                    startButton.setDisable(true);
                }
            }

        });
    }

    private void onSetPathButton() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Android SDK Folder");

        Stage stage = (Stage) setPathButton.getScene().getWindow();

        File selected = chooser.showDialog(stage);
        log.debug("Selected directory: {}", selected);
        if (selected != null) {
            customAdbPathTextField.setText(selected.getAbsolutePath());
        }
    }

    private void onStartButton() {
        adb.start(customAdbPathTextField.getText());
    }

    private void scanDefaultAdbLocations() {
        customAdbPathTextField.setText(
                ADBLocator.findInDefaultAdbLocations()
                        .orElse(Path.of(""))
                        .toString());
    }

    public void onBackgroundClicked(MouseEvent mouseEvent) {
        rootPane.requestFocus();
    }
}
