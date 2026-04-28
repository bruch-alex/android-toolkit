package app.androidtoolkit.mapper;

import app.androidtoolkit.model.AndroidDevice;
import app.androidtoolkit.viewmodel.DeviceView;
import com.android.ddmlib.IDevice;

public abstract class AndroidDeviceMapper {
    public static AndroidDevice toModel(IDevice d) {
        return AndroidDevice.builder()
                .model(d.getProperty("ro.product.model"))
                .state(d.getState().toString())
                .manufacturer(d.getProperty("ro.product.manufacturer"))
                .androidVersion(d.getProperty("ro.build.version.release"))
                .build();
    }

    public static DeviceView toView(AndroidDevice device) {
        DeviceView view = new DeviceView();
        view.modelProperty().set(device.getModel());
        view.androidVersionProperty().set(device.getAndroidVersion());
        view.manufacturerProperty().set(device.getManufacturer());
        view.stateProperty().set(device.getState());
        view.getUsers().addAll(device.getUsers());
        view.getPackages().putAll(device.getPackages());
        return view;
    }
}