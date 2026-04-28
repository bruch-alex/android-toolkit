package app.androidtoolkit.controller;

import app.androidtoolkit.AppState;
//import app.androidtoolkit.model.permissions.AndroidPermission;
import app.androidtoolkit.model.AppPackage;
import app.androidtoolkit.model.permissions.InstallPermission;
import app.androidtoolkit.model.permissions.RuntimePermission;
import app.androidtoolkit.viewmodel.DeviceView;
import javafx.collections.FXCollections;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.text.TextFlow;

public class PackagePermissionsController {
    private final AppState appState = AppState.getInstance();

    public ListView<InstallPermission> installPermissionsList;
    public ListView<RuntimePermission> runtimePermissionsList;
    public TextFlow permissionDetails;
    public Label totalInstallPermissionsLabel;
    public Label totalRuntimePermissionsLabel;

    public void initialize() {
        setupUI();
        appState.getConnectedDevice().addListener((observableValue, deviceView, newDevice) -> {
            if (newDevice != null) {
                dataLogic();
            }
        });
    }

    public void setupUI() {
        installPermissionsList.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(InstallPermission item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.shortName());
                }
            }
        });

    }

    public void dataLogic() {
        appState.getSelectedPackage().addListener((obs, oldVal, selectedPackage) -> {
            if (selectedPackage == null) {
                installPermissionsList.setItems(FXCollections.observableArrayList());
                runtimePermissionsList.setItems(FXCollections.observableArrayList());
                return;
            }

            installPermissionsList.setItems(
                    FXCollections.observableArrayList(selectedPackage.getInstalledPermissions())
            );
            runtimePermissionsList.setItems(
                    FXCollections.observableList(selectedPackage.getInstanceDetailsMap().get(appState.getSelectedUser().get().id()).getRuntimePermissions())
            );
        });
    }
}
