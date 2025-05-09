package com.example.javarice_capstone.javarice_capstone.Gameplay;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.stage.Stage;

public class MenuController {
    @FXML private Button singleplayerButton;
    @FXML private Button multiplayerButton;
    @FXML private Button exitButton;

    @FXML
    private void initialize() {
        singleplayerButton.setOnAction(e -> handleSingleplayer());
        multiplayerButton.setOnAction(e -> handleMultiplayer());
        exitButton.setOnAction(e -> exitGame());
    }

    private void handleSingleplayer() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/javarice_capstone/javarice_capstone/GameSetupUI.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) singleplayerButton.getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle("UNO - Setup Game");
        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Cannot load game setup screen");
            alert.setContentText("Make sure GameSetupUI.fxml exists in the resources/Gameplay folder.");
            alert.showAndWait();
        }
    }

    private void exitGame() {
        System.exit(0);
    }

    private void handleMultiplayer() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Multiplayer Mode");
        alert.setHeaderText(null);
        alert.setContentText("Multiplayer mode is not implemented yet.");
        alert.showAndWait();
    }

}