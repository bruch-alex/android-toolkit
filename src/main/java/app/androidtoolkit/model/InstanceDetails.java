package app.androidtoolkit.model;

import lombok.Data;

import java.util.List;

@Data
public class InstanceDetails {
    private boolean installed;
    private boolean hidden;
    // 0 = enabled, 1 = ignored, 2 = denied, 3 = ask
    private int enabledStatusCode;
    private boolean disabled;
    private List<AndroidPermission> runtimePermissions;
}
