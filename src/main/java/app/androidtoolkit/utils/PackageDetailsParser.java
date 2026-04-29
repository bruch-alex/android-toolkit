package app.androidtoolkit.utils;

import app.androidtoolkit.model.AndroidUser;
import app.androidtoolkit.model.AppPackage;
import app.androidtoolkit.model.InstanceDetails;
import app.androidtoolkit.model.PackageDetails;
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

    public static AppPackage getPackageDetails(List<String> lines, List<AndroidUser> users) {
        var appPackage = new AppPackage();
        appPackage.setPackageName(getPackageName(lines));
        var details = parseAndSetPackageInfo(lines);

        details.setQueriesPackages(parseQueriedPackages(lines));
        details.setInstalledPermissions(parseInstallPermissions(lines));
        details.setDeclaredPermissions(parseDeclaredPermissions(lines));
        appPackage.setPackageDetails(details);

        for (var user : users) {
            InstanceDetails instanceDetails = new InstanceDetails();
            parseAndSetUserSpecificDetails(lines, instanceDetails, user.id());
            if (!instanceDetails.isInstalled()) {
                continue;
            }
            instanceDetails.setRuntimePermissions(parseRuntimePermissionsForUser(lines, user.id()));
            appPackage.getInstanceDetailsMap().put(user.id(), instanceDetails);
        }
        return appPackage;
    }

    private static List<InstallPermission> parseInstallPermissions(List<String> details) {
        var inSection = false;
        List<InstallPermission> permissions = new ArrayList<>();
        for (var line : details) {
            if (!inSection && line.equals("install permissions:")) {
                inSection = true;
                continue;
            }
            if (inSection && line.startsWith("User ")) {
                break;
            }
            if (inSection) {
                var parts = line.split(":");
                var nameParts = parts[0].split("\\.");
                var shortName = nameParts[nameParts.length - 1];
                permissions.add(new InstallPermission(parts[0], shortName, parts[1].endsWith("granted")));
            }
        }
        System.out.println("Found " + permissions.size() + " install permissions:");
        return permissions;
    }

    private static List<DeclaredPermission> parseDeclaredPermissions(List<String> details) {
        var inSection = false;
        List<DeclaredPermission> permissions = new ArrayList<>();
        for (var line : details) {
            if (!inSection && line.equals("declared permissions:")) {
                inSection = true;
                continue;
            }
            if (inSection && (line.equals("requested permissions:") || line.equals("install permissions:"))) {
                break;
            }
            if (inSection) {
                var parts = line.split(":");
                var nameParts = parts[0].split("\\.");
                var shortName = nameParts[nameParts.length - 1];
                permissions.add(new DeclaredPermission(parts[0], shortName, parts[1].substring(5)));
            }
        }
        System.out.println("Found " + permissions.size() + " declared permissions:");
        return permissions;
    }

    private static List<RuntimePermission> parseRuntimePermissionsForUser(List<String> details, String userId) {
        var inUserSection = false;
        var inPermissionsSection = false;
        List<RuntimePermission> permissions = new ArrayList<>();
        System.out.println("Parsing declared permissions..." + details.size());
        for (var line : details) {
            if (!inUserSection && line.startsWith("User " + userId)) {
                inUserSection = true;
                continue;
            }
            if (inUserSection && line.equals("runtime permissions:")) {
                inPermissionsSection = true;
                continue;
            }
            if (inPermissionsSection && (line.trim().equals("disabledComponents:") || line.trim().startsWith("User "))) {
                break;
            }
            if (inPermissionsSection) {
                if (line.startsWith("User ") || line.isEmpty() || line.trim().equals("enabledComponents:")) {
                    break;
                }
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

    /**
     * Parses adb output from line Packages: till User 0
     *
     * @param lines
     */
    private static PackageDetails parseAndSetPackageInfo(List<String> lines) {
        var packageDetails = new PackageDetails();
        for (var line : lines) {
            if (line.startsWith("appId=")) {
                packageDetails.setAppId(line.substring("appId=".length()));
            } else if (line.startsWith("versionName=")) {
                System.out.println("Found versionName: " + line);
                packageDetails.setVersionName(line.substring("versionName=".length()));
            } else if (line.startsWith("versionCode=")) {
                System.out.println("Found versionCode: " + line);
                var codeDetails = line.split(" ");
                packageDetails.setVersionCode(codeDetails[0].substring("versionCode=".length()));
                packageDetails.setMinSdkVersion(Integer.parseInt(codeDetails[1].substring("minSdk=".length())));
                packageDetails.setTargetSdkVersion(Integer.parseInt(codeDetails[2].substring("targetSdk=".length())));
            }
        }
        return packageDetails;
    }

    private static String getPackageName(List<String> lines) {
        for (var line : lines) {
            Matcher packageHeaderMatcher = PACKAGE_HEADER_PATTERN.matcher(line);
            if (packageHeaderMatcher.find()) {
                return packageHeaderMatcher.group(1);
            }
        }
        return null;
    }

    private static void parseAndSetUserSpecificDetails(List<String> details, InstanceDetails targetDetails, String userId) {
        for (var line : details) {
            if (line.startsWith("User " + userId)) {
                System.out.println("Found userSpecificDetails: " + line);
                if (line.equals("User " + userId + ":")) {
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

    private static List<String> parseQueriedPackages(List<String> lines) {
        var packages = new ArrayList<String>();
        for (var line : lines) {
            if (line.startsWith("queriesPackages=[")) {
                for (var pkg : line.substring("queriesPackages=[".length()).split(",")) {
                    pkg = pkg.trim().replace("]", "");
                    packages.add(pkg);
                }
                System.out.println("Found queries packages: " + packages.size());
                return packages;
            }
        }
        return packages;
    }
}
