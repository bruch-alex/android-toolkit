package app.androidtoolkit.model.permissions;

/**
 * Represents a declared permission associated with an Android application.
 * <p>
 * A declared permission is a type of permission that an application defines
 * in its manifest file to specify the level of access it requires or enforces
 * on other applications or system components.
 *
 * @param fullName The fully qualified name of the permission
 *                 (e.g., android.permission.ACCESS_FINE_LOCATION).
 * @param shortName A concise name or alias for the permission
 *                  (e.g., ACCESS_FINE_LOCATION).
 * @param protectionLevel The protection level of the permission defining how
 *                        it is granted, such as normal, dangerous, signature, etc.
 */
public record DeclaredPermission(
        String fullName,
        String shortName,
        String protectionLevel
) {
}