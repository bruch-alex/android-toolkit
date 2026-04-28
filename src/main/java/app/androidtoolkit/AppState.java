package app.androidtoolkit;

//import app.androidtoolkit.model.permissions.AndroidPermission;

import app.androidtoolkit.model.AndroidUser;
import app.androidtoolkit.model.AppPackage;
import app.androidtoolkit.viewmodel.DeviceView;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AppState {
    private static final AppState INSTANCE = new AppState();

    private final ObjectProperty<DeviceView> connectedDevice = new SimpleObjectProperty<>(new DeviceView());
    private final ObjectProperty<AndroidUser> selectedUser = new SimpleObjectProperty<>();
    private final ObjectProperty<AppPackage> selectedPackage = new SimpleObjectProperty<>();

    private AppState() {
    }

    public static AppState getInstance() {
        return INSTANCE;
    }

    public void deviceDisconnected() {
        connectedDevice.set(null);
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
}
