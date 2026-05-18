package app.androidtoolkit.utils;

import app.androidtoolkit.AppState;
import app.androidtoolkit.model.AppPackage;
import app.androidtoolkit.service.ADBService;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCombination;
import lombok.extern.slf4j.Slf4j;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.fontawesome6.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

@Slf4j
public final class ContextMenuUtils {
    private final static AppState appState = AppState.getInstance();
    private final static ADBService adb = ADBService.getInstance();

    private ContextMenuUtils() {
    }

    public static ContextMenu createContextMenuForPackage(AppPackage appPackage, Runnable onSuccess) {
        var contextMenu = new ContextMenu();
        contextMenu.getItems().addAll(
                createCopyMenuItem(),
                createDeleteMenuItem(appPackage),
                createChangeEnabledStatusMenuItem(appPackage, onSuccess)
        );
        return contextMenu;
    }

    private static MenuItem createChangeEnabledStatusMenuItem(AppPackage appPackage, Runnable onSuccess) {
        if (appPackage.getInstanceDetailsMap().get(appState.getSelectedUser().get().id()).isEnabled()) {
            return createDisablePackageMenuItem(appPackage, onSuccess);
        } else {
            return createEnablePackageMenuItem(appPackage, onSuccess);
        }
    }

    private static MenuItem createDisablePackageMenuItem(AppPackage appPackage, Runnable onSuccess) {
        var menuItem = createItem("Disable package", FontAwesomeSolid.STOP, null);
        menuItem.setOnAction(_ -> {
            if (DialogUtils.showConfirmationDialog("Disable Package", "Are you sure you want to disable this package?")) {
                try {
                    System.out.println("Disabling package:");
                    adb.setEnabledToDisabled(appPackage.getPackageName(), appState.getSelectedUser().get().id());
                    appPackage.getInstanceDetailsMap()
                            .get(appState.getSelectedUser().get().id())
                            .setEnabled(false);
                    onSuccess.run();
                } catch (Exception e) {
                    log.error("Failed to disable package: {}", appPackage.getPackageName(), e);
                }
            }
        });
        return menuItem;
    }

    private static MenuItem createEnablePackageMenuItem(AppPackage appPackage, Runnable onSuccess) {
        var menuItem = createItem("Enable package", FontAwesomeSolid.ARROW_ALT_CIRCLE_RIGHT, null);
        menuItem.setOnAction(_ -> {
            if (DialogUtils.showConfirmationDialog("Enable Package", "Are you sure you want to enable this package?")) {
                try {
                    System.out.println("Enabling package:");
                    adb.setEnabledToDefaultState(appPackage.getPackageName(), appState.getSelectedUser().get().id());
                    appPackage.getInstanceDetailsMap()
                            .get(appState.getSelectedUser().get().id())
                            .setEnabled(true);
                    onSuccess.run();
                } catch (Exception e) {
                    log.error("Failed to enable package: {}", appPackage.getPackageName(), e);
                }
            }
        });
        return menuItem;
    }

    private static MenuItem createDeleteMenuItem(AppPackage appPackage) {
        var menuItem = createItem("Delete package", FontAwesomeSolid.TRASH, null);
        menuItem.setOnAction(_ -> {
            if (DialogUtils.showConfirmationDialog("Delete Package", "Are you sure you want to delete this package?", true)) {
                try {
                    adb.deleteAppForUser(appPackage.getPackageName(), appState.getSelectedUser().get().id());
                } catch (Exception e) {
                    log.error("Failed to delete package: {}", appPackage.getPackageName(), e);
                }
            }
        });
        return menuItem;
    }

    private static MenuItem createCopyMenuItem() {
        var menuItem = createItem("Copy package name", FontAwesomeSolid.COPY, null);
        menuItem.setOnAction(_ -> {
            var clipboard = Clipboard.getSystemClipboard();
            var content = new ClipboardContent();
            content.putString(appState.getSelectedPackage().get().getPackageName());
            clipboard.setContent(content);
        });
        return menuItem;
    }

    private static MenuItem createItem(String text, Ikon graphic, KeyCombination accelerator) {
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
