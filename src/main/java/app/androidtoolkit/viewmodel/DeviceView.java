package app.androidtoolkit.viewmodel;

import app.androidtoolkit.model.AndroidDevice;
import app.androidtoolkit.model.AndroidUser;
import app.androidtoolkit.model.AppPackage;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

@ToString
@AllArgsConstructor
@EqualsAndHashCode
public class DeviceView {
    private final AndroidDevice device;

    private final StringProperty model;
    private final StringProperty manufacturer;
    private final StringProperty state;
    private final StringProperty androidVersion;
    @Getter
    private final ObservableList<AndroidUser> users;

    @Getter
    private final ObservableMap<String, AppPackage> packages; // package name -> AppPackage

    public DeviceView() {
        this.device = null;
        this.model = new SimpleStringProperty("");
        this.manufacturer = new SimpleStringProperty("");
        this.state = new SimpleStringProperty("");
        this.androidVersion = new SimpleStringProperty("");
        this.users = FXCollections.observableArrayList(List.of());
        this.packages = FXCollections.observableHashMap();
    }

    public StringProperty modelProperty() {
        return model;
    }

    public String getModel() {
        return model.get();
    }

    public void setModel(String model) {
        this.model.set(model);
    }

    public StringProperty manufacturerProperty() {
        return manufacturer;
    }

    public String getManufacturer() {
        return manufacturer.get();
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer.set(manufacturer);
    }

    public StringProperty stateProperty() {
        return state;
    }

    public String getState() {
        return state.get();
    }

    public void setState(String state) {
        this.state.set(state);
    }

    public StringProperty androidVersionProperty() {
        return androidVersion;
    }

    public String getAndroidVersion() {
        return androidVersion.get();
    }

    public void setAndroidVersion(String androidVersion) {
        this.androidVersion.set(androidVersion);
    }

}
