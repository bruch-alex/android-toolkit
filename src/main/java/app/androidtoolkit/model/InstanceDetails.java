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
    // 0 = enabled, 1 = ignored, 2 = denied, 3 = disabled (ask user)
    private int enabledStatusCode;
    private boolean enabled;
    private List<RuntimePermission> runtimePermissions;

    public void setEnabledStatusCode(int enabledStatusCode) {
        switch (enabledStatusCode) {
            case 0, 1:
                this.enabled = true;
                break;
            case 2, 3:
                this.enabled = false;
                break;
        }
    }
}
