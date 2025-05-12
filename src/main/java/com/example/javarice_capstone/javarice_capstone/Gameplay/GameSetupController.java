package com.example.javarice_capstone.javarice_capstone.Gameplay;

import com.example.javarice_capstone.javarice_capstone.Multiplayer.SessionState;
import com.example.javarice_capstone.javarice_capstone.Multiplayer.ThreadLobbyManager;
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
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import javafx.application.Platform;
import javafx.scene.control.Alert;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.sql.ResultSet;

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
    private Timeline playerUpdateTimeline;
    private boolean isReady = false;

    public void setupHost(String username) {
        isHost = true;
        isJoin = false;
        if (username != null && !username.isEmpty()) currentUser = username;
        
        // Set the lobby code from SessionState
        lobbyCode = SessionState.LobbyCode;
        
        // Set host as ready in the database
        try {
            String url = "jdbc:mysql://" + SessionState.LobbyConnection + "/game_data?useSSL=false";
            try (Connection conn = DriverManager.getConnection(url, "root", "")) {
                String updateReadySQL = "UPDATE players_in_lobbies SET is_ready = TRUE WHERE lobby_code = ? AND player = ?";
                try (PreparedStatement stmt = conn.prepareStatement(updateReadySQL)) {
                    stmt.setString(1, lobbyCode);
                    stmt.setString(2, currentUser);
                    stmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            System.err.println("Error setting host ready status: " + e.getMessage());
            e.printStackTrace();
        }
        
        // Clear and update the players container with all players
        if (playersContainer != null) {
            playersContainer.getChildren().clear();
            // Add host as first player
            addPlayerEntry(currentUser, true);
        }
        
        updateLobbyCodeLabel(lobbyCode);
        updateBottomButtons();
        
        // Start periodic player list updates
        startPlayerUpdates();
    }

    public void setupJoin(String username, String code, List<ThreadLobbyManager.PlayerInfo> players) {
        isHost = false;
        isJoin = true;
        if (username != null && !username.isEmpty()) currentUser = username;
        if (code != null) lobbyCode = code;
        
        // Clear and update the players container with all players
        if (playersContainer != null) {
            playersContainer.getChildren().clear();
            for (ThreadLobbyManager.PlayerInfo player : players) {
                addPlayerEntry(player.name, player.isHost);
            }
        }
        
        updateLobbyCodeLabel(lobbyCode);
        updateBottomButtons();
        
        // Start periodic player list updates
        startPlayerUpdates();
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

            if (isHostEntry) {
                playerBox.getStyleClass().add("host-player");
            } else {
                playerBox.getStyleClass().add("player");
                // Check ready status for non-host players
                try {
                    String url = "jdbc:mysql://" + SessionState.LobbyConnection + "/game_data?useSSL=false";
                    try (Connection conn = DriverManager.getConnection(url, "root", "")) {
                        String checkReadySQL = "SELECT is_ready FROM players_in_lobbies WHERE lobby_code = ? AND player = ?";
                        try (PreparedStatement stmt = conn.prepareStatement(checkReadySQL)) {
                            stmt.setString(1, lobbyCode);
                            stmt.setString(2, name);
                            java.sql.ResultSet rs = stmt.executeQuery();
                            if (rs.next()) {
                                boolean isPlayerReady = rs.getBoolean("is_ready");
                                Label nameLabel = (Label) playerBox.lookup("#nameLabel");
                                if (nameLabel != null) {
                                    nameLabel.setText(name);
                                    nameLabel.setStyle(isPlayerReady ? "-fx-text-fill: green;" : "-fx-text-fill: red;");
                                }
                            }
                        }
                    }
                } catch (SQLException e) {
                    System.err.println("Error checking ready status: " + e.getMessage());
                    e.printStackTrace();
                }
            }

            ImageView avatar = (ImageView) playerBox.lookup("#avatarImageView");
            if (avatar != null) avatar.setImage(new Image(getClass().getResourceAsStream("/images/cards/card_back.png")));

            Label nameLabel = (Label) playerBox.lookup("#nameLabel");
            if (nameLabel != null && isHostEntry) {
                nameLabel.setText(name);
                nameLabel.setStyle("-fx-text-fill: green;"); // Host is always ready
            }

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
        if (!isHost) return; // Only host can start the game

        // Check if all players are ready
        try {
            String url = "jdbc:mysql://" + SessionState.LobbyConnection + "/game_data?useSSL=false";
            try (Connection conn = DriverManager.getConnection(url, "root", "")) {
                String checkReadySQL = "SELECT COUNT(*) as total, SUM(CASE WHEN is_ready = 1 THEN 1 ELSE 0 END) as ready_count " +
                                     "FROM players_in_lobbies WHERE lobby_code = ?";
                try (PreparedStatement stmt = conn.prepareStatement(checkReadySQL)) {
                    stmt.setString(1, lobbyCode);
                    ResultSet rs = stmt.executeQuery();
                    if (rs.next()) {
                        int totalPlayers = rs.getInt("total");
                        int readyPlayers = rs.getInt("ready_count");
                        
                        if (readyPlayers < totalPlayers) {
                            // Not all players are ready
                            Alert alert = new Alert(Alert.AlertType.WARNING);
                            alert.setTitle("Cannot Start Game");
                            alert.setHeaderText(null);
                            alert.setContentText("Cannot start the game yet. All players must be ready first.\n" +
                                               "Ready players: " + readyPlayers + "/" + totalPlayers);
                            alert.showAndWait();
                            return;
                        }
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error checking player ready status: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        // If we get here, all players are ready
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
        if (!isJoin) return; // Only handle ready status for joining players
        
        try {
            String url = "jdbc:mysql://" + SessionState.LobbyConnection + "/game_data?useSSL=false";
            try (Connection conn = DriverManager.getConnection(url, "root", "")) {
                // Toggle ready status
                isReady = !isReady;
                String updateReadySQL = "UPDATE players_in_lobbies SET is_ready = ? WHERE lobby_code = ? AND player = ?";
                try (PreparedStatement stmt = conn.prepareStatement(updateReadySQL)) {
                    stmt.setBoolean(1, isReady);
                    stmt.setString(2, lobbyCode);
                    stmt.setString(3, currentUser);
                    stmt.executeUpdate();
                }
                
                // Update button text
                Platform.runLater(() -> {
                    startGameButton.setText(isReady ? "Unready" : "Ready");
                });
            }
        } catch (SQLException e) {
            System.err.println("Error updating ready status: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void startPlayerUpdates() {
        // Create a timeline that updates every second
        playerUpdateTimeline = new Timeline(
            new KeyFrame(Duration.seconds(1), event -> updatePlayerList())
        );
        playerUpdateTimeline.setCycleCount(Timeline.INDEFINITE);
        playerUpdateTimeline.play();
    }

    private void updatePlayerList() {
        if (lobbyCode.isEmpty()) {
            System.out.println("Lobby code is empty, skipping update");
            return;
        }

        System.out.println("Updating player list for lobby: " + lobbyCode);
        // Check lobby status first
        try {
            String url = "jdbc:mysql://" + SessionState.LobbyConnection + "/game_data?useSSL=false";
            try (Connection conn = DriverManager.getConnection(url, "root", "")) {
                String checkLobbySQL = "SELECT status FROM lobbies WHERE lobby_code = ?";
                try (PreparedStatement stmt = conn.prepareStatement(checkLobbySQL)) {
                    stmt.setString(1, lobbyCode);
                    java.sql.ResultSet rs = stmt.executeQuery();
                    if (rs.next()) {
                        String status = rs.getString("status");
                        if ("closed".equalsIgnoreCase(status)) {
                            // Stop the timeline first
                            if (playerUpdateTimeline != null) {
                                playerUpdateTimeline.stop();
                            }
                            
                            // Host has left, show alert and return to main menu
                            Platform.runLater(() -> {
                                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                                alert.setTitle("Host Left");
                                alert.setHeaderText(null);
                                alert.setContentText("The host has left the lobby. Returning to main menu.");
                                alert.showAndWait();
                                try {
                                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/javarice_capstone/javarice_capstone/MenuUI.fxml"));
                                    Parent root = loader.load();
                                    Stage stage = (Stage) playersContainer.getScene().getWindow();
                                    stage.getScene().setRoot(root);
                                    stage.setTitle("UNO - Setup Game");
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            });
                            return;
                        }
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error checking lobby status: " + e.getMessage());
            e.printStackTrace();
        }

        List<ThreadLobbyManager.PlayerInfo> currentPlayers = ThreadLobbyManager.getPlayersInLobby(lobbyCode);
        if (currentPlayers != null && !currentPlayers.isEmpty()) {
            System.out.println("Found " + currentPlayers.size() + " players in lobby");
            // Clear and update the players container
            if (playersContainer != null) {
                playersContainer.getChildren().clear();
                for (ThreadLobbyManager.PlayerInfo player : currentPlayers) {
                    System.out.println("Adding player: " + player.name + (player.isHost ? " (host)" : ""));
                    addPlayerEntry(player.name, player.isHost);
                }
            }
        } else {
            System.out.println("No players found in lobby or error occurred");
        }
    }

    private void handleLeaveLobby() {
        // Stop the player updates when leaving
        if (playerUpdateTimeline != null) {
            playerUpdateTimeline.stop();
        }

        // Remove player from the database
        if (!lobbyCode.isEmpty()) {
            try {
                String url = "jdbc:mysql://" + SessionState.LobbyConnection + "/game_data?useSSL=false";
                try (Connection conn = DriverManager.getConnection(url, "root", "")) {
                    // Remove player from players_in_lobbies
                    String removePlayerSQL = "DELETE FROM players_in_lobbies WHERE lobby_code = ? AND player = ?";
                    try (PreparedStatement stmt = conn.prepareStatement(removePlayerSQL)) {
                        stmt.setString(1, lobbyCode);
                        stmt.setString(2, currentUser);
                        stmt.executeUpdate();
                    }

                    // If this was the host, update the lobby status
                    if (isHost) {
                        String updateLobbySQL = "UPDATE lobbies SET status = 'closed' WHERE lobby_code = ?";
                        try (PreparedStatement stmt = conn.prepareStatement(updateLobbySQL)) {
                            stmt.setString(1, lobbyCode);
                            stmt.executeUpdate();
                        }
                    }
                }
            } catch (SQLException e) {
                System.err.println("Error removing player from lobby: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
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