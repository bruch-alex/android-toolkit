package app.androidtoolkit.model.permissions;

/**
 * Represents an installation permission associated with an Android application.
 * <p>
 * Install permissions are configured in the application's manifest
 * and define the access or capabilities granted at the time of app installation.
 *
 * @param fullName The fully qualified name of the installation permission
 *                 (e.g., android.permission.INSTALL_PACKAGES).
 * @param shortName A shorthand representation or alias for the permission name
 *                  (e.g., INSTALL_PACKAGES).
 * @param granted Indicates whether the installation permission is granted (true)
 *                or not (false).
 */
public record InstallPermission(
        String fullName,
        String shortName,
        boolean granted
) {
}
