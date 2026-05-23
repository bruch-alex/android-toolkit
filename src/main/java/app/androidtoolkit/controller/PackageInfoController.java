package app.androidtoolkit.controller;

import app.androidtoolkit.AppState;
import app.androidtoolkit.model.AppPackage;
import app.androidtoolkit.service.ADBService;
import atlantafx.base.theme.Styles;
import javafx.collections.FXCollections;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PackageInfoController {
    private final AppState appState = AppState.getInstance();
    private final ADBService adb = ADBService.getInstance();

    public Label totalQueriedPackagesLabel;
    public ListView<String> queriedPackagesListView;
    public GridPane container;
    public VBox packageDetailsContainer;

    public void initialize() {
        appState.getConnectedDevice().addListener((_, _, newDevice) -> {
            if (newDevice != null) {
                appState.getSelectedPackage().addListener((_, _, newPackage) -> {
                    if (newPackage == null) {
                        container.setVisible(false);
                        queriedPackagesListView.setItems(FXCollections.observableArrayList());
                        return;
                    }
                    setupDetailsContainer(newPackage);
                    totalQueriedPackagesLabel.setText(String.valueOf(newPackage.getPackageDetails().getQueriesPackages().size()));
                    queriedPackagesListView.setItems(
                            FXCollections.observableArrayList(newPackage.getPackageDetails().getQueriesPackages()));
                    container.setVisible(true);
                });
            }
        });
    }

    private void setupDetailsContainer(AppPackage newPackage) {
        packageDetailsContainer.getChildren().clear();

        var packageNameLabel = new Label(newPackage.getPackageName());
        packageNameLabel.getStyleClass().addAll("text-bold", Styles.TITLE_3);

        var enabled = newPackage.getInstanceDetailsMap().get(appState.getSelectedUser().get().id()).isEnabled();
        var enabledStatusLabel = new Label(enabled ? "Enabled" : "Disabled");

        var appIdLabel = new Label(newPackage.getPackageDetails().getAppId());
        var appIdContainer = new HBox(2, new Label("App ID:"), appIdLabel);

        var versionNameLabel = new Label(newPackage.getPackageDetails().getVersionName());
        var versionNameContainer = new HBox(2, new Label("Version:"), versionNameLabel);

        packageDetailsContainer.getChildren().addAll(
                packageNameLabel,
                enabledStatusLabel,
                appIdContainer,
                versionNameContainer
        );
    }
}

