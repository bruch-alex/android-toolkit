package app.androidtoolkit;

import app.androidtoolkit.model.AndroidUser;
import app.androidtoolkit.model.AppPackage;
import app.androidtoolkit.model.device.AndroidDeviceRecord;
import app.androidtoolkit.viewmodel.DeviceView;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Setter
@Slf4j
public class AppState {
    private static final AppState INSTANCE = new AppState();

    private final ObjectProperty<DeviceView> selectedDevice = new SimpleObjectProperty<>(new DeviceView());
    private final ObjectProperty<AndroidUser> selectedUser = new SimpleObjectProperty<>();
    private final ObjectProperty<AppPackage> selectedPackage = new SimpleObjectProperty<>();

    /**
     * A map of currently connected ADB devices.
     *
     * <p>Backed by an {@link javafx.collections.ObservableMap} so UI components
     * can listen for device connect/disconnect events via a
     * {@link javafx.collections.MapChangeListener}.
     *
     * <br>
     * <p><b>Key:</b> Device serial number (e.g. {@code A1B2C3DEFG})
     * <br><b>Value:</b> Device model name (e.g. {@code Pixel 9a})
     */
    private final ObservableList<AndroidDeviceRecord> connectedDevices = FXCollections.observableArrayList();

    private AppState() {
    }

    public static AppState getInstance() {
        return INSTANCE;
    }

    public void deviceDisconnected() {
        selectedDevice.set(null);
        selectedUser.set(null);
        selectedPackage.set(null);
    }

    public void selectNewUser(AndroidUser user) {
        selectedPackage.set(null);
        selectedUser.set(user);
    }

    public void forceUpdateSelectedPackage() {
        var pkg = selectedPackage.get();
        selectedPackage.set(null);
        selectedPackage.set(pkg);
    }

    public void addConnectedDevice(AndroidDeviceRecord device) {
        for (var d : connectedDevices) {
            if (d.serial().equals(device.serial())) {
                log.debug("Device already connected: old: {}, new: {}", d, device);
                return;
            }
        }
        log.debug("Adding new device: {}", device);
        connectedDevices.add(device);
    }

    public void updateConnectedDevice(AndroidDeviceRecord device) {
        for (var d : connectedDevices) {
            if (d.serial().equals(device.serial())) {
                log.debug("Updating device: {}", device);
                connectedDevices.set(connectedDevices.indexOf(d), device);
                return;
            }
        }
    }
}
