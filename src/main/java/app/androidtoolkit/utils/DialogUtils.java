package app.androidtoolkit.utils;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

public final class DialogUtils {
    private DialogUtils() {
    }

    public static boolean showConfirmationDialog(String title, String headerText, boolean isIrreversable) {
        var alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);

        if (isIrreversable) {
            headerText += "\nThis action cannot be undone.";
        }
        alert.setContentText(headerText);
        return alert.showAndWait().filter(response -> response == ButtonType.OK).isPresent();
    }

    public static boolean showConfirmationDialog(String title, String headerText) {
        return showConfirmationDialog(title, headerText, false);
    }
}
