package app.androidtoolkit.model;

import app.androidtoolkit.model.permissions.RuntimePermission;
import lombok.Data;

import java.util.List;

/**
 * Represents detailed information about an application instance for a specific user.
 * <p>
 * This class is used in association with an application package to manage per-user
 * and per-instance details for installed applications.
 */
@Data
public class InstanceDetails {
    private int userId;
    private boolean installed;
    private boolean hidden;
    private boolean suspended;
    private boolean stopped;
    private boolean notLaunched;
    private int enabledStatusCode;
    private boolean enabled;
    private List<RuntimePermission> runtimePermissions;


    /**
     * Sets the enabled status code for the instance and determines whether the instance
     * is enabled based on the given status code.
     * Status codes from 0 or 1 will mark the instance as enabled, while 2 or 3 will disable it.
     *
     * @param enabledStatusCode the status code representing the enabled state of the instance.
     *                          Accepted values:
     *                          - 0: enabled
     *                          - 1: ignored (treated as enabled)
     *                          - 2: denied (treated as disabled)
     *                          - 3: disabled (ask user)
     */
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
