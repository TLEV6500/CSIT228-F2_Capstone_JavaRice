package com.example.javarice_capstone.javarice_capstone.Factory;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;

public class SingleplayerContentController {
    @FXML
    private ComboBox<Integer> playerCountComboBox;
    @FXML private Button okButton;
    @FXML private Button cancelButton;

    private GameSetupDialogController parent;

    public void init(GameSetupDialogController parent) {
        this.parent = parent;
        playerCountComboBox.getItems().setAll(2, 3, 4, 5, 6);
    }

    @FXML
    private void initialize() {
        playerCountComboBox.getSelectionModel().selectFirst();
    }

    @FXML
    private void okClicked() {
        Integer count = playerCountComboBox.getValue();
        if (count != null) {
            parent.onSingleplayerOk(count);
        }
    }

    @FXML
    private void cancelClicked() {
        parent.onSingleplayerCancel();
    }
}
