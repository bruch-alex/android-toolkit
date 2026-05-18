package app.androidtoolkit.controller;

import app.androidtoolkit.AppState;
import app.androidtoolkit.model.AndroidUser;
import app.androidtoolkit.model.AppPackage;
import app.androidtoolkit.service.ADBService;
import app.androidtoolkit.utils.ContextMenuUtils;
import app.androidtoolkit.viewmodel.DeviceView;
import atlantafx.base.theme.Styles;
import javafx.animation.PauseTransition;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;

@Slf4j
public class PackageListController {
    private final AppState appState = AppState.getInstance();
    private final ADBService adb = ADBService.getInstance();
    private final ObservableList<AppPackage> userPackages = FXCollections.observableArrayList();

    public TextField searchField;
    public ListView<AppPackage> packageList;
    public CheckBox hideSystemApps;
    public Label totalInstalledAppsLabel;
    public Label matchedAppsLabel;
    public ComboBox<AndroidUser> selectedUserBox;
    public CheckBox hideDisabledAppsCheckBox;
    PauseTransition delay = new PauseTransition(Duration.millis(300));
    private FilteredList<AppPackage> filteredApps;

    public void initialize() {
        setupUI();
        appState.getConnectedDevice().addListener((_, _, newValue) -> {
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

        appState.selectNewUser(null);
    }

    private void onDeviceConnect(DeviceView device) {
        assert device != null;

        selectedUserBox.getItems().setAll(device.getUsers());
        log.debug("Users: {}", selectedUserBox.getItems());
        selectedUserBox.getSelectionModel().selectFirst();
        log.debug("Selected user: {}", selectedUserBox.getSelectionModel().getSelectedItem());

        filteredApps = new FilteredList<>(userPackages, _ -> true);
        SortedList<AppPackage> sortedApps = new SortedList<>(filteredApps);

        filteredApps.addListener((ListChangeListener<AppPackage>) _ -> {
            matchedAppsLabel.setText(String.valueOf(filteredApps.size()));
        });

        sortedApps.setComparator(Comparator.comparing(
                AppPackage::getPackageName, String.CASE_INSENSITIVE_ORDER
        ));

        packageList.setItems(sortedApps);


        observePackages(device);

        selectedUserBox.setOnAction(_ -> refreshUserPackages(device));

        searchField.textProperty().addListener((_, _, _) -> applyFilters());
        hideSystemApps.selectedProperty().addListener((_, _, _) -> applyFilters());
        hideDisabledAppsCheckBox.selectedProperty().addListener((_, _, _) -> applyFilters());
        packageList.getSelectionModel().selectedItemProperty().addListener((_, _, newValue) -> {
            delay.stop();
            delay.setOnFinished(_ -> {
                if (newValue != null) {
                    appState.getSelectedPackage().set(newValue);
                    System.out.println("Selected package: " + newValue.getPackageName());
                }
            });
            delay.playFromStart();
        });
    }

    private void observePackages(DeviceView device) {
        PauseTransition debounce = new PauseTransition(Duration.millis(100));
        debounce.setOnFinished(_ -> refreshUserPackages(device));

        device.getPackages().addListener((MapChangeListener<String, AppPackage>) _ -> {
            debounce.playFromStart();
        });
    }

    private void setupUI() {
        packageList.setCellFactory(_ -> new ListCell<>() {
            private final Label packageNameLabel = new Label();
            private final Label systemLabel = new Label();
            private final Label enabledLabel = new Label();
            private final HBox tags = new HBox(2, systemLabel, enabledLabel);
            private final VBox container = new VBox(packageNameLabel, tags);

            {
                packageNameLabel.getStyleClass().addAll(Styles.TEXT, Styles.TEXT_BOLD);
                systemLabel.getStyleClass().addAll(Styles.TEXT, Styles.TEXT_ITALIC);
                enabledLabel.getStyleClass().addAll(Styles.TEXT, Styles.TEXT_ITALIC);
            }

            @Override
            protected void updateItem(AppPackage item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setGraphic(null);
                    setContextMenu(null);
                } else {
                    updateContent(item);
                    setGraphic(container);
                    setContextMenu(ContextMenuUtils.createContextMenuForPackage(item, () -> packageList.refresh()));
                }
            }

            private void updateContent(AppPackage item) {
                final boolean isSystemApp = item.isSystemApp();
                final boolean isEnabled = item.getInstanceDetailsMap()
                        .get(appState.getSelectedUser().get().id())
                        .isEnabled();

                packageNameLabel.setText(item.getPackageName());

                systemLabel.setText(isSystemApp ? "System" : "");
                systemLabel.setVisible(isSystemApp);
                systemLabel.getStyleClass().removeAll(Styles.DANGER, Styles.TEXT_MUTED);
                systemLabel.getStyleClass().add(isSystemApp ? Styles.DANGER : Styles.TEXT_MUTED);

                enabledLabel.setText(isEnabled ? "Enabled" : "Disabled");
                enabledLabel.getStyleClass().removeAll(Styles.SUCCESS, Styles.TEXT_MUTED);
                enabledLabel.getStyleClass().add(isEnabled ? Styles.SUCCESS : Styles.TEXT_MUTED);
            }
        });

        selectedUserBox.setCellFactory(_ -> new ListCell<>() {
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
        appState.selectNewUser(selectedUser);

        if (device == null || selectedUser == null) {
            userPackages.clear();
            return;
        }
        var devicePackages = device.getPackages().values().stream()
                .filter(pkg -> pkg.getInstanceDetailsMap().containsKey(selectedUser.id()))
                .toList();
        log.debug("User packages size: {}", devicePackages.size());
        userPackages.setAll(devicePackages);
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
            var matchesDisabledFilter = !hideDisabledAppsCheckBox.isSelected() || app.getInstanceDetailsMap().get(appState.getSelectedUser().get().id()).isEnabled();
            return matchesSearch && matchesSystemFilter && matchesDisabledFilter;
        });
    }
}
