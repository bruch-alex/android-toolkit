package app.androidtoolkit.controller;

import app.androidtoolkit.service.ADBService;
import app.androidtoolkit.utils.ADBInstaller;
import atlantafx.base.controls.Card;
import javafx.concurrent.Task;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;

@Slf4j
public class SetupScreenController {
    public BorderPane rootPane;
    public Card devicesCard;
    public ProgressBar progressBar;
    public Label statusLabel;

    public void initialize() {
        log.debug("Starting SetupController initialization");

        var task = new AdbSetupTask();
        progressBar.progressProperty().bind(task.progressProperty());
        statusLabel.textProperty().bind(task.messageProperty());

        task.setOnSucceeded(_ -> devicesCard.setVisible(true));
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
}

class AdbSetupTask extends Task<Path> {
    private final ADBInstaller installer = new ADBInstaller();
    private final ADBService adb = ADBService.getInstance();

    @Override
    protected Path call() throws Exception {
        updateMessage("Checking ADB installation...");
        updateProgress(0, 3);
        Thread.sleep(250);

        updateMessage("Downloading platform-tools...");
        updateProgress(1, 3);
        Path adbPath = installer.getOrInstall();
        Thread.sleep(250);

        updateMessage("Starting ADB server...");
        adb.start(adbPath.toString());
        updateProgress(2, 3);
        Thread.sleep(250);

        updateMessage("ADB ready");
        updateProgress(3, 3);

        return adbPath;
    }
}
