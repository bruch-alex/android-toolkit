package app.androidtoolkit.utils;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public final class ADBLocator {

    private ADBLocator() {}

    public static Optional<Path> findAdbPath() {
        String adbExecutable = isWindows() ? "adb.exe" : "adb";

        // 1. Check PATH first
        Optional<Path> adbFromPath = findExecutableInPath(adbExecutable);
        if (adbFromPath.isPresent()) {
            return adbFromPath;
        }

        // 2. Common SDK locations
        List<Path> candidates = Stream.of(
                        envPath("ANDROID_SDK_ROOT"),
                        envPath("ANDROID_HOME"),
                        Path.of(System.getProperty("user.home"), "Android", "Sdk"),
                        Path.of(System.getProperty("user.home"), "Library", "Android", "sdk"), // macOS
                        Path.of(System.getProperty("user.home"), "AppData", "Local", "Android", "Sdk"), // Windows
                        Path.of("/usr/lib/android-sdk"),
                        Path.of("/opt/android-sdk"),
                        Path.of(System.getProperty("user.home"), "platform-tools") // direct platform-tools dir
                )
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        for (Path candidate : candidates) {

            // Handle direct platform-tools path
            Path adbPath;
            if (candidate.getFileName() != null &&
                    candidate.getFileName().toString().equals("platform-tools")) {

                adbPath = candidate.resolve(adbExecutable);

            } else {
                adbPath = candidate.resolve("platform-tools")
                        .resolve(adbExecutable);
            }

            if (isExecutable(adbPath)) {
                return Optional.of(adbPath.toAbsolutePath().normalize());
            }
        }

        return Optional.empty();
    }

    public static Path requireAdb() {
        return findAdbPath()
                .orElseThrow(() -> new IllegalStateException(
                        "ADB not found. Install Android Platform Tools " +
                                "and ensure adb is available in PATH or ANDROID_SDK_ROOT."
                ));
    }

    public static boolean isAdbInstalled() {
        return findAdbPath().isPresent();
    }

    private static Optional<Path> findExecutableInPath(String executable) {
        String pathEnv = System.getenv("PATH");

        if (pathEnv == null || pathEnv.isBlank()) {
            return Optional.empty();
        }

        String separator = isWindows() ? ";" : ":";

        for (String dir : pathEnv.split(separator)) {
            try {
                Path candidate = Path.of(dir).resolve(executable);

                if (isExecutable(candidate)) {
                    return Optional.of(candidate.toAbsolutePath().normalize());
                }
            } catch (InvalidPathException ignored) {
            }
        }

        return Optional.empty();
    }

    private static boolean isExecutable(Path path) {
        return Files.isRegularFile(path) && Files.isExecutable(path);
    }

    private static boolean isWindows() {
        return System.getProperty("os.name")
                .toLowerCase(Locale.ROOT)
                .contains("win");
    }

    private static Path envPath(String name) {
        String value = System.getenv(name);

        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return Path.of(value);
        } catch (InvalidPathException e) {
            return null;
        }
    }


    /**
     * Checks if adb exists at a user-provided location.
     */
    public static boolean isValidAdbPath(String input) {
        if (input == null || input.isBlank()) {
            return false;
        }

        try {
            Path path = Path.of(input.trim());

            // User may provide:
            // - full adb executable path
            // - SDK root
            // - platform-tools directory

            // Case 1: direct executable
            if (isExecutable(path)) {
                return isAdbExecutable(path);
            }

            String adbName = adbExecutableName();

            // Case 2: SDK root
            Path sdkAdb = path.resolve("platform-tools")
                    .resolve(adbName);

            if (isExecutable(sdkAdb)) {
                return true;
            }

            // Case 3: platform-tools directory
            Path platformToolsAdb = path.resolve(adbName);

            return isExecutable(platformToolsAdb);

        } catch (InvalidPathException e) {
            return false;
        }
    }

    /**
     * Resolves adb executable from user input.
     */
    public static Optional<Path> resolveAdbPath(String input) {
        if (input == null || input.isBlank()) {
            return Optional.empty();
        }

        try {
            Path path = Path.of(input.trim());

            // Direct executable
            if (isExecutable(path) && isAdbExecutable(path)) {
                return Optional.of(path.toAbsolutePath().normalize());
            }

            String adbName = adbExecutableName();

            // SDK root
            Path sdkAdb = path.resolve("platform-tools")
                    .resolve(adbName);

            if (isExecutable(sdkAdb)) {
                return Optional.of(sdkAdb.toAbsolutePath().normalize());
            }

            // platform-tools directory
            Path platformToolsAdb = path.resolve(adbName);

            if (isExecutable(platformToolsAdb)) {
                return Optional.of(platformToolsAdb.toAbsolutePath().normalize());
            }

        } catch (InvalidPathException ignored) {
        }

        return Optional.empty();
    }


    private static boolean isAdbExecutable(Path path) {
        String name = path.getFileName().toString().toLowerCase();

        return isWindows()
                ? name.equals("adb.exe")
                : name.equals("adb");
    }

    private static String adbExecutableName() {
        return isWindows() ? "adb.exe" : "adb";
    }

}
