package app.androidtoolkit.service;

import app.androidtoolkit.AppState;
import app.androidtoolkit.mapper.AndroidDeviceMapper;
import app.androidtoolkit.model.AndroidDevice;
import app.androidtoolkit.model.AndroidUser;
import app.androidtoolkit.model.AppPackage;
import app.androidtoolkit.utils.PackageDetailsParser;
import com.android.ddmlib.*;
import javafx.application.Platform;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class ADBService {
    private final static ADBService INSTANCE = new ADBService();
    private final AppState appState = AppState.getInstance();
    private final AtomicBoolean refreshRunning = new AtomicBoolean(false);
    private IDevice connectedIDevice = null;

    public static ADBService getInstance() {
        return INSTANCE;
    }

    public static Optional<String> findAdbPath() {
        List<Path> paths = Stream.of(
                envPath("ANDROID_SDK_ROOT"),
                envPath("ANDROID_HOME"),
                Path.of(System.getProperty("user.home"), "Android", "Sdk"),
                Path.of(System.getProperty("user.home"), "android-sdk"),
                Path.of("/usr/lib/android-sdk")
        ).filter(Objects::nonNull).toList();

        for (Path sdkRoot : paths) {
            Path adb = sdkRoot.resolve("platform-tools").resolve(isWindows() ? "adb.exe" : "adb");
            if (Files.isRegularFile(adb) && Files.isExecutable(adb)) {
                return Optional.of(adb.toString());
            }
        }
        return Optional.empty();
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    private static Path envPath(String name) {
        var value = System.getenv(name);
        return (value == null || value.isBlank()) ? null : Path.of(value);
    }

    public boolean isAdbInstalled() {
        return findAdbPath().isPresent();
    }

    public void start() {
        System.out.println("Starting ADB service...");
        var adbPath = findAdbPath().orElse("");
        AndroidDebugBridge.init(false);
        AndroidDebugBridge bridge = AndroidDebugBridge.createBridge(adbPath, false, 5000, TimeUnit.MILLISECONDS);
        AndroidDebugBridge.addDeviceChangeListener(new AndroidDebugBridge.IDeviceChangeListener() {
            @Override
            public void deviceConnected(IDevice device) {
                System.out.println("Device connected: " + device);
                refreshDevices(bridge);
            }

            @Override
            public void deviceDisconnected(IDevice device) {
                System.out.println("Device disconnected: " + device);
                connectedIDevice = null;
                Platform.runLater(appState::deviceDisconnected);
                refreshDevices(bridge);
            }

            @Override
            public void deviceChanged(IDevice device, int changeMask) {
                refreshDevices(bridge);
            }
        });
        refreshDevices(bridge);

        appState.getSelectedPackage().addListener((_, _, newValue) -> {
            if (newValue != null) {
                try {
                    System.out.println("Scanning package info for: " + newValue.getPackageName());
                    scanPackageInfoAndUpdateModel(newValue);
                    System.out.println("Package info scanned: " + newValue.getInstalledPermissions().size());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void refreshDevices(AndroidDebugBridge bridge) {
        AndroidDevice newDevice = null;
        if (bridge != null) {
            for (IDevice d : bridge.getDevices()) {
                if (d != null && d.isOnline()) {
                    connectedIDevice = d;
                    newDevice = AndroidDeviceMapper.toModel(d);
                    try {
                        var users = getUsers(d);
                        newDevice.getUsers().addAll(users);
                        scanAllAppsAsync(users);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                }
            }
        }
        var finalDevice = newDevice;
        Platform.runLater(() -> appState.getConnectedDevice().set(finalDevice == null ? null : AndroidDeviceMapper.toView(finalDevice)));
    }

    private List<AndroidUser> getUsers(IDevice device) throws ShellCommandUnresponsiveException, AdbCommandRejectedException, IOException, TimeoutException {
        List<AndroidUser> tempUsers = new ArrayList<>();
        Pattern pattern = Pattern.compile("\\{([^}]*)\\}");
        device.executeShellCommand("pm list users", new MultiLineReceiver() {

            @Override
            public boolean isCancelled() {
                return false;
            }

            @Override
            public void processNewLines(String[] lines) {
                for (var line : lines) {
                    Matcher matcher = pattern.matcher(line);
                    if (matcher.find()) {
                        line = matcher.group(1);
                        String[] parts = line.split(":");
                        AndroidUser user = new AndroidUser(parts[0], parts[1]);
                        tempUsers.add(user);
                    }
                }
            }
        });
        System.out.println("Found users: " + tempUsers.size());
        return tempUsers;
    }

    private void scanAllAppsAsync(List<AndroidUser> users) {
        Thread.startVirtualThread(() -> {
            Map<String, AppPackage> scannedPackages = scanAllAppsToMap(users);

            Platform.runLater(() -> {
                appState.getConnectedDevice().get().getPackages().clear();
                System.out.println("Scanned packages: " + scannedPackages.size());
                appState.getConnectedDevice().get().getPackages().putAll(scannedPackages);
            });
        });
    }

    private Map<String, AppPackage> scanAllAppsToMap(List<AndroidUser> users) {
        System.out.println("Scanning all apps...");
        Map<String, AppPackage> scannedPackages = new HashMap<>();
        try {
            for (AndroidUser user : users) {

                var systemAps = scanAppsForUser(connectedIDevice, user, "-s");
                var thirdPartyAps = scanAppsForUser(connectedIDevice, user, "-3");
                var disabledAps = scanAppsForUser(connectedIDevice, user, "-d");

                for (String packageName : systemAps) {
                    var appPackage = scannedPackages.computeIfAbsent(packageName, AppPackage::new);
                    appPackage.setSystemApp(true);

                    var instanceDetails = appPackage.getOrCreateInstanceDetails(user.id());
                    instanceDetails.setInstalled(true);
                    instanceDetails.setDisabled(false);
                }
                for (String packageName : thirdPartyAps) {
                    var appPackage = scannedPackages.computeIfAbsent(packageName, AppPackage::new);
                    appPackage.setSystemApp(false);

                    var details = appPackage.getOrCreateInstanceDetails(user.id());
                    details.setInstalled(true);
                    details.setDisabled(false);
                }
                for (String packageName : disabledAps) {
                    var appPackage = scannedPackages.computeIfAbsent(packageName, AppPackage::new);
                    var details = appPackage.getOrCreateInstanceDetails(user.id());
                    details.setInstalled(true);
                    details.setDisabled(true);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return scannedPackages;
    }

    /**
     *
     * @param flag '-d' = disabled apps, '-3' = 3rd party apps, '-s' = for system apps
     * @return List of package names
     */
    private List<String> scanAppsForUser(IDevice device, AndroidUser user, String flag) throws ShellCommandUnresponsiveException, AdbCommandRejectedException, IOException, TimeoutException {
        final List<String> temp = new ArrayList<>();
        device.executeShellCommand("pm list packages --user " + user.id() + " " + flag, new MultiLineReceiver() {
            @Override
            public void processNewLines(String[] lines) {
                for (String line : lines) {
                    if (line.startsWith("package:")) {
                        temp.add(line.substring(8).trim());
                    }
                }
            }

            @Override
            public boolean isCancelled() {
                return false;
            }
        });
        return temp;
    }

    public void scanPackageInfoAndUpdateModel(AppPackage appPackage) throws ShellCommandUnresponsiveException, AdbCommandRejectedException, IOException, TimeoutException {

        final List<String> temp = new ArrayList<>();
        connectedIDevice.executeShellCommand("dumpsys package " + appPackage.getPackageName(), new MultiLineReceiver() {
            private boolean inPackageSection = false;

            @Override
            public void processNewLines(String[] lines) {

                for (String line : lines) {
                    if (!inPackageSection && line.equals("Packages:")) {
                        inPackageSection = true;
                    }
                    if (inPackageSection && (line.equals("Queries:") || line.isEmpty())) {
                        break;
                    }
                    if (inPackageSection) {
                        temp.add(line);
                    }
                }
            }

            @Override
            public boolean isCancelled() {
                return false;
            }
        });

        System.out.println("Extracted package info size: " + temp.size());
        var parsedPackage = PackageDetailsParser.parsePackageDetails(temp, getUsers(connectedIDevice));
        if (appPackage.merge(parsedPackage)){
            System.out.println("Package info updated: " + appPackage.getPackageName());
        } else {
            System.out.println("Package info not updated: " + appPackage.getPackageName());
        }
    }
}
