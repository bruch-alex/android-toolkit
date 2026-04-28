package app.androidtoolkit.utils;

import app.androidtoolkit.model.AndroidUser;
import app.androidtoolkit.model.AppPackage;
import app.androidtoolkit.model.InstanceDetails;
import app.androidtoolkit.model.permissions.DeclaredPermission;
import app.androidtoolkit.model.permissions.InstallPermission;
import app.androidtoolkit.model.permissions.RuntimePermission;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PackageDetailsParser {

    private static final Pattern PACKAGE_HEADER_PATTERN = Pattern.compile("Package \\[([^]]+)]");

    private PackageDetailsParser() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static AppPackage parsePackageDetails(List<String> lines, List<AndroidUser> users) {
        var appPackage = new AppPackage();
        parseAndSetPackageInfo(lines, appPackage);
        appPackage.setInstalledPermissions(parseInstallPermissions(lines));
        appPackage.setDeclaredPermissions(parseDeclaredPermissions(lines));
        for (var user : users) {
            InstanceDetails instanceDetails = new InstanceDetails();
            parseAndSetUserSpecificDetails(lines, instanceDetails, user.id());
            instanceDetails.setRuntimePermissions(parseRuntimePermissionsForUser(lines, user.id()));
            if (instanceDetails.isInstalled()){
                appPackage.getInstanceDetailsMap().put(user.id(), instanceDetails);
            }
        }
        return appPackage;
    }

    public static List<InstallPermission> parseInstallPermissions(List<String> details) {
        var inSection = false;
        List<InstallPermission> permissions = new ArrayList<>();
        System.out.println("Parsing install permissions..." + details.size());
        for (var line : details) {
            if (!inSection && line.equals("install permissions:")) {
                System.out.println("Parsing install permissions...");
                inSection = true;
                continue;
            }
            if (inSection && line.startsWith("User ")) {
                System.out.println("Found user section");
                break;
            }
            if (inSection) {
                System.out.println("Parsing permission: " + line);
                var parts = line.split(":");
                var nameParts = parts[0].split("\\.");
                var shortName = nameParts[nameParts.length - 1];
                permissions.add(new InstallPermission(parts[0], shortName, parts[1].endsWith("granted")));
            }
        }
        System.out.println("Found " + permissions.size() + " install permissions:");
        return permissions;
    }

    public static List<DeclaredPermission> parseDeclaredPermissions(List<String> details) {
        var inSection = false;
        List<DeclaredPermission> permissions = new ArrayList<>();
        System.out.println("Parsing declared permissions..." + details.size());
        for (var line : details) {
            if (!inSection && line.equals("declared permissions:")) {
                System.out.println("entering section...");
                inSection = true;
                continue;
            }
            if (inSection && (line.equals("requested permissions:") || line.equals("install permissions:"))) {
                System.out.println("existing section");
                break;
            }
            if (inSection) {
                System.out.println("Parsing declared permission: " + line);
                var parts = line.split(":");
                var nameParts = parts[0].split("\\.");
                var shortName = nameParts[nameParts.length - 1];
                permissions.add(new DeclaredPermission(parts[0], shortName, parts[1].substring(5)));
            }
        }
        System.out.println("Found " + permissions.size() + " declared permissions:");
        return permissions;
    }

    public static List<RuntimePermission> parseRuntimePermissionsForUser(List<String> details, String userId) {
        var inUserSection = false;
        var inPermissionsSection = false;
        List<RuntimePermission> permissions = new ArrayList<>();
        System.out.println("Parsing declared permissions..." + details.size());
        for (var line : details) {
            if (!inUserSection && line.startsWith("User " + userId)) {
                System.out.println("Found user section");
                inUserSection = true;
                continue;
            }
            if (inUserSection && line.equals("runtime permissions:")) {
                System.out.println("entering section...");
                inPermissionsSection = true;
                continue;
            }
            if (inPermissionsSection && (line.trim().equals("disabledComponents:") || line.trim().startsWith("User "))) {
                System.out.println("existing section");
                break;
            }
            if (inPermissionsSection) {
                if (line.startsWith("User ") || line.isEmpty() || line.trim().equals("enabledComponents:")){
                    System.out.println("Exiting");
                    break;
                }
                System.out.println("Parsing permission: " + line);
                var parts = line.split(":");
                var nameParts = parts[0].split("\\.");
                var shortName = nameParts[nameParts.length - 1];

                boolean granted = parts[1].split(",")[0].trim().equals("granted=true");
                String flags = parts[1].trim().substring(8);
                permissions.add(new RuntimePermission(parts[0], shortName, granted, flags));
            }
        }
        return permissions;
    }

    public static void parseAndSetPackageInfo(List<String> details, AppPackage target) {
        for (var line : details) {
            Matcher packageHeaderMatcher = PACKAGE_HEADER_PATTERN.matcher(line);
            if (packageHeaderMatcher.find()) {
                target.setPackageName(packageHeaderMatcher.group(1));
                break; // Stop parsing once we find the package header for now (Change later)
            }
        }
    }

    private static void parseAndSetUserSpecificDetails(List<String> details, InstanceDetails targetDetails, String userId) {
        for (var line : details) {
            if (line.startsWith("User " + userId)) {
                System.out.println("Found userSpecificDetails: " + line);
                if (line.equals("User " + userId + ":")){
                    break;
                }
                var parts = line.split(":")[1].split(" ");
                for (var part : parts) {
                    if (part.startsWith("installed=")) {
                        targetDetails.setInstalled(Boolean.parseBoolean(part.substring("installed=".length())));
                    } else if (part.startsWith("hidden=")) {
                        targetDetails.setHidden(Boolean.parseBoolean(part.substring("hidden=".length())));
                    } else if (part.startsWith("suspended=")) {
                        targetDetails.setSuspended(Boolean.parseBoolean(part.substring("suspended=".length())));
                    } else if (part.startsWith("stopped=")) {
                        targetDetails.setStopped(Boolean.parseBoolean(part.substring("stopped=".length())));
                    } else if (part.startsWith("notLaunched=")) {
                        targetDetails.setNotLaunched(Boolean.parseBoolean(part.substring("notLaunched=".length())));
                    } else if (part.startsWith("enabled=")) {
                        targetDetails.setEnabledStatusCode(Integer.parseInt(part.substring("enabled=".length())));
                    }
                }
            } else if (line.equals("Queries:")) {
                break;
            }
        }
    }
}
