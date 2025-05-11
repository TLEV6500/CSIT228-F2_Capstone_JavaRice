package com.example.javarice_capstone.javarice_capstone.Gameplay;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class GameSetupController {

    @FXML private Button startGameButton; // Will be Start Game or Ready
    @FXML private Button cancelButton; // Will be Leave Lobby
    @FXML private HBox playersContainer;
    @FXML private Label dateTimeLabel;
    @FXML
    TextField lobbyCodeField;

    private static final int MIN_PLAYERS = 2;
    private static final int MAX_PLAYERS = 6;

    private String currentUser = "Player";
    private String lobbyCode = "";
    private boolean isHost = false;
    private boolean isJoin = false;

    public void setupHost(String username) {
        isHost = true;
        isJoin = false;
        if (username != null && !username.isEmpty()) currentUser = username;
        initializePlayersContainer();
        updateLobbyCodeLabel(generateLobbyCode());
        updateBottomButtons();
    }

    public void setupJoin(String username, String code) {
        isHost = false;
        isJoin = true;
        if (username != null && !username.isEmpty()) currentUser = username;
        if (code != null) lobbyCode = code;
        initializePlayersContainer();
        updateLobbyCodeLabel(lobbyCode);
        updateBottomButtons();
    }

    private void updateLobbyCodeLabel(String code) {
        if (lobbyCodeField != null) {
            lobbyCodeField.setText(code != null && !code.isEmpty() ? code : "");
        }
    }

    @FXML
    public void initialize() {
        updateDateTimeLabel();
        if (!isHost && !isJoin && playersContainer != null) initializePlayersContainer();
        updateBottomButtons();
    }

    private void updateDateTimeLabel() {
        if (dateTimeLabel != null) {
            dateTimeLabel.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
    }

    private void initializePlayersContainer() {
        if (playersContainer == null) return;
        playersContainer.getChildren().clear();
        addPlayerEntry(currentUser, true);
    }

    private void removeLastPlayer() {
        int idx = playersContainer.getChildren().size() - 1;
        if (idx > 0 && playersContainer.getChildren().size() > MIN_PLAYERS) {
            playersContainer.getChildren().remove(idx);
        }
    }

    private void addPlayerEntry(String name, boolean isHostEntry) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/javarice_capstone/javarice_capstone/PlayerCard.fxml"));
            VBox playerBox = loader.load();

            if (isHostEntry) playerBox.getStyleClass().add("host-player");
            else playerBox.getStyleClass().add("player");

            ImageView avatar = (ImageView) playerBox.lookup("#avatarImageView");
            if (avatar != null) avatar.setImage(new Image(getClass().getResourceAsStream("/images/cards/card_back.png")));

            Label nameLabel = (Label) playerBox.lookup("#nameLabel");
            if (nameLabel != null) nameLabel.setText(name);

            playersContainer.getChildren().add(playerBox);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void updateBottomButtons() {
        if (startGameButton != null && cancelButton != null) {
            if (isHost) {
                startGameButton.setText("Start Game");
                startGameButton.setOnAction(e -> handleStartGame());
            } else {
                startGameButton.setText("Ready");
                startGameButton.setOnAction(e -> handleReady());
            }
            cancelButton.setText("Leave Lobby");
            cancelButton.setOnAction(e -> handleLeaveLobby());
        }
    }

    private void handleStartGame() {
        try {
            int numberOfPlayers = playersContainer.getChildren().size();
            List<String> playerNames = new ArrayList<>();
            for (var node : playersContainer.getChildren()) {
                VBox entry = (VBox) node;
                Label nameLabel = (Label) entry.lookup("#nameLabel");
                if (nameLabel != null) {
                    playerNames.add(nameLabel.getText());
                }
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/javarice_capstone/javarice_capstone/GameUI.fxml"));
            Parent root = loader.load();
            GameController gameUIController = loader.getController();

            gameUIController.startGame(numberOfPlayers, playerNames);

            Stage stage = (Stage) startGameButton.getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("UNO - Gameplay");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleReady() {
        // Implement what should happen when a non-host player clicks "Ready"
        System.out.println(currentUser + " is ready!");
        // You may want to update the player's status or send a network message here
    }

    private void handleLeaveLobby() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/javarice_capstone/javarice_capstone/MenuUI.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) cancelButton.getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle("UNO - Setup Game");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String generateLobbyCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
        Random rand = new Random();
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            code.append(chars.charAt(rand.nextInt(chars.length())));
        }
        lobbyCode = code.toString();
        return lobbyCode;
    }
}