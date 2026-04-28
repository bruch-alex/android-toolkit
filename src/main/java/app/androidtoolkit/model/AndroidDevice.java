package app.androidtoolkit.model;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private final Map<String, AppPackage> packages = new HashMap<>();
}
