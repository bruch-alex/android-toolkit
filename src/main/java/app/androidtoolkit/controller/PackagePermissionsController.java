package app.androidtoolkit.controller;

import app.androidtoolkit.AppState;
import app.androidtoolkit.model.permissions.InstallPermission;
import app.androidtoolkit.model.permissions.RuntimePermission;
import app.androidtoolkit.service.ADBService;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Orientation;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextFlow;

import java.util.ArrayList;
import java.util.List;

public class PackagePermissionsController {
    private final AppState appState = AppState.getInstance();
    private final ADBService adb = ADBService.getInstance();

    public ListView<InstallPermission> installPermissionsList;
    public ListView<RuntimePermission> runtimePermissionsList;
    public TextFlow permissionDetails;
    public Label totalInstallPermissionsLabel;
    public Label totalRuntimePermissionsLabel;
    public CheckBox showOnlyGrantedRuntimeCheckbox;
    public VBox container;

    private FilteredList<RuntimePermission> filteredRuntimePermissions;

    public void initialize() {
        setupUI();
        appState.getConnectedDevice().addListener((_, _, newDevice) -> {
            if (newDevice != null) {
                dataLogic();
            }
        });
    }

    public void setupUI() {
        installPermissionsList.setCellFactory(_ -> new ListCell<>() {
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

        runtimePermissionsList.setCellFactory(_ -> new ListCell<>() {
            private final Label nameLabel = new Label();
            private final Label statusLabel = new Label();

            private final Button actionButton = new Button("");
//            private final Button grantButton = new Button("Grant");
//            private final Button revokeButton = new Button("Revoke");

            private final VBox textBox = new VBox(2, nameLabel, statusLabel);
            private final Region spacer = new Region();
//            private final HBox buttonBox = new HBox(5, grantButton, revokeButton);
            private final HBox container = new HBox(10, textBox, spacer, actionButton);

            {
                HBox.setHgrow(spacer, Priority.ALWAYS);
            }

            @Override
            protected void updateItem(RuntimePermission item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    nameLabel.setText(item.shortName());
                    updateStatus(item);
                    setGraphic(container);
                }
            }

            private void updateStatus(RuntimePermission item) {
                boolean granted = item.granted();
                if (granted) {
                    setupRevokeButton();
                } else {
                    setupGrantButton();
                }
                statusLabel.setText(granted ? "Granted" : "Revoked");
                statusLabel.setStyle("-fx-font-style: italic;");
            }

            private void setupGrantButton(){
                actionButton.setText("Grant");
                actionButton.setOnAction(_ -> {
                    RuntimePermission item = getItem();
                    if (item != null) {
                        try {
                            adb.grantPermission(appState.getSelectedPackage().get().getPackageName(), item.fullName(), appState.getSelectedUser().get().id());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        updateStatus(item);
                        appState.forceUpdateSelectedPackage();
                        applyFilters();
                    }
                });
            }

            private void setupRevokeButton(){
                actionButton.setText("Revoke");
                actionButton.setOnAction(_ -> {
                    RuntimePermission item = getItem();
                    if (item != null) {
                        try {
                            adb.revokePermission(appState.getSelectedPackage().get().getPackageName(), item.fullName(), appState.getSelectedUser().get().id());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        updateStatus(item);
                        appState.forceUpdateSelectedPackage();
                        applyFilters();
                    }
                });
            }
        });

        showOnlyGrantedRuntimeCheckbox.selectedProperty().addListener((_, _, _) -> applyFilters());

        appState.getSelectedPackage().addListener((_, _, newValue) -> {
            container.setVisible(newValue != null);
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
