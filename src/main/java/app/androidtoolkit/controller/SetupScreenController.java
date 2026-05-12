package app.androidtoolkit.controller;

import app.androidtoolkit.AppState;
import app.androidtoolkit.service.ADBService;
import app.androidtoolkit.utils.ADBLocator;
import atlantafx.base.controls.Card;
import atlantafx.base.layout.InputGroup;
import atlantafx.base.theme.Styles;
import com.android.ddmlib.AndroidDebugBridge;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
public class SetupScreenController {
    private static final int ADB_MAX_CONNECT_ATTEMPTS = 10;
    private final AppState appState = AppState.getInstance();
    private final ADBService adb = ADBService.getInstance();
    private final ScheduledExecutorService adbScheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "adb-status-poller");
                t.setDaemon(true);
                return t;
            });
    public BorderPane rootPane;
    public Label adbStatusLabel;
    public TextField customAdbPathTextField;
    public Button setPathButton;
    public Button startButton;
    public Button scanDefaultAdbLocationsButton;
    public Card devicesCard;
    public InputGroup pathGroup;
    private ChangeListener<Boolean> adbServiceListener;
    private ScheduledFuture<?> adbPollTask;

    public void initialize() {
        log.debug("Starting SetupController initialization");

        setPathButton.setOnAction(_ -> onSetPathButton());
        startButton.setOnAction(_ -> startAdb());
        scanDefaultAdbLocationsButton.setOnAction(_ -> scanDefaultAdbLocations());

        AndroidDebugBridge.addDebugBridgeChangeListener(bridge -> {
            log.debug("ADB bridge state changed");

            if (bridge == null) {
                log.debug("ADB bridge is null");
                return;
            }

            if (adbServiceListener != null) {
                adb.isAdbServiceRunningProperty().removeListener(adbServiceListener);
            }
            adbServiceListener = (_, _, isRunning) ->
                    Platform.runLater(() -> {
                        adbStatusLabel.setText(isRunning ? "ADB: Ready" : "ADB: Not running");
                        updateAdbButton(isRunning);
                    });
            adb.isAdbServiceRunningProperty().addListener(adbServiceListener);

            if (adbPollTask != null) {
                adbPollTask.cancel(true);
            }

            log.debug("ADB bridge state changed");

            final int[] attempts = {0};
            adbPollTask = adbScheduler.scheduleWithFixedDelay(() -> {
                if (Thread.currentThread().isInterrupted()) return;

                if (bridge.isConnected()) {
                    log.info("ADB ready");
                    Platform.runLater(() -> {
                        adbStatusLabel.setText("ADB: Running");
                        devicesCard.setVisible(true);
                        updateAdbButton(true);
                    });
                    adbPollTask.cancel(false);

                } else if (++attempts[0] >= ADB_MAX_CONNECT_ATTEMPTS) {
                    log.warn("ADB failed to connect after {} attempts", ADB_MAX_CONNECT_ATTEMPTS);
                    Platform.runLater(() -> {
                        adbStatusLabel.setText("ADB: Failed to connect");
                        updateAdbButton(false);
                    });
                    adbPollTask.cancel(false);

                } else {
                    log.debug("ADB not ready yet (attempt {}/{})", attempts[0], ADB_MAX_CONNECT_ATTEMPTS);
                    Platform.runLater(() ->
                            adbStatusLabel.setText("ADB: Connecting...")
                    );
                }
            }, 0, 1, TimeUnit.SECONDS);
        });

        customAdbPathTextField.textProperty().addListener((_, _, newValue) -> {
            Optional<Path> adb = ADBLocator.resolveAdbPath(newValue);
            if (adb.isPresent()) {
                adbStatusLabel.setText("ADB found, not started");
                customAdbPathTextField.pseudoClassStateChanged(Styles.STATE_SUCCESS, true);
                setPathButton.getStyleClass().add(Styles.SUCCESS);
                startButton.setDisable(false);
            } else {
                adbStatusLabel.setText("Invalid ADB path");
                setPathButton.getStyleClass().remove(Styles.SUCCESS);
                customAdbPathTextField.pseudoClassStateChanged(Styles.STATE_SUCCESS, false);
                startButton.setDisable(true);
            }
        });
    }

    private void updateAdbButton(boolean isRunning) {
        startButton.setText(isRunning ? "Stop ADB" : "Start ADB");
        startButton.setOnAction(isRunning ? _ -> stopAdb() : _ -> startAdb());
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

    private void startAdb() {
        adb.start(customAdbPathTextField.getText());
    }

    private void stopAdb() {
        adb.stop();
        devicesCard.setVisible(false);
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
