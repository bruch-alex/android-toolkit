package app.androidtoolkit.model.permissions;

public record DeclaredPermission(
        String fullName,
        String shortName,
        String protectionLevel
) {
}