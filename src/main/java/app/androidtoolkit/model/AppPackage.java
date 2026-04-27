package app.androidtoolkit.model;

import lombok.Data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class AppPackage {
    private String packageName;
    private boolean systemApp;
    private List<String> queriesPackages;
    private List<AndroidPermission> declaredPermissions;
    private List<AndroidPermission> requestedPermissions;
    private List<AndroidPermission> installedPermissions;
    private Map<String, InstanceDetails> instanceDetailsMap;

    public AppPackage(String packageName) {
        this.packageName = packageName;
        this.instanceDetailsMap = new HashMap<>();
    }

    public InstanceDetails getOrCreateInstanceDetails(String userId) {
        return instanceDetailsMap.computeIfAbsent(userId, _ -> new InstanceDetails());
    }
}
