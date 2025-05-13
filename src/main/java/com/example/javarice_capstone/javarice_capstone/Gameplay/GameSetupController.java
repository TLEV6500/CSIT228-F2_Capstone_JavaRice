package com.example.javarice_capstone.javarice_capstone.Gameplay;

import com.example.javarice_capstone.javarice_capstone.Multiplayer.SessionState;
import com.example.javarice_capstone.javarice_capstone.Multiplayer.ThreadLobbyManager;
import com.example.javarice_capstone.javarice_capstone.Multiplayer.LobbyManager;
import com.example.javarice_capstone.javarice_capstone.Multiplayer.InitializeDatabase;
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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
    private boolean isWaitingForCards = false;
    private ScheduledExecutorService scheduler;

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
            addPlayerEntry(currentUser, true, true);
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
                addPlayerEntry(player.name, player.isHost, player.isReady);
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
        // Initialize all database tables
        InitializeDatabase.InitializeAllTables();
        
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
        addPlayerEntry(currentUser, true, true);
    }

    private void removeLastPlayer() {
        int idx = playersContainer.getChildren().size() - 1;
        if (idx > 0 && playersContainer.getChildren().size() > MIN_PLAYERS) {
            playersContainer.getChildren().remove(idx);
        }
    }

    private void addPlayerEntry(String name, boolean isHost, boolean isReady) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/javarice_capstone/javarice_capstone/PlayerCard.fxml"));
            VBox playerBox = loader.load();

            ImageView avatar = (ImageView) playerBox.lookup("#avatarImageView");
            if (avatar != null) {
                avatar.setImage(new Image(getClass().getResourceAsStream("/images/cards/card_back.png")));
            }

            Label nameLabel = (Label) playerBox.lookup("#nameLabel");
            if (nameLabel != null) {
                nameLabel.setText(name);
                if (isReady) {
                    nameLabel.setStyle("-fx-text-fill: green;");
                } else {
                    nameLabel.setStyle("-fx-text-fill: red;");
                }
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
        if (!isHost) return;

        // Check if all players are ready
        if (!ThreadLobbyManager.areAllPlayersReady(lobbyCode)) {
            showAlert(Alert.AlertType.WARNING, "Not All Players Ready", 
                "Please wait for all players to be ready before starting the game.");
            return;
        }

        // Update lobby status to started
        try {
            String url = "jdbc:mysql://" + SessionState.LobbyConnection + "/game_data?useSSL=false";
            try (Connection conn = DriverManager.getConnection(url, "root", "")) {
                String updateLobbySQL = "UPDATE lobbies SET status = 'started' WHERE lobby_code = ?";
                try (PreparedStatement stmt = conn.prepareStatement(updateLobbySQL)) {
                    stmt.setString(1, lobbyCode);
                    stmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            showError("Database Error", "Could not update lobby status: " + e.getMessage());
            return;
        }

        // Broadcast start game event to all players in the lobby
        ThreadLobbyManager.broadcastStartGame(lobbyCode);

        // Stop the player updates
        ThreadLobbyManager.stopLobbyUpdates();

        // Get player names for game initialization
        List<String> playerNames = new ArrayList<>();
        for (var node : playersContainer.getChildren()) {
            VBox entry = (VBox) node;
            Label nameLabel = (Label) entry.lookup("#nameLabel");
            if (nameLabel != null) {
                playerNames.add(nameLabel.getText());
            }
        }

        // Initialize game cards
        // GameCardManager.initializeGame(lobbyCode, playerNames);

        // Launch game UI
        try {
            Stage currentStage = (Stage) startGameButton.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/javarice_capstone/javarice_capstone/GameUI.fxml"));
            Parent root = loader.load();
            GameController gameUIController = loader.getController();

            gameUIController.startGame(playerNames.size(), playerNames);

            Scene scene = new Scene(root);
            currentStage.setScene(scene);
            currentStage.setTitle("UNO - Gameplay");
        } catch (Exception e) {
            showError("Error", "Failed to start game: " + e.getMessage());
        }
    }

    private void handleReady() {
        if (!isJoin) return; // Only handle ready status for joining players
        
        isReady = !isReady;
        boolean updated = ThreadLobbyManager.updatePlayerReadyStatus(lobbyCode, currentUser, isReady);
        
        if (updated) {
            Platform.runLater(() -> {
                startGameButton.setText(isReady ? "Unready" : "Ready");
            });
        }
    }

    private void startPlayerUpdates() {
        // Use the enhanced ThreadLobbyManager for real-time updates
        ThreadLobbyManager.startLobbyUpdates(lobbyCode, players -> {
            Platform.runLater(() -> {
                if (playersContainer != null) {
                    playersContainer.getChildren().clear();
                    for (ThreadLobbyManager.PlayerInfo player : players) {
                        addPlayerEntry(player.name, player.isHost, player.isReady);
                    }
                }
            });
        });

        // Check lobby status periodically for non-host players
        if (!isHost) {
            scheduler = Executors.newScheduledThreadPool(1);
            scheduler.scheduleAtFixedRate(() -> {
                String status = ThreadLobbyManager.checkLobbyStatus(lobbyCode);
                if ("started".equals(status)) {
                    Platform.runLater(() -> {
                        try {
                            Stage currentStage = (Stage) startGameButton.getScene().getWindow();
                            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/javarice_capstone/javarice_capstone/GameUI.fxml"));
                            Parent root = loader.load();
                            GameController gameUIController = loader.getController();

                            List<String> playerNames = new ArrayList<>();
                            for (var node : playersContainer.getChildren()) {
                                VBox entry = (VBox) node;
                                Label nameLabel = (Label) entry.lookup("#nameLabel");
                                if (nameLabel != null) {
                                    playerNames.add(nameLabel.getText());
                                }
                            }

                            gameUIController.startGame(playerNames.size(), playerNames);

                            Scene scene = new Scene(root);
                            currentStage.setScene(scene);
                            currentStage.setTitle("UNO - Gameplay");
                        } catch (Exception e) {
                            showError("Error", "Failed to start game: " + e.getMessage());
                        }
                    });
                    scheduler.shutdown();
                }
            }, 0, 1, TimeUnit.SECONDS);
        }
    }

    private void handleLeaveLobby() {
        // Stop the player updates
        ThreadLobbyManager.stopLobbyUpdates();

        try {
            String url = "jdbc:mysql://" + SessionState.LobbyConnection + "/game_data?useSSL=false";
            try (Connection conn = DriverManager.getConnection(url, "root", "")) {
                conn.setAutoCommit(false);
                try {
                    if (isHost) {
                        LobbyManager.deleteLobby(lobbyCode);
                    } else {
                        String removePlayerSQL = "DELETE FROM players_in_lobbies WHERE lobby_code = ? AND player = ?";
                        try (PreparedStatement stmt = conn.prepareStatement(removePlayerSQL)) {
                            stmt.setString(1, lobbyCode);
                            stmt.setString(2, currentUser);
                            stmt.executeUpdate();
                        }
                    }
                    conn.commit();
                    cleanupResources();
                    returnToMainMenu();
                } catch (SQLException e) {
                    conn.rollback();
                    showError("Error leaving lobby", "Failed to properly leave the lobby: " + e.getMessage());
                }
            }
        } catch (SQLException e) {
            showError("Database Error", "Could not connect to the database: " + e.getMessage());
        }
    }

    private void cleanupResources() {
        // Reset session state
        SessionState.LobbyCode = "";
        SessionState.LobbyConnection = "";
        
        // Clear any remaining UI elements
        if (playersContainer != null) {
            playersContainer.getChildren().clear();
        }
        
        // Reset local state
        isHost = false;
        isJoin = false;
        isReady = false;
        currentUser = "Player";
        lobbyCode = "";
    }

    private void returnToMainMenu() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/javarice_capstone/javarice_capstone/MenuUI.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) playersContainer.getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle("UNO - Main Menu");
        } catch (IOException e) {
            showError("Navigation Error", "Failed to return to main menu: " + e.getMessage());
        }
    }

    private void showError(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }
}