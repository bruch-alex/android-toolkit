package app.androidtoolkit.model;

import lombok.*;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@EqualsAndHashCode
@ToString
public class AppPackage {
    private String packageName;
    private boolean systemApp;
    private PackageDetails packageDetails;
    private Map<String, InstanceDetails> instanceDetailsMap; // userId -> InstanceDetails
    public AppPackage() {
        this.instanceDetailsMap = new HashMap<>();
        this.packageDetails = new PackageDetails();
    }

    public AppPackage(String packageName) {
        this();
        this.packageName = packageName;
    }

    public InstanceDetails getOrCreateInstanceDetails(String userId) {
        return instanceDetailsMap.computeIfAbsent(userId, _ -> new InstanceDetails());
    }

    public boolean merge(AppPackage source) {
        if (!this.getPackageName().equals(source.getPackageName())) {
            System.out.println("Name mismatch: " + this.getPackageName() + " != " + source.getPackageName());
            return false;
        }
        this.systemApp = source.isSystemApp();
        this.packageDetails = source.getPackageDetails();
        this.instanceDetailsMap = source.getInstanceDetailsMap();
        return true;
    }
}
