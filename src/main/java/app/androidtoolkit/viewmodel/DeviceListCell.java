package app.androidtoolkit.viewmodel;

import app.androidtoolkit.model.device.AndroidDeviceRecord;
import atlantafx.base.controls.Spacer;
import atlantafx.base.theme.Styles;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;

import java.util.function.Consumer;

public class DeviceListCell extends ListCell<AndroidDeviceRecord> {
    private final HBox root;
    private final Label titleLabel;

    public DeviceListCell(Consumer<AndroidDeviceRecord> onConnect) {
        titleLabel = new Label();
        var connectButton = new Button("Select");
        connectButton.getStyleClass().addAll(Styles.ACCENT);
        connectButton.setOnAction(_ -> {
            AndroidDeviceRecord item = getItem();
            if (item != null) onConnect.accept(item);
        });

        root = new HBox(10,
                titleLabel,
                new Spacer(),
                connectButton);
        root.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
    }

    @Override
    public void updateItem(AndroidDeviceRecord item, boolean empty) {
        super.updateItem(item, empty);

        if (empty || item == null) {
            setGraphic(null);
            return;
        }

        titleLabel.setText(item.model());
        setGraphic(root);
    }
}
