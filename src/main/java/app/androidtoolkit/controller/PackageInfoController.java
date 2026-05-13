package app.androidtoolkit.controller;

import app.androidtoolkit.AppState;
import app.androidtoolkit.model.AppPackage;
import app.androidtoolkit.service.ADBService;
import javafx.collections.FXCollections;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PackageInfoController {
    private final AppState appState = AppState.getInstance();
    private final ADBService adb = ADBService.getInstance();

    public Label totalQueriedPackagesLabel;
    public Label packageNameLabel;
    public Label enabledStatusLabel;
    public Label appIdLabel;
    public Label versionNameLabel;
    public Button toggleEnabledStatusButton;
    public Button deletePackageButton;
    public ListView<String> queriedPackagesListView;
    public GridPane container;

    public void initialize() {
        appState.getConnectedDevice().addListener((_, _, newDevice) -> {
            if (newDevice != null) {
                appState.getSelectedPackage().addListener((_, _, newPackage) -> {
                    if (newPackage == null) {
                        container.setVisible(false);
                        queriedPackagesListView.setItems(FXCollections.observableArrayList());
                        return;
                    }
                    var enabled = newPackage.getInstanceDetailsMap().get(appState.getSelectedUser().get().id()).isEnabled();
                    enabledStatusLabel.setText(enabled ? "Enabled" : "Disabled");

                    packageNameLabel.setText(newPackage.getPackageName());
                    appIdLabel.setText(newPackage.getPackageDetails().getAppId());
                    versionNameLabel.setText(newPackage.getPackageDetails().getVersionName());
                    totalQueriedPackagesLabel.setText(String.valueOf(newPackage.getPackageDetails().getQueriesPackages().size()));
                    queriedPackagesListView.setItems(
                            FXCollections.observableArrayList(newPackage.getPackageDetails().getQueriesPackages()));
                    toggleEnabledStatusButton.setText(enabled ? "Disable" : "Enable");

                    toggleEnabledStatusButton.setOnAction(_ -> toggleEnabledStatus(newPackage, enabled));
                    deletePackageButton.setOnAction(_ -> deletePackage(newPackage));

                    container.setVisible(true);
                });
            }
        });
    }

    private void toggleEnabledStatus(AppPackage newPackage, boolean enabled) {
        var uid = appState.getSelectedUser().get().id();
        var packageName = newPackage.getPackageName();
        if (enabled) {
            try {
                if (showConfirmationDialog("Disable Package", "Are you sure you want to disable this package?")) {
                    adb.setEnabledToDisabled(packageName, uid);
                }

            } catch (Exception e) {
                log.error("Failed to disable package: {}", packageName, e);
            }
        } else {
            try {
                if (showConfirmationDialog("Enable Package", "Are you sure you want to enable this package?")) {
                    adb.setEnabledToDefaultState(packageName, uid);
                }
            } catch (Exception e) {
                log.error("Failed to enable package: {}", packageName, e);
            }
        }
        appState.forceUpdateSelectedPackage();
    }

    private void deletePackage(AppPackage newPackage) {
        var uid = appState.getSelectedUser().get().id();
        var packageName = newPackage.getPackageName();
        try {
            if (showConfirmationDialog("Delete Package", "Are you sure you want to delete this package?", true)) {
                adb.deleteAppForUser(packageName, uid);
            }
        } catch (Exception e) {
            log.error("Failed to delete package: {}", packageName, e);
        }
    }

    private boolean showConfirmationDialog(String title, String headerText, boolean permanent) {
        var alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);

        if (permanent) {
            headerText += "\nThis action cannot be undone.";
        }
        alert.setContentText(headerText);
        return alert.showAndWait()
                .filter(response -> response == ButtonType.OK)
                .isPresent();
    }

    private boolean showConfirmationDialog(String title, String headerText) {
        return showConfirmationDialog(title, headerText, false);
    }
}

