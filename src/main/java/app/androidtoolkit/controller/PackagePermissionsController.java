package app.androidtoolkit.controller;

import app.androidtoolkit.AppState;
import app.androidtoolkit.model.permissions.InstallPermission;
import app.androidtoolkit.model.permissions.RuntimePermission;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.*;
import javafx.scene.text.TextFlow;

import java.util.ArrayList;
import java.util.List;

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
        appState.getSelectedPackage().addListener((_, _, selectedPackage) -> {
            // Reset UI
            installPermissionsList.setItems(FXCollections.observableArrayList());
            runtimePermissionsList.setItems(FXCollections.observableArrayList());
            totalInstallPermissionsLabel.setText("0");
            totalRuntimePermissionsLabel.setText("0");

            if (selectedPackage == null) {
                return;
            }

            // Install permissions
            var installPermissions = selectedPackage
                    .getPackageDetails()
                    .getInstalledPermissions();

            installPermissionsList.setItems(
                    FXCollections.observableArrayList(installPermissions)
            );

            totalInstallPermissionsLabel.setText(
                    String.valueOf(installPermissions.size())
            );

            // Runtime permissions
            var user = appState.getSelectedUser().get();

            var instanceDetails = selectedPackage
                    .getInstanceDetailsMap()
                    .get(user.id());

            List<RuntimePermission> allRuntimePermissions = (instanceDetails != null &&
                    instanceDetails.getRuntimePermissions() != null)
                    ? instanceDetails.getRuntimePermissions()
                    : new ArrayList<>();

            totalRuntimePermissionsLabel.setText(
                    String.valueOf(allRuntimePermissions.size())
            );

            filteredRuntimePermissions = new FilteredList<>(
                    FXCollections.observableArrayList(allRuntimePermissions),
                    _ -> true);
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
