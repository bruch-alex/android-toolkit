package app.androidtoolkit.model;

import app.androidtoolkit.model.permissions.RuntimePermission;
import lombok.Data;

import java.util.List;

@Data
public class InstanceDetails {
    private int userId;
    private boolean installed;
    private boolean hidden;
    private boolean suspended;
    private boolean stopped;
    private boolean notLaunched;
    // 0 = enabled, 1 = ignored, 2 = denied, 3 = ask
    private int enabledStatusCode;
    private boolean disabled;
    private List<RuntimePermission> runtimePermissions;
}
