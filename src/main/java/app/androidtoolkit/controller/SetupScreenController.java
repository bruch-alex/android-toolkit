package app.androidtoolkit.controller;

import app.androidtoolkit.AppState;
import app.androidtoolkit.model.device.AndroidDeviceRecord;
import app.androidtoolkit.service.ADBService;
import app.androidtoolkit.utils.ADBInstaller;
import atlantafx.base.controls.Card;
import javafx.concurrent.Task;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;

@Slf4j
public class SetupScreenController {
    private final ADBService adb = ADBService.getInstance();
    private final AppState state = AppState.getInstance();

    public BorderPane rootPane;
    public Card devicesCard;
    public ProgressBar progressBar;
    public Label statusLabel;
    public ListView<AndroidDeviceRecord> devicesListView;
    public Button connectButton;

    public void initialize() {
        log.debug("Starting SetupController initialization");

        var task = new AdbSetupTask();
        progressBar.progressProperty().bind(task.progressProperty());
        statusLabel.textProperty().bind(task.messageProperty());
        connectButton.setOnMouseClicked(_ -> connectToDevice());

        task.setOnSucceeded(_ -> {
            onAdbSucceed();
        });
        task.setOnFailed(_ -> {
            statusLabel.setText("Failed to setup ADB");
            devicesCard.setVisible(false);
        });
        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    public void onBackgroundClicked(MouseEvent mouseEvent) {
        rootPane.requestFocus();
    }

    public void connectToDevice() {
        var selectedDevice = devicesListView.getSelectionModel().getSelectedItem();
        if (selectedDevice != null) {
            adb.connectToDevice(selectedDevice);
        }
    }

    public void onAdbSucceed() {
        devicesCard.setVisible(true);
        devicesListView.setCellFactory(_ -> new ListCell<>() {
            @Override
            protected void updateItem(AndroidDeviceRecord device, boolean empty) {
                super.updateItem(device, empty);
                if (empty || device == null) {
                    setText(null);
                } else {
                    setText(device.model() + " (serial: " + device.serial() + " )");
                }
            }
        });
        devicesListView.setItems(state.getConnectedDevices());
    }

}

class AdbSetupTask extends Task<Path> {
    private final ADBInstaller installer = new ADBInstaller();
    private final ADBService adb = ADBService.getInstance();

    @Override
    protected Path call() throws Exception {
        updateMessage("Downloading platform-tools...");
        updateProgress(0, 2);
        Path adbPath = installer.getOrInstall();
        Thread.sleep(250);

        updateMessage("Starting ADB server...");
        adb.start(adbPath.toString());
        updateProgress(1, 2);
        Thread.sleep(250);

        updateMessage("ADB ready");
        updateProgress(2, 2);

        return adbPath;
    }
}
