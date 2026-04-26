module app.androidtoolkit {
    requires javafx.controls;
    requires javafx.fxml;


    opens app.androidtoolkit to javafx.fxml;
    exports app.androidtoolkit;
}