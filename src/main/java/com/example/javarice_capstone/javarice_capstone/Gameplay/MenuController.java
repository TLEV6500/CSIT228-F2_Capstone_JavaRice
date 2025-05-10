package com.example.javarice_capstone.javarice_capstone.Gameplay;

import com.example.javarice_capstone.javarice_capstone.Factory.GameSetupDialogController;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.*;

public class MenuController {
    @FXML private VBox singleplayerCard;
    @FXML private VBox hostGameCard; // changed from HostGame
    @FXML private VBox joinGameCard; // changed from JoinGame
    @FXML private VBox exitCard;

    @FXML private ImageView singleplayerImageView;
    @FXML private ImageView hostgameImageView;
    @FXML private ImageView joingameImageView;
    @FXML private ImageView exitImageView;

    private final List<String> aiNamePool = Arrays.asList(
            "Ven", "Raimar", "Grant", "Tim", "Jay Vince", "Romar", "Aaron", "Zillion", "Raymond", "Seth", "4 AM Gaming", "5 Cans of Red Bull", "No Sleep"
    );

    @FXML
    private void initialize() {
        singleplayerImageView.setImage(new Image(getClass().getResourceAsStream("/images/cards/green_1.png")));
        hostgameImageView.setImage(new Image(getClass().getResourceAsStream("/images/cards/blue_6.png")));
        joingameImageView.setImage(new Image(getClass().getResourceAsStream("/images/cards/yellow_reverse.png")));
        exitImageView.setImage(new Image(getClass().getResourceAsStream("/images/cards/red_skip.png")));

        singleplayerCard.setOnMouseClicked(e -> handleSingleplayer());
        hostGameCard.setOnMouseClicked(e -> handleHostGame());
        joinGameCard.setOnMouseClicked(e -> handleJoinGame());
        exitCard.setOnMouseClicked(e -> exitGame());
    }

    private void handleHostGame() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/javarice_capstone/javarice_capstone/SetupDialog.fxml"));
            Parent dialogRoot = loader.load();
            GameSetupDialogController dialogController = loader.getController();

            dialogController.setMode(GameSetupDialogController.Mode.HOST);

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Host Game Setup");
            dialogStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            dialogStage.setScene(new Scene(dialogRoot));
            dialogStage.setResizable(false);

            dialogStage.showAndWait();

            dialogController.getHostGameResult().ifPresent(result -> {
                launchGameSetupUI(GameSetupDialogController.Mode.HOST, result);
            });
        } catch (Exception e) {
            e.printStackTrace();
            showError("Cannot load host game setup dialog", "Make sure SetupDialog.fxml exists in the resources folder.");
        }
    }

    private void handleJoinGame() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/javarice_capstone/javarice_capstone/SetupDialog.fxml"));
            Parent dialogRoot = loader.load();
            GameSetupDialogController dialogController = loader.getController();

            dialogController.setMode(GameSetupDialogController.Mode.JOIN);

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Join Game Setup");
            dialogStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            dialogStage.setScene(new Scene(dialogRoot));
            dialogStage.setResizable(false);

            dialogStage.showAndWait();

            dialogController.getJoinGameResult().ifPresent(result -> {

            });
        } catch (Exception e) {
            e.printStackTrace();
            showError("Cannot load join game setup dialog", "Make sure SetupDialog.fxml exists in the resources folder.");
        }
    }

    private void launchGameSetupUI(GameSetupDialogController.Mode mode, GameSetupDialogController.MultiplayerSetupResult setupResult) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/javarice_capstone/javarice_capstone/GameSetupUI.fxml"));
            Parent setupUIRoot = loader.load();
            Object setupController = loader.getController();

            if (mode == GameSetupDialogController.Mode.HOST) {
                setupController.getClass().getMethod("setupHost", String.class).invoke(setupController, setupResult.username);
            } else if (mode == GameSetupDialogController.Mode.JOIN) {
                setupController.getClass().getMethod("setupJoin", String.class, String.class).invoke(setupController, setupResult.username, setupResult.hostCode);
            }

            Stage stage = (Stage) hostGameCard.getScene().getWindow();
            Scene scene = new Scene(setupUIRoot);
            stage.setScene(scene);
            stage.setTitle(mode == GameSetupDialogController.Mode.HOST ? "Host Lobby" : "Join Lobby");
        } catch (Exception e) {
            e.printStackTrace();
            showError("Cannot launch game setup UI", "Something went wrong while launching the game setup UI.");
        }
    }

    private void exitGame() {
        System.exit(0);
    }

    private void handleSingleplayer() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/javarice_capstone/javarice_capstone/SetupDialog.fxml"));
            Parent dialogRoot = loader.load();
            GameSetupDialogController dialogController = loader.getController();

            dialogController.setMode(GameSetupDialogController.Mode.SINGLEPLAYER);

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Quick Game Setup");
            dialogStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            dialogStage.setScene(new Scene(dialogRoot));
            dialogStage.setResizable(false);

            dialogStage.showAndWait();

            Optional<Integer> result = dialogController.getSelectedPlayerCount();

            result.ifPresent(numPlayers -> {
                List<String> playerNames = new ArrayList<>();
                playerNames.add("Player");
                playerNames.addAll(getUniqueAiNames(numPlayers - 1));
                launchSingleplayerGame(numPlayers, playerNames);
            });
        } catch (Exception e) {
            e.printStackTrace();
            showError("Cannot load singleplayer dialog", "Make sure SetupDialog.fxml exists in the resources folder.");
        }
    }

    private List<String> getUniqueAiNames(int count) {
        List<String> aiNames = new ArrayList<>();
        List<String> shuffled = new ArrayList<>(aiNamePool);
        Collections.shuffle(shuffled);
        int idx = 0;
        for (int i = 0; i < count; i++) {
            if (idx < shuffled.size()) {
                aiNames.add(shuffled.get(idx++));
            } else {
                aiNames.add("Computer " + (i + 1));
            }
        }
        return aiNames;
    }

    private void launchSingleplayerGame(int numberOfPlayers, List<String> playerNames) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/javarice_capstone/javarice_capstone/GameUI.fxml"));
            Parent root = loader.load();
            GameController gameUIController = loader.getController();

            gameUIController.startGame(numberOfPlayers, playerNames);

            Stage stage = (Stage) singleplayerCard.getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("UNO - Gameplay");
        } catch (Exception e) {
            e.printStackTrace();
            showError("Cannot launch singleplayer game", "Something went wrong while launching the game.");
        }
    }

    private void showError(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}