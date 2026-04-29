package app.androidtoolkit.controller;

import app.androidtoolkit.AppState;
import javafx.collections.FXCollections;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;

public class PackageInfoController {
    public Label totalQueriedPackagesLabel;
    public Label packageNameLabel;
    public Label appIdLabel;
    public Label versionNameLabel;
    AppState appState = AppState.getInstance();

    public ListView<String> queriedPackagesListView;

    public void initialize() {
        appState.getConnectedDevice().addListener((_, _, newDevice) -> {
            if (newDevice != null) {
                appState.getSelectedPackage().addListener((_,_,newPackage) -> {
                    if (newPackage == null) {
                        return;
                    }
                    packageNameLabel.setText(newPackage.getPackageName());
                    appIdLabel.setText(newPackage.getPackageDetails().getAppId());
                    versionNameLabel.setText(newPackage.getPackageDetails().getVersionName());
                    totalQueriedPackagesLabel.setText(String.valueOf(newPackage.getPackageDetails().getQueriesPackages().size()));
                    queriedPackagesListView.setItems(
                            FXCollections.observableArrayList(newPackage.getPackageDetails().getQueriesPackages()));
                });
            }
        });
    }
}
