module app.androidtoolkit {
    requires javafx.controls;
    requires javafx.fxml;
    requires static lombok;
    requires ddmlib;

    opens app.androidtoolkit to javafx.fxml;
    exports app.androidtoolkit to javafx.graphics;
    exports app.androidtoolkit.controller to javafx.fxml;

    opens app.androidtoolkit.model to javafx.fxml;
    opens app.androidtoolkit.controller to javafx.fxml;
    opens app.androidtoolkit.viewmodel to javafx.base;
}