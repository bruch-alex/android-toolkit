package app.androidtoolkit.controller;

import javafx.event.ActionEvent;
import lombok.extern.slf4j.Slf4j;

import java.awt.*;
import java.net.URI;

@Slf4j
public class MainController {
    public void openSourceCode(ActionEvent actionEvent) {
        Thread thread = new Thread(() -> {
            try {
                Desktop.getDesktop().browse(
                        new URI("https://github.com/bruch-alex/android-toolkit")
                );
            } catch (Exception e) {
                log.error("Failed to open source code", e);
            }
        });
        thread.setDaemon(true);
        thread.setName("browser-opener");
        thread.start();
    }
}

