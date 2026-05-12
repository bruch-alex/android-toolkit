package app.androidtoolkit.controller;

import app.androidtoolkit.AppState;
import app.androidtoolkit.service.ADBService;
import app.androidtoolkit.utils.ADBLocator;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
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

    public Label adbStatusLabel;
    public TextField customAdbPathTextField;
    public Button setPathButton;
    public Button startButton;

    public void initialize() {
        log.debug("Starting SetupController initialization");
        customAdbPathTextField.setText(ADBLocator.findAdbPath().get().toString());
        adbStatusLabel.setText(ADBLocator.isAdbInstalled() ? "ADB installed" : "ADB not installed");

        setPathButton.setOnAction(_ -> onSetPathButton());
        startButton.setOnAction(_ -> onStartButton());
    }

    private void onSetPathButton() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Android SDK Folder");

        Stage stage = (Stage) setPathButton.getScene().getWindow();

        File selected = chooser.showDialog(stage);
        log.debug("Selected directory: {}", selected);
        if (selected != null) {
            customAdbPathTextField.setText(selected.getAbsolutePath());
            Optional<Path> adb = ADBLocator.resolveAdbPath(selected.getAbsolutePath());

            if (adb.isPresent()) {
                adbStatusLabel.setText("ADB found: " + adb.get());
            } else {
                adbStatusLabel.setText("Invalid ADB path");
            }
        }
    }

    private void onStartButton() {
        adb.start(customAdbPathTextField.getText());
    }
}
