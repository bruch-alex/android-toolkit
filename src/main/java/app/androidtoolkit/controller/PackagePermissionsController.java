package app.androidtoolkit.controller;

import app.androidtoolkit.AppState;
import app.androidtoolkit.model.permissions.InstallPermission;
import app.androidtoolkit.model.permissions.RuntimePermission;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.*;
import javafx.scene.text.TextFlow;

import java.util.ArrayList;

//import app.androidtoolkit.model.permissions.AndroidPermission;

public class PackagePermissionsController {
    private final AppState appState = AppState.getInstance();
    public ListView<InstallPermission> installPermissionsList;
    public ListView<RuntimePermission> runtimePermissionsList;
    public TextFlow permissionDetails;
    public Label totalInstallPermissionsLabel;
    public Label totalRuntimePermissionsLabel;
    public CheckBox showOnlyGrantedRuntimeCheckbox;
    public Button refreshRuntimeButton;
    private FilteredList<RuntimePermission> filteredRuntimePermissions;

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

        runtimePermissionsList.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(RuntimePermission item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.shortName());
                }
            }
        });

        showOnlyGrantedRuntimeCheckbox.selectedProperty().addListener((_, _, _) -> applyFilters());
        refreshRuntimeButton.setOnAction(e -> {
            appState.forceUpdateSelectedPackage();
            applyFilters();
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
                    FXCollections.observableArrayList(selectedPackage.getPackageDetails().getInstalledPermissions())
            );


            var allRuntimePermissions =
                    selectedPackage.getInstanceDetailsMap()
                            .get(appState.getSelectedUser().get().id())
                            .getRuntimePermissions();

            if (allRuntimePermissions == null) {
                allRuntimePermissions = new ArrayList<>();
            }
            filteredRuntimePermissions = new FilteredList<>(FXCollections.observableArrayList(allRuntimePermissions), _ -> true);

            runtimePermissionsList.setItems(filteredRuntimePermissions);
        });
    }

    private void applyFilters() {
        filteredRuntimePermissions.setPredicate(perm -> {
            if (perm == null) {
                return false;
            }

            return !showOnlyGrantedRuntimeCheckbox.isSelected() || perm.granted();
        });
    }
}
