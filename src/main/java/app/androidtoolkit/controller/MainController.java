package app.androidtoolkit.controller;

import app.androidtoolkit.AppState;
import app.androidtoolkit.service.ADBService;
import app.androidtoolkit.utils.ADBLocator;
import javafx.beans.binding.Bindings;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

public class MainController {
    private final ADBService adb = ADBService.getInstance();
    private final AppState appState = AppState.getInstance();

    public Label deviceModelLabel;
    public Label androidVersionLabel;
    public Label deviceManufacturerLabel;
    public Label isAdbInstalledLabel;
    public HBox androidVersionContainer;
    public HBox manufacturerContainer;
    public HBox deviceModelContainer;

    public void initialize() {
        System.out.println("MAIN CONTROLLER: DEVICE INSTANCE: " + adb);
        isAdbInstalledLabel.setText(ADBLocator.isAdbInstalled() ? "Installed" : "Not found");
        deviceModelLabel.textProperty().bind(Bindings.selectString(appState.getConnectedDevice(), "model"));
        androidVersionLabel.textProperty().bind(Bindings.selectString(appState.getConnectedDevice(), "androidVersion"));
        deviceManufacturerLabel.textProperty().bind(Bindings.selectString(appState.getConnectedDevice(), "manufacturer"));

        appState.getConnectedDevice().addListener((_, _, newValue) -> {
            if (newValue != null) {
                deviceModelContainer.setVisible(true);
                androidVersionContainer.setVisible(true);
                manufacturerContainer.setVisible(true);
            } else {
                deviceModelContainer.setVisible(false);
                androidVersionContainer.setVisible(false);
                manufacturerContainer.setVisible(false);
            }
        });
    }
}

