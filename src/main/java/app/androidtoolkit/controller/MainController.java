package app.androidtoolkit.controller;

import javafx.event.ActionEvent;

import java.awt.*;
import java.net.URI;

public class MainController {
    public void openSourceCode(ActionEvent actionEvent) {
        Thread thread = new Thread(() -> {
            try {
                Desktop.getDesktop().browse(
                        new URI("https://github.com/bruch-alex/android-toolkit")
                );
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        thread.setDaemon(true);
        thread.setName("browser-opener");
        thread.start();
    }
}

