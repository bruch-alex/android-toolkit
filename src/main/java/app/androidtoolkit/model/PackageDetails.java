package app.androidtoolkit.model;

import app.androidtoolkit.model.permissions.DeclaredPermission;
import app.androidtoolkit.model.permissions.InstallPermission;
import lombok.Data;

import java.util.List;

@Data
public class PackageDetails {
    private String appId;

    private String versionCode;
    private int minSdkVersion;
    private int targetSdkVersion;
    private String versionName;
    private List<String> queriesPackages;
    private List<DeclaredPermission> declaredPermissions;
    private List<String> requestedPermissions;
    private List<InstallPermission> installedPermissions;

    public PackageDetails() {
        this.installedPermissions = List.of();
        this.declaredPermissions = List.of();
        this.requestedPermissions = List.of();
        this.queriesPackages = List.of();
    }
}
