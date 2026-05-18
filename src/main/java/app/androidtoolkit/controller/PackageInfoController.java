package app.androidtoolkit.controller;

import app.androidtoolkit.AppState;
import app.androidtoolkit.model.AppPackage;
import app.androidtoolkit.service.ADBService;
import app.androidtoolkit.utils.DialogUtils;
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
                    container.setVisible(true);
                });
            }
        });
    }



}

