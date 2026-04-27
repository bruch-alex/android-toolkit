package app.androidtoolkit.controller;

import app.androidtoolkit.AppState;
import app.androidtoolkit.model.AndroidUser;
import app.androidtoolkit.model.AppPackage;
import app.androidtoolkit.viewmodel.DeviceView;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.scene.control.*;

import java.util.Comparator;
import java.util.List;

public class PackageListController {
    private final AppState appState = AppState.getInstance();
    private final ObservableList<AppPackage> userPackages = FXCollections.observableArrayList();
    public TextField searchField;
    public ListView<AppPackage> packageList;
    public CheckBox hideSystemApps;
    public Label totalInstalledAppsLabel;
    public Label matchedAppsLabel;
    public ComboBox<AndroidUser> selectedUserBox;
    private FilteredList<AppPackage> filteredApps;

    public void initialize() {
        setupUI();
        appState.getConnectedDevice().addListener((observableValue, deviceView, newValue) -> {
            if (newValue != null) {
                onDeviceConnect(newValue);
                applyFilters();
            } else {
                onDeviceDisconnect();
            }
        });
    }

    private void onDeviceDisconnect() {
        selectedUserBox.getItems().clear();
        selectedUserBox.getSelectionModel().clearSelection();

        userPackages.clear();
        totalInstalledAppsLabel.setText("0");
        matchedAppsLabel.setText("0");

        appState.getSelectedUser().set(null);
        appState.getSelectedPackage().set(null);
        appState.getSelectedPermission().set(null);
    }

    private void onDeviceConnect(DeviceView device) {
        selectedUserBox.getItems().setAll(device != null ? device.getUsers() : List.of());
        selectedUserBox.getSelectionModel().selectFirst();

        filteredApps = new FilteredList<>(userPackages, _ -> true);
        SortedList<AppPackage> sortedApps = new SortedList<>(filteredApps);

        filteredApps.addListener((ListChangeListener<AppPackage>) _ -> {
            matchedAppsLabel.setText(String.valueOf(filteredApps.size()));
        });

        sortedApps.setComparator(Comparator.comparing(
                AppPackage::getPackageName, String.CASE_INSENSITIVE_ORDER
        ));

        packageList.setItems(sortedApps);
        device.getPackages().addListener((MapChangeListener<String, AppPackage>) change -> {
            refreshUserPackages(device);
        });
        selectedUserBox.setOnAction(_ -> refreshUserPackages(device));

        searchField.textProperty().addListener((observable, oldValue, newValue) -> applyFilters());
        hideSystemApps.selectedProperty().addListener((observable, oldValue, newValue) -> applyFilters());
    }

    private void setupUI() {
        packageList.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(AppPackage item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getPackageName());
                }
            }
        });
        selectedUserBox.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(AndroidUser user, boolean empty) {
                super.updateItem(user, empty);
                if (empty || user == null) {
                    setText(null);
                } else {
                    setText(user.name() + " (" + user.id() + ")");
                }
            }
        });

        selectedUserBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(AndroidUser user, boolean empty) {
                super.updateItem(user, empty);
                if (empty || user == null) {
                    setText("Select user");
                } else {
                    setText(user.name() + " (" + user.id() + ")");
                }
            }
        });
    }


    private void refreshUserPackages(DeviceView device) {
        var selectedUser = selectedUserBox.getSelectionModel().getSelectedItem();

        if (device == null || selectedUser == null) {
            userPackages.clear();
            return;
        }
        userPackages.setAll(
                device.getPackages().values().stream()
                        .filter(pkg -> pkg.getInstanceDetailsMap().containsKey(selectedUser.id()))
                        .toList()
        );
        totalInstalledAppsLabel.setText(String.valueOf(device.getPackages().size()));
        applyFilters();
    }

    private void applyFilters() {
        String query = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        appState.getSelectedPackage().set(null);

        filteredApps.setPredicate(app -> {
            if (app == null) {
                return false;
            }

            var matchesSearch = query.isEmpty()
                    || (app.getPackageName() != null
                    && app.getPackageName().toLowerCase().contains(query));

            var matchesSystemFilter = !hideSystemApps.isSelected() || !app.isSystemApp();
            return matchesSearch && matchesSystemFilter;
        });
    }
}
