package app.androidtoolkit.model;

import app.androidtoolkit.model.permissions.DeclaredPermission;
import app.androidtoolkit.model.permissions.InstallPermission;
import lombok.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@EqualsAndHashCode
@ToString
public class AppPackage {
    private String packageName;
    private boolean systemApp;
    private List<String> queriesPackages;
    private List<DeclaredPermission> declaredPermissions;
    private List<String> requestedPermissions;
    private List<InstallPermission> installedPermissions;
    private Map<String, InstanceDetails> instanceDetailsMap; // userId -> InstanceDetails

    public AppPackage(String packageName) {
        this.packageName = packageName;
        this.instanceDetailsMap = new HashMap<>();
        this.declaredPermissions = List.of();
        this.requestedPermissions = List.of();
        this.installedPermissions = List.of();
    }

    public AppPackage() {
        this.instanceDetailsMap = new HashMap<>();
        this.declaredPermissions = List.of();
        this.requestedPermissions = List.of();
        this.installedPermissions = List.of();
    }

    public InstanceDetails getOrCreateInstanceDetails(String userId) {
        return instanceDetailsMap.computeIfAbsent(userId, _ -> new InstanceDetails());
    }

    public boolean merge(AppPackage target) {
        if (!this.packageName.equals(target.packageName)) {
            return false;
        }
        this.declaredPermissions = target.getDeclaredPermissions();
        this.requestedPermissions = target.getRequestedPermissions();
        this.installedPermissions = target.getInstalledPermissions();
        this.instanceDetailsMap = target.getInstanceDetailsMap();
        return true;
    }
}
