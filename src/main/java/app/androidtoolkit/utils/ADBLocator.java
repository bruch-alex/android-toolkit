package app.androidtoolkit.utils;

import lombok.extern.slf4j.Slf4j;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

@Slf4j
public final class ADBLocator {

    private static final String ADB_EXECUTABLE = isWindows() ? "adb.exe" : "adb";

    private ADBLocator() {
    }

    public static Optional<Path> findAdbPath() {

        log.debug("Searching for adb executable");

        // 1. PATH
        Optional<Path> fromPath = findExecutableInPath();

        if (fromPath.isPresent()) {
            log.info("ADB found in PATH: {}", fromPath.get());
            return fromPath;
        }

        // 2. Known SDK locations
        for (Path candidate : sdkCandidates()) {

            Optional<Path> resolved = resolveFromBase(candidate);

            if (resolved.isPresent()) {
                log.info("ADB found in SDK location: {}", resolved.get());
                return resolved;
            }
        }

        log.warn("ADB executable not found");

        return Optional.empty();
    }

    public static Path requireAdb() {
        return findAdbPath().orElseThrow(() -> {
            log.error("ADB is required but was not found");

            return new IllegalStateException("ADB not found. Install Android Platform Tools " + "and ensure adb is available in PATH or ANDROID_SDK_ROOT.");
        });
    }

    public static boolean isAdbInstalled() {
        return findAdbPath().isPresent();
    }

    public static boolean isValidAdbPath(String input) {
        return resolveAdbPath(input).isPresent();
    }

    public static Optional<Path> resolveAdbPath(String input) {

        if (input == null || input.isBlank()) {
            log.debug("ADB path input is blank");
            return Optional.empty();
        }

        try {

            Path base = Path.of(input.trim());

            log.debug("Resolving adb from input: {}", base);

            // direct executable
            if (isExecutable(base) && isAdbExecutable(base)) {
                return Optional.of(normalize(base));
            }

            return resolveFromBase(base);

        } catch (InvalidPathException e) {

            log.warn("Invalid adb path provided: {}", input);

            return Optional.empty();
        }
    }

    private static Optional<Path> findExecutableInPath() {

        String pathEnv = System.getenv("PATH");

        if (pathEnv == null || pathEnv.isBlank()) {
            log.debug("PATH environment variable is empty");
            return Optional.empty();
        }

        String separator = isWindows() ? ";" : ":";

        for (String dir : pathEnv.split(separator)) {

            try {

                Path candidate = Path.of(dir).resolve(ADB_EXECUTABLE);

                log.trace("Checking PATH candidate: {}", candidate);

                if (isExecutable(candidate)) {
                    return Optional.of(normalize(candidate));
                }

            } catch (InvalidPathException e) {

                log.debug("Skipping invalid PATH entry: {}", dir);
            }
        }

        return Optional.empty();
    }

    private static Optional<Path> resolveFromBase(Path base) {

        // SDK root
        Path sdkAdb = base.resolve("platform-tools").resolve(ADB_EXECUTABLE);

        if (isExecutable(sdkAdb)) {
            return Optional.of(normalize(sdkAdb));
        }

        // platform-tools dir
        Path platformToolsAdb = base.resolve(ADB_EXECUTABLE);

        if (isExecutable(platformToolsAdb)) {
            return Optional.of(normalize(platformToolsAdb));
        }

        return Optional.empty();
    }

    private static List<Path> sdkCandidates() {

        return Stream.of(envPath("ANDROID_SDK_ROOT"), envPath("ANDROID_HOME"),
                        // Linux
                        Path.of(System.getProperty("user.home"), "Android", "Sdk"),
                        Path.of("/usr/lib/android-sdk"),
                        Path.of("/opt/android-sdk"),
                        // macOS
                        Path.of(System.getProperty("user.home"), "Library", "Android", "sdk"),
                        // Windows
                        Path.of(System.getProperty("user.home"), "AppData", "Local", "Android", "Sdk"),
                        // direct platform-tools
                        Path.of(System.getProperty("user.home"), "platform-tools"))
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private static boolean isExecutable(Path path) {
        return Files.isRegularFile(path) && Files.isExecutable(path);
    }

    private static Path normalize(Path path) {
        return path.toAbsolutePath().normalize();
    }

    private static boolean isAdbExecutable(Path path) {

        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);

        return isWindows() ? name.equals("adb.exe") : name.equals("adb");
    }

    private static Path envPath(String name) {

        String value = System.getenv(name);

        if (value == null || value.isBlank()) {
            return null;
        }

        try {

            Path path = Path.of(value);

            log.debug("Environment variable {} -> {}", name, path);

            return path;

        } catch (InvalidPathException e) {

            log.warn("Invalid environment path for {}", name);

            return null;
        }
    }

    private static boolean isWindows() {

        return System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win");
    }
}