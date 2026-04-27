package app.androidtoolkit.model;

import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class AndroidDevice {
    private final String model;
    private final String manufacturer;
    private final String state;
    private final String androidVersion;
    @Builder.Default
    private final List<AndroidUser> users = new ArrayList<>();
    @Builder.Default
    private final ObservableMap<String, AppPackage> packageRegistry = FXCollections.observableHashMap();
}
