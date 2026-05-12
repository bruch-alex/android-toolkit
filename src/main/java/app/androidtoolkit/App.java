package app.androidtoolkit;

import app.androidtoolkit.service.ADBService;
import atlantafx.base.theme.PrimerDark;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class App extends Application {
    private final AppState appState = AppState.getInstance();
    private Scene connectedDeviceStage;
    private Scene setupStage;
    private Stage primaryStage;

    @Override
    public void start(Stage stage) throws IOException {
        this.primaryStage = stage;
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());
//        ADBService adb = ADBService.getInstance();
//        adb.start();

        FXMLLoader setupLoader = new FXMLLoader(App.class.getResource("fxml/setup-adb.fxml"));
        setupStage = new Scene(setupLoader.load(), 1920, 1080);

        FXMLLoader appLoader = new FXMLLoader(App.class.getResource("fxml/main-view.fxml"));
        connectedDeviceStage = new Scene(appLoader.load(), 1920, 1080);

        primaryStage.setTitle("Android Device Toolkit");
        primaryStage.setScene(setupStage);
        primaryStage.setMinWidth(1280);
        primaryStage.setMinHeight(720);
        primaryStage.show();

        startMonitoring();
    }

    private void startMonitoring() {
        appState.getConnectedDevice().addListener((_, _, newDevice) -> {
            if (newDevice != null) {
                Platform.runLater(() -> primaryStage.setScene(connectedDeviceStage));
            } else Platform.runLater(() -> primaryStage.setScene(setupStage));
        });
    }
}
