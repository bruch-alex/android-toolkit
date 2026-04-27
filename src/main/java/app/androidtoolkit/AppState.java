package app.androidtoolkit;

import app.androidtoolkit.model.AndroidPermission;
import app.androidtoolkit.model.AppPackage;
import app.androidtoolkit.viewmodel.DeviceView;
import app.androidtoolkit.viewmodel.UserView;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AppState {
    private static final AppState INSTANCE = new AppState();

    private final ObjectProperty<DeviceView> connectedDevice = new SimpleObjectProperty<>(new DeviceView());
    private final ObjectProperty<UserView> selectedUser = new SimpleObjectProperty<>();
    private final ObjectProperty<AppPackage> selectedPackage = new SimpleObjectProperty<>();
    private final ObjectProperty<AndroidPermission> selectedPermission = new SimpleObjectProperty<>();

    private AppState() {
    }

    public static AppState getInstance() {
        return INSTANCE;
    }

    public void deviceDisconnected() {
        connectedDevice.set(null);
        selectedUser.set(null);
        selectedPackage.set(null);
        selectedPermission.set(null);
    }

}
