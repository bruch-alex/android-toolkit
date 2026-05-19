package app.androidtoolkit.model;

import lombok.Getter;

@Getter
public enum PlatformTools {
    WINDOWS("https://dl.google.com/android/repository/platform-tools-latest-windows.zip"),
    MAC("https://dl.google.com/android/repository/platform-tools-latest-darwin.zip"),
    LINUX("https://dl.google.com/android/repository/platform-tools-latest-linux.zip");

    private final String url;

    PlatformTools(String url) {
        this.url = url;
    }

    public static PlatformTools current() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) return WINDOWS;
        if (os.contains("mac")) return MAC;
        return LINUX;
    }

}
