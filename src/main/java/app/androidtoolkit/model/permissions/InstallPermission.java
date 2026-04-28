package app.androidtoolkit.model.permissions;

public record InstallPermission(
        String fullName,
        String shortName,
        boolean granted
) {
}
