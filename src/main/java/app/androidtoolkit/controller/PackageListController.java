package app.androidtoolkit.controller;

import app.androidtoolkit.AppState;
import app.androidtoolkit.model.AndroidUser;
import app.androidtoolkit.model.AppPackage;
import app.androidtoolkit.viewmodel.DeviceView;
import javafx.animation.PauseTransition;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.util.Duration;
import lombok.extern.slf4j.Slf4j;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.fontawesome6.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.Comparator;

import static javafx.scene.input.KeyCombination.CONTROL_DOWN;

@Slf4j
public class PackageListController {
    private final AppState appState = AppState.getInstance();
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
            @Override
            protected void updateItem(AppPackage item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getPackageName());
                    setContextMenu(createContextMenuForPackage(item));
                }
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

    private ContextMenu createContextMenuForPackage(AppPackage appPackage) {
        var contextMenu = new ContextMenu();
        contextMenu.getItems().addAll(
                createCopyMenuItem(),
                createItem("Delete package", FontAwesomeSolid.TRASH, new KeyCodeCombination(KeyCode.X, CONTROL_DOWN)),
                createItem("Disable package", FontAwesomeSolid.STOP, null),
                createItem("Share package details", FontAwesomeSolid.SHARE, null),
                createItem("Export details to pdf", FontAwesomeSolid.FILE_PDF, null)
        );
        return contextMenu;
    }

    private MenuItem createCopyMenuItem() {
        var menuItem = createItem("Copy package name", FontAwesomeSolid.COPY, null);
        menuItem.setOnAction(_ -> {
            var clipboard = Clipboard.getSystemClipboard();
            var content = new ClipboardContent();
            content.putString(appState.getSelectedPackage().get().getPackageName());
            clipboard.setContent(content);
        });
        return menuItem;
    }

    private MenuItem createItem(String text, Ikon graphic, KeyCombination accelerator) {
        var item = new MenuItem(text);
        if (graphic != null) {
            item.setGraphic(new FontIcon(graphic));
        }

        if (accelerator != null) {
            item.setAccelerator(accelerator);
        }
        return item;
    }
}
