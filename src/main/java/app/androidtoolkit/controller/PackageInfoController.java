package app.androidtoolkit.controller;

import app.androidtoolkit.AppState;
import app.androidtoolkit.service.ADBService;
import javafx.collections.FXCollections;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;

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

    public void initialize() {
        appState.getConnectedDevice().addListener((_, _, newDevice) -> {
            if (newDevice != null) {
                appState.getSelectedPackage().addListener((_, _, newPackage) -> {
                    if (newPackage == null) {
                        packageNameLabel.setText("No package selected");
                        appIdLabel.setText("");
                        versionNameLabel.setText("");
                        totalQueriedPackagesLabel.setText("");
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

                    toggleEnabledStatusButton.setOnAction(_ -> {
                        var uid = appState.getSelectedUser().get().id();
                        var packageName = newPackage.getPackageName();
                        if (enabled) {
                            try {
                                adb.setEnabledToDisabled(packageName, uid);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } else {
                            try {
                                adb.setEnabledToDefaultState(packageName, uid);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        appState.forceUpdateSelectedPackage();
                    });

                    deletePackageButton.setOnAction(_ -> {
                        var uid = appState.getSelectedUser().get().id();
                        var packageName = newPackage.getPackageName();
                        try {
                            adb.deleteAppForUser(packageName, uid);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                });
            }
        });
    }
}
