package app.androidtoolkit.service;

import app.androidtoolkit.AppState;
import app.androidtoolkit.mapper.AndroidDeviceMapper;
import app.androidtoolkit.model.AndroidDevice;
import app.androidtoolkit.model.AndroidUser;
import app.androidtoolkit.model.AppPackage;
import app.androidtoolkit.utils.ADBLocator;
import app.androidtoolkit.utils.PackageDetailsParser;
import com.android.ddmlib.*;
import javafx.application.Platform;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class ADBService {
    private final static ADBService INSTANCE = new ADBService();
    private final AppState appState = AppState.getInstance();
    private IDevice connectedIDevice = null;
    private AndroidDebugBridge bridge;

    public static ADBService getInstance() {
        return INSTANCE;
    }

    public void start() {
        start(null);
    }

    public boolean isAdbServiceRunning() {
        return bridge.isConnected();
    }

    public void start(String adbPath) {

        try {

//            String resolvedAdbPath = resolveAdbPath(adbPath);

            log.info("Starting ADB service using: {}", adbPath);

            initializeBridge();

            bridge = createBridge(adbPath);

            registerDeviceListeners(bridge);

            refreshDevices(bridge);

            registerPackageSelectionListener();

            log.info("ADB service started successfully");

        } catch (Exception e) {

            log.error("Failed to start ADB service", e);

//            Platform.runLater(() ->
//                    appState.showError(
//                            "Failed to start ADB",
//                            e.getMessage()
//                    )
//            );
        }
    }

    private String resolveAdbPath(String adbPath) {

        if (adbPath != null && !adbPath.isBlank()) {

            log.debug("Using provided adb path: {}", adbPath);

            return adbPath;
        }

        log.debug("No adb path provided, searching automatically");

        return ADBLocator.findInDefaultAdbLocations()
                .map(Path::toString)
                .orElseThrow(() -> new IllegalStateException(
                        "ADB not found. Please install Android Platform Tools."
                ));
    }

    private void initializeBridge() {
        if (AndroidDebugBridge.getBridge() == null) {
            log.debug("Initializing AndroidDebugBridge");
            AndroidDebugBridge.init(false);
        }
    }

    private AndroidDebugBridge createBridge(String adbPath) {

        log.debug("Creating AndroidDebugBridge");

        AndroidDebugBridge bridge = AndroidDebugBridge.createBridge(
                adbPath,
                false,
                5000,
                TimeUnit.MILLISECONDS
        );

        if (bridge == null) {

            throw new IllegalStateException(
                    "Failed to create AndroidDebugBridge"
            );
        }

        return bridge;
    }

    private void registerDeviceListeners(AndroidDebugBridge bridge) {

        AndroidDebugBridge.addDeviceChangeListener(
                new AndroidDebugBridge.IDeviceChangeListener() {

                    @Override
                    public void deviceConnected(IDevice device) {

                        log.info("Device connected: {}", device.getSerialNumber());

                        refreshDevices(bridge);
                    }

                    @Override
                    public void deviceDisconnected(IDevice device) {

                        log.info("Device disconnected: {}", device.getSerialNumber());

                        connectedIDevice = null;

                        Platform.runLater(appState::deviceDisconnected);

                        refreshDevices(bridge);
                    }

                    @Override
                    public void deviceChanged(IDevice device, int changeMask) {

                        log.debug(
                                "Device changed: {} mask={}",
                                device.getSerialNumber(),
                                changeMask
                        );

                        refreshDevices(bridge);
                    }
                }
        );
    }

    private void registerPackageSelectionListener() {

        appState.getSelectedPackage().addListener((_, _, newValue) -> {

            if (newValue == null) {
                return;
            }

            try {

                log.debug(
                        "Scanning package info for: {}",
                        newValue.getPackageName()
                );

                scanPackageInfoAndUpdateModel(newValue);

                log.info(
                        "Package scan completed: {} permissions",
                        newValue.getPackageDetails()
                                .getInstalledPermissions()
                                .size()
                );

            } catch (Exception e) {

                log.error(
                        "Failed to scan package: {}",
                        newValue.getPackageName(),
                        e
                );
            }
        });
    }

    private void refreshDevices(AndroidDebugBridge bridge) {
        AndroidDevice newDevice = null;
        if (bridge != null) {
            log.debug("ADB Bridge initialized");
            for (IDevice d : bridge.getDevices()) {
                log.debug("Checking device: {}", d);
                if (d != null && d.isOnline()) {
                    log.debug("Configuring new device: {}", d);
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
        log.debug("Configured device: {}", finalDevice);
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
                    instanceDetails.setEnabled(true);
                }
                for (String packageName : thirdPartyAps) {
                    var appPackage = scannedPackages.computeIfAbsent(packageName, AppPackage::new);
                    appPackage.setSystemApp(false);

                    var details = appPackage.getOrCreateInstanceDetails(user.id());
                    details.setInstalled(true);
                    details.setEnabled(true);
                }
                for (String packageName : disabledAps) {
                    var appPackage = scannedPackages.computeIfAbsent(packageName, AppPackage::new);
                    var details = appPackage.getOrCreateInstanceDetails(user.id());
                    details.setInstalled(true);
                    details.setEnabled(false);
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
        var parsedPackage = PackageDetailsParser.getPackageDetails(temp, getUsers(connectedIDevice));
        System.out.println("Parsed package info: " + parsedPackage);
        if (appPackage.merge(parsedPackage)) {
            System.out.println("Package info updated: " + appPackage);
        } else {
            System.out.println("Package info not updated: " + appPackage);
        }
    }

    public void revokePermission(String packageName, String permissionFullName, String uid) throws ShellCommandUnresponsiveException, AdbCommandRejectedException, IOException, TimeoutException {
        String command = "pm revoke " + packageName +
                " " + permissionFullName +
                " --user " + uid;
        connectedIDevice.executeShellCommand(command, new MultiLineReceiver() {
            @Override
            public boolean isCancelled() {
                return false;
            }

            @Override
            public void processNewLines(String[] lines) {
                for (String line : lines) {
                    System.out.println(line);
                }
            }
        });
    }

    public void grantPermission(String packageName, String permissionFullName, String uid) throws ShellCommandUnresponsiveException, AdbCommandRejectedException, IOException, TimeoutException {
        String command = "pm grant " + packageName +
                " " + permissionFullName +
                " --user " + uid;
        connectedIDevice.executeShellCommand(command, new MultiLineReceiver() {
            @Override
            public boolean isCancelled() {
                return false;
            }

            @Override
            public void processNewLines(String[] lines) {
                for (String line : lines) {
                    System.out.println(line);
                }
            }
        });
    }

    public void setEnabledToDisabled(String packageName, String uid) throws ShellCommandUnresponsiveException, AdbCommandRejectedException, IOException, TimeoutException {
        String command = "pm disable-user " + packageName +
                " --user " + uid;
        connectedIDevice.executeShellCommand(command, new MultiLineReceiver() {
            @Override
            public void processNewLines(String[] lines) {
                for (String line : lines) {
                    System.out.println(line);
                }
            }

            @Override
            public boolean isCancelled() {
                return false;
            }
        });
    }

    public void setEnabledToDefaultState(String packageName, String uid) throws ShellCommandUnresponsiveException, AdbCommandRejectedException, IOException, TimeoutException {
        String command = "pm default-state " + packageName +
                " --user " + uid;
        connectedIDevice.executeShellCommand(command, new MultiLineReceiver() {
            @Override
            public void processNewLines(String[] lines) {
                for (String line : lines) {
                    System.out.println(line);
                }
            }

            @Override
            public boolean isCancelled() {
                return false;
            }
        });
    }

    public void deleteAppForUser(String packageName, String uid) throws ShellCommandUnresponsiveException, AdbCommandRejectedException, IOException, TimeoutException {
        String command = "pm uninstall " + "--user " + uid + " " + packageName;
        connectedIDevice.executeShellCommand(command, new MultiLineReceiver() {
            @Override
            public void processNewLines(String[] lines) {
                for (String line : lines) {
                    if (line.contains("Success")) {
                        System.out.println("App deleted: " + packageName);
                        appState.getConnectedDevice().get().getPackages().remove(packageName);
                        return;
                    }
                }
            }

            @Override
            public boolean isCancelled() {
                return false;
            }
        });

    }
}
