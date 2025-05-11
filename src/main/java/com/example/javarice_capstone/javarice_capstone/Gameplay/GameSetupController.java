package com.example.javarice_capstone.javarice_capstone.Gameplay;

import com.example.javarice_capstone.javarice_capstone.Multiplayer.GenerateLobbyCode;
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

    @FXML private Button startGameButton;
    @FXML private Button cancelButton;
    @FXML private HBox playersContainer;
    @FXML private Label dateTimeLabel;
    @FXML private Button addPlayerButton;
    @FXML private Button removePlayerButton;
    @FXML public TextField lobbyCodeField;

    private static final int MIN_PLAYERS = 2;
    private static final int MAX_PLAYERS = 6;

    private String currentUser = "Player";
    private String lobbyCode = "";
    private boolean isHost = false;
    private boolean isJoin = false;

    /**
     * Call this method to set up the controller for Host mode.
     * @param username The host's username.
     */
    public void setupHost(String username) {
        isHost = true;
        isJoin = false;
        if (username != null && !username.isEmpty()) currentUser = username;
        initializePlayersContainer();
        updateLobbyCodeLabel(GenerateLobbyCode.GenerateLobbyCode());
        updateAddRemoveButtons();
    }

    /**
     * Call this method to set up the controller for Join mode.
     * @param username The joining user's username.
     * @param code The lobby code to join.
     */
    public void setupJoin(String username, String code) {
        isHost = false;
        isJoin = true;
        if (username != null && !username.isEmpty()) currentUser = username;
        if (code != null) lobbyCode = code;
        initializePlayersContainer();
        updateLobbyCodeLabel(lobbyCode);
        if (addPlayerButton != null) addPlayerButton.setDisable(true);
        if (removePlayerButton != null) removePlayerButton.setDisable(true);
    }

    private void updateLobbyCodeLabel(String code) {
        if (lobbyCodeField != null) {
            lobbyCodeField.setText(code != null && !code.isEmpty() ? code : "");
        }
    }

    @FXML
    public void initialize() {
        if (startGameButton != null) startGameButton.setOnAction(e -> handleStartGame());
        if (cancelButton != null) cancelButton.setOnAction(e -> handleCancel());
        if (addPlayerButton != null) addPlayerButton.setOnAction(e -> handleAddPlayer());
        if (removePlayerButton != null) removePlayerButton.setOnAction(e -> handleRemovePlayer());
        updateDateTimeLabel();
        if (!isHost && !isJoin && playersContainer != null) initializePlayersContainer();
        updateAddRemoveButtons();
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
        updateAddRemoveButtons();
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

            if (isHostEntry) {
                playerBox.getStyleClass().add("host-player");
            } else {
                playerBox.getStyleClass().add("player");
            }

            ImageView avatar = (ImageView) playerBox.lookup("#avatarImageView");
            if (avatar != null) {
                avatar.setImage(new Image(getClass().getResourceAsStream("/images/cards/card_back.png")));
            }

            Label nameLabel = (Label) playerBox.lookup("#nameLabel");
            if (nameLabel != null) {
                nameLabel.setText(name);
            }

            playersContainer.getChildren().add(playerBox);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void addPlayer(String name) {
        if (playersContainer != null) addPlayerEntry(name, false);
        updateAddRemoveButtons();
    }

    private void handleAddPlayer() {
        if (playersContainer.getChildren().size() < MAX_PLAYERS) {
            addPlayer("Player " + (playersContainer.getChildren().size() + 1));
        }
        updateAddRemoveButtons();
    }

    private void handleRemovePlayer() {
        if (playersContainer.getChildren().size() > MIN_PLAYERS) removeLastPlayer();
        updateAddRemoveButtons();
    }

    private void updateAddRemoveButtons() {
        int count = playersContainer != null ? playersContainer.getChildren().size() : 0;
        if (addPlayerButton != null)
            addPlayerButton.setDisable(isJoin || count >= MAX_PLAYERS);
        if (removePlayerButton != null)
            removePlayerButton.setDisable(isJoin || count <= MIN_PLAYERS);
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

    private void handleCancel() {
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
}