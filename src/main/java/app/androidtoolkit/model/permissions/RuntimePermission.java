package app.androidtoolkit.model.permissions;

/**
 * Represents a runtime permission associated with an Android application.
 * <p>
 * A runtime permission is a type of permission that an app must explicitly
 * request and obtain from the user at runtime rather than being granted during installation.
 *
 * @param fullName:  The fully qualified name of the permission (e.g., com.android.permission.CAMERA)
 * @param shortName: A shorter representation of the permission name (e.g., CAMERA).
 * @param granted:   Indicates if the permission has been granted or not.
 * @param flags:     Additional attributes or metadata related to the permission.
 */
public record RuntimePermission(
        String fullName,
        String shortName,
        boolean granted,
        String flags
) {
}
