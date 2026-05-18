package app.androidtoolkit.utils;

import app.androidtoolkit.model.PlatformTools;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ADBInstaller {
    private static final Path ADB_DIR = Path.of(
            System.getProperty("user.home"), ".androidtoolkit", "platform-tools"
    );

    public Path getOrInstall() throws Exception {
        Path adbPath = resolveAdbPath();
        if (Files.exists(adbPath)) return adbPath;

        download();
        adbPath.toFile().setExecutable(true);
        return adbPath;
    }

    private Path resolveAdbPath() {
        String binary = System.getProperty("os.name").toLowerCase()
                .contains("win") ? "adb.exe" : "adb";
        return ADB_DIR.resolve(binary);
    }

    private void download() throws Exception {
        String url = PlatformTools.current().getUrl();
        Path zipPath = ADB_DIR.getParent().resolve("platform-tools.zip");
        Files.createDirectories(ADB_DIR.getParent());

        try (var in = URI.create(url).toURL().openStream()) {
            Files.copy(in, zipPath, StandardCopyOption.REPLACE_EXISTING);
        }

        try (var zip = new ZipInputStream(Files.newInputStream(zipPath))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                Path target = ADB_DIR.getParent().resolve(entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectories(target);
                } else {
                    Files.createDirectories(target.getParent());
                    Files.copy(zip, target, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }

        Files.deleteIfExists(zipPath);
    }
}
