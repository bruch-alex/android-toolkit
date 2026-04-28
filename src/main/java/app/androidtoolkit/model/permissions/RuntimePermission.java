package app.androidtoolkit.model.permissions;

public record RuntimePermission(
        String fullName,
        String shortName,
        boolean granted,
        String flags
) {
}
