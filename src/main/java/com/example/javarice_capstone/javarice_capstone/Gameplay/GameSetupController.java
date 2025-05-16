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
import com.example.javarice_capstone.javarice_capstone.Models.Game;
import com.example.javarice_capstone.javarice_capstone.Abstracts.AbstractCard;
import com.example.javarice_capstone.javarice_capstone.Models.MultiplayerGame;
import com.example.javarice_capstone.javarice_capstone.Models.PlayerComputer;
import com.example.javarice_capstone.javarice_capstone.Strategies.NormalStrat;

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
    private Game game;

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
        
        // Get the number of players from the database for multiplayer games
        int numPlayers = 2; // Default minimum
        if (isHost || isJoin) {
            try {
                String url = "jdbc:mysql://" + SessionState.LobbyConnection + "/game_data?useSSL=false";
                try (Connection conn = DriverManager.getConnection(url, "root", "")) {
                    String query = "SELECT player_count FROM lobbies WHERE lobby_code = ?";
                    try (PreparedStatement stmt = conn.prepareStatement(query)) {
                        stmt.setString(1, lobbyCode);
                        ResultSet rs = stmt.executeQuery();
                        if (rs.next()) {
                            numPlayers = rs.getInt("player_count");
                            System.out.println("Found player count in database: " + numPlayers);
                        }
                    }
                }
            } catch (SQLException e) {
                System.err.println("Error getting player count: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            // For single player, use the number of players in the container
            numPlayers = playersContainer.getChildren().size();
        }
        
        // Initialize the game with the correct number of players
        if (isHost || isJoin) {
            game = new MultiplayerGame(numPlayers, lobbyCode, isHost, currentUser);
        } else {
            game = new Game(numPlayers);
            // For single player mode, ensure we have computer players
            while (game.getPlayers().size() < numPlayers) {
                game.getPlayers().add(new PlayerComputer("Computer " + (game.getPlayers().size() + 1), new NormalStrat()));
            }
        }
        
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

        // Fetch the top discard pile card and push it to the database
        AbstractCard topCard = game.getTopCard();
        String discardPileCard = topCard.getColor() + "_" + topCard.getValue();
        
        // Push to database and verify
        boolean pushSuccess = false;
        int retryCount = 0;
        while (!pushSuccess && retryCount < 3) {
            ThreadLobbyManager.pushDiscardPile(lobbyCode, discardPileCard);
            
            // Verify the card was pushed successfully
            try {
                Thread.sleep(500); // Wait for database update
                String pushedCard = ThreadLobbyManager.fetchDiscardPile(lobbyCode);
                pushSuccess = discardPileCard.equals(pushedCard);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            
            if (!pushSuccess) {
                retryCount++;
            }
        }
        
        if (!pushSuccess) {
            showError("Synchronization Error", "Could not synchronize discard pile. Please try again.");
            return;
        }

        // Wait a moment to ensure database update is propagated
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        // Then update local game state
        game.updateDiscardPile(discardPileCard);

        // Log the top discard pile card for the host
        System.out.println("Host's top discard pile card: " + discardPileCard);
        System.out.println("Host's entire discard pile: " + game.getDiscardPile());

        // Broadcast start game event to all players in the lobby
        ThreadLobbyManager.broadcastStartGame(lobbyCode);

        // Stop the player updates
        ThreadLobbyManager.stopLobbyUpdates();

        // Get player names for game initialization from the database
        List<ThreadLobbyManager.PlayerInfo> playerInfos = ThreadLobbyManager.getPlayersInLobby(lobbyCode);
        for (ThreadLobbyManager.PlayerInfo info : playerInfos) {
            System.out.println("[DEBUG] Player in lobby: " + info.name);
        }
        List<String> playerNames = new ArrayList<>();
        for (ThreadLobbyManager.PlayerInfo info : playerInfos) {
            playerNames.add(info.name);
        }
        System.out.println("[DEBUG] playerNames passed to GameController: " + playerNames);

        // Rotate playerNames so local player is first
        String localPlayerName = currentUser;
        int localIndex = playerNames.indexOf(localPlayerName);
        if (localIndex > 0) {
            List<String> rotated = new ArrayList<>();
            rotated.addAll(playerNames.subList(localIndex, playerNames.size()));
            rotated.addAll(playerNames.subList(0, localIndex));
            playerNames = rotated;
        }
        System.out.println("[DEBUG] Rotated playerNames for local view: " + playerNames);

        // Always reconstruct the game with the correct number of players for the host
        game = new com.example.javarice_capstone.javarice_capstone.Models.MultiplayerGame(playerNames.size(), lobbyCode, isHost, currentUser);
        // Ensure the game has enough player objects
        while (game.getPlayers().size() < playerNames.size()) {
            game.getPlayers().add(new com.example.javarice_capstone.javarice_capstone.Models.PlayerHuman("Player" + (game.getPlayers().size() + 1)));
        }
        for (int i = 0; i < playerNames.size(); i++) {
            game.getPlayers().get(i).setName(playerNames.get(i));
        }
        // Set the discard pile for the host from the database
        String hostDiscardPileCard = ThreadLobbyManager.fetchDiscardPile(lobbyCode);
        if (hostDiscardPileCard != null) {
            game.updateDiscardPile(hostDiscardPileCard);
        }

        // Launch game UI
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/javarice_capstone/javarice_capstone/GameUI.fxml"));
            Parent root = loader.load();
            GameController gameUIController = loader.getController();
            gameUIController.setGame(game);
            gameUIController.startGame(playerNames.size(), playerNames);

            Stage currentStage = (Stage) startGameButton.getScene().getWindow();
            Scene scene = new Scene(root);
            currentStage.setScene(scene);
            currentStage.setTitle("UNO - Gameplay");
            currentStage.show();

            // Start polling for discard pile changes for host
            ScheduledExecutorService discardPoller = Executors.newSingleThreadScheduledExecutor();
            discardPoller.scheduleAtFixedRate(() -> {
                try {
                    String dbTopCard = ThreadLobbyManager.fetchDiscardPile(lobbyCode);
                    AbstractCard localTopCard = game.getTopCard();
                    String localTopCardStr = localTopCard != null ? localTopCard.getColor() + "_" + localTopCard.getValue() : null;
                    System.out.println("[HOST POLL] DB Card: " + dbTopCard + " | Local Card: " + localTopCardStr);
                    
                    if (dbTopCard != null && !dbTopCard.equals(localTopCardStr)) {
                        Platform.runLater(() -> {
                            System.out.println("[HOST POLL] Updating discard pile from DB: " + dbTopCard);
                            game.updateDiscardPile(dbTopCard);
                            if (gameUIController != null) {
                                System.out.println("[HOST POLL] Calling updateUI()");
                                gameUIController.updateUI();
                            }
                        });
                    }
                } catch (Exception e) {
                    System.err.println("[HOST POLL] Error in polling: " + e.getMessage());
                    e.printStackTrace();
                }
            }, 0, 2, TimeUnit.SECONDS);

            // Start polling for current player changes for host
            ScheduledExecutorService currentPlayerPoller = Executors.newSingleThreadScheduledExecutor();
            currentPlayerPoller.scheduleAtFixedRate(() -> {
                try {
                    String dbCurrentPlayer = ThreadLobbyManager.getCurrentPlayer(lobbyCode);
                    String localCurrentPlayer = game.getCurrentPlayer().getName();
                    System.out.println("[HOST POLL] DB Current Player: " + dbCurrentPlayer + " | Local Current Player: " + localCurrentPlayer);
                    
                    if (dbCurrentPlayer != null && !dbCurrentPlayer.equals(localCurrentPlayer)) {
                        Platform.runLater(() -> {
                            System.out.println("[HOST POLL] Updating current player to: " + dbCurrentPlayer);
                            game.setCurrentPlayer(dbCurrentPlayer);
                            if (gameUIController != null) {
                                System.out.println("[HOST POLL] Calling updateUI()");
                                gameUIController.updateUI();
                            }
                        });
                    }
                } catch (Exception e) {
                    System.err.println("[HOST POLL] Error in current player polling: " + e.getMessage());
                    e.printStackTrace();
                }
            }, 0, 2, TimeUnit.SECONDS);

            System.out.println("[DEBUG] Host polling setup complete");
        } catch (Exception e) {
            System.err.println("[DEBUG] Error launching game UI: " + e.getMessage());
            e.printStackTrace();
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
            System.out.println("[DEBUG] Setting up joiner polling...");
            scheduler = Executors.newScheduledThreadPool(1);
            scheduler.scheduleAtFixedRate(() -> {
                String status = ThreadLobbyManager.checkLobbyStatus(lobbyCode);
                System.out.println("[DEBUG] Joiner checking lobby status: " + status);
                if ("started".equals(status)) {
                    System.out.println("[DEBUG] Game started, setting up joiner game...");
                    // Fetch the discard pile card from the database with retries
                    String discardPileCard = null;
                    int retryCount = 0;
                    while (discardPileCard == null && retryCount < 3) {
                        discardPileCard = ThreadLobbyManager.fetchDiscardPile(lobbyCode);
                        if (discardPileCard == null) {
                            try {
                                Thread.sleep(2000);  // Changed to 2 seconds
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            retryCount++;
                        }
                    }
                    
                    // Poll for moves and update local game state/UI
                    List<ThreadLobbyManager.MoveInfo> moves = ThreadLobbyManager.getGameMoves(lobbyCode);
                    if (!moves.isEmpty()) {
                        ThreadLobbyManager.MoveInfo latestMove = moves.get(moves.size() - 1);
                        // Update discard pile if the latest move is a play
                        if ("play".equals(latestMove.action) && latestMove.cardPlayed != null && !latestMove.cardPlayed.isEmpty()) {
                            game.updateDiscardPile(latestMove.cardPlayed);
                        }
                        // Only allow local player to act if it's their turn
                        boolean isMyTurn = latestMove != null && !latestMove.playerName.equals(currentUser);
                        // You can use isMyTurn to enable/disable UI actions
                        // (e.g., set a flag or call Platform.runLater to update UI controls)
                    }
                    
                    if (discardPileCard != null) {
                        System.out.println("Joined player received discard pile card: " + discardPileCard);
                        // Update the local discard pile
                        game.updateDiscardPile(discardPileCard);
                        
                        // Verify the update
                        AbstractCard topCard = game.getTopCard();
                        String localCard = topCard.getColor() + "_" + topCard.getValue();
                        if (!discardPileCard.equals(localCard)) {
                            System.out.println("Warning: Local discard pile card does not match database card");
                            System.out.println("Database card: " + discardPileCard);
                            System.out.println("Local card: " + localCard);
                        }

                        // Log the top discard pile card for joined players
                        System.out.println("Joined player's top discard pile card: " + discardPileCard);
                        System.out.println("Joined player's entire discard pile: " + game.getDiscardPile());
                    } else {
                        System.out.println("Error: Could not fetch discard pile card after multiple retries");
                    }

                    Platform.runLater(() -> {
                        try {
                            System.out.println("[DEBUG] Creating joiner game UI...");
                            Stage currentStage = (Stage) startGameButton.getScene().getWindow();
                                                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/javarice_capstone/javarice_capstone/GameUI.fxml"));
                                                Parent root = loader.load();
                                                GameController gameUIController = loader.getController();

                            // Get player names from the database for correct order and names
                            List<ThreadLobbyManager.PlayerInfo> playerInfos = ThreadLobbyManager.getPlayersInLobby(lobbyCode);
                            for (ThreadLobbyManager.PlayerInfo info : playerInfos) {
                                System.out.println("[DEBUG] Player in lobby: " + info.name);
                            }
                            List<String> playerNames = new ArrayList<>();
                            for (ThreadLobbyManager.PlayerInfo info : playerInfos) {
                                playerNames.add(info.name);
                            }
                            System.out.println("[DEBUG] playerNames passed to GameController: " + playerNames);

                            // Rotate playerNames so local player is first
                            String localPlayerName = currentUser;
                            int localIndex = playerNames.indexOf(localPlayerName);
                            if (localIndex > 0) {
                                List<String> rotated = new ArrayList<>();
                                rotated.addAll(playerNames.subList(localIndex, playerNames.size()));
                                rotated.addAll(playerNames.subList(0, localIndex));
                                playerNames = rotated;
                            }
                            System.out.println("[DEBUG] Rotated playerNames for local view: " + playerNames);

                            // Always reconstruct the game with the correct number of players for the joiner
                            game = new com.example.javarice_capstone.javarice_capstone.Models.MultiplayerGame(playerNames.size(), lobbyCode, isHost, currentUser);
                            // Ensure the game has enough player objects
                            while (game.getPlayers().size() < playerNames.size()) {
                                game.getPlayers().add(new com.example.javarice_capstone.javarice_capstone.Models.PlayerHuman("Player" + (game.getPlayers().size() + 1)));
                            }
                            for (int i = 0; i < playerNames.size(); i++) {
                                game.getPlayers().get(i).setName(playerNames.get(i));
                            }
                            // Set the discard pile for the joiner
                            String joinerDiscardPileCard = ThreadLobbyManager.fetchDiscardPile(lobbyCode);
                            if (joinerDiscardPileCard != null) {
                                game.updateDiscardPile(joinerDiscardPileCard);
                            }
                            gameUIController.setGame(game);
                            gameUIController.startGame(playerNames.size(), playerNames);

                            // Set the new scene and show it
                                                Scene scene = new Scene(root);
                                                currentStage.setScene(scene);
                                                currentStage.setTitle("UNO - Gameplay");
                            currentStage.show(); // Make sure the window is visible

                            // Start polling for discard pile changes
                            ScheduledExecutorService discardPoller = Executors.newSingleThreadScheduledExecutor();
                            discardPoller.scheduleAtFixedRate(() -> {
                                try {
                                    String dbTopCard = ThreadLobbyManager.fetchDiscardPile(lobbyCode);
                                    AbstractCard localTopCard = game.getTopCard();
                                    String localTopCardStr = localTopCard != null ? localTopCard.getColor() + "_" + localTopCard.getValue() : null;
                                    System.out.println("[POLL] DB Card: " + dbTopCard + " | Local Card: " + localTopCardStr);
                                    
                                    if (dbTopCard != null && !dbTopCard.equals(localTopCardStr)) {
                                        Platform.runLater(() -> {
                                            System.out.println("[POLL] Updating discard pile from DB: " + dbTopCard);
                                            game.updateDiscardPile(dbTopCard);
                                            if (gameUIController != null) {
                                                System.out.println("[POLL] Calling updateUI()");
                                                gameUIController.updateUI();
                                            }
                                        });
                                    }
                                } catch (Exception e) {
                                    System.err.println("[POLL] Error in polling: " + e.getMessage());
            e.printStackTrace();
        }
                            }, 0, 1, TimeUnit.SECONDS);

                            // Start polling for current player changes
                            ScheduledExecutorService currentPlayerPoller = Executors.newSingleThreadScheduledExecutor();
                            currentPlayerPoller.scheduleAtFixedRate(() -> {
                                String dbCurrentPlayer = ThreadLobbyManager.getCurrentPlayer(lobbyCode);
                                String localCurrentPlayer = game.getCurrentPlayer().getName();
                                if (dbCurrentPlayer != null && !dbCurrentPlayer.equals(localCurrentPlayer)) {
                                    Platform.runLater(() -> {
                                        game.setCurrentPlayer(dbCurrentPlayer);
                                        if (gameUIController != null) gameUIController.updateUI();
                                    });
                                }
                            }, 0, 1, TimeUnit.SECONDS);

                            System.out.println("[DEBUG] Joiner polling setup complete");
                        } catch (Exception e) {
                            System.err.println("[DEBUG] Error setting up joiner game: " + e.getMessage());
                            e.printStackTrace();
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
                        // Decrement player_count in lobbies
                        String updateCountSQL = "UPDATE lobbies SET player_count = player_count - 1 WHERE lobby_code = ? AND player_count > 0";
                        try (PreparedStatement updateStmt = conn.prepareStatement(updateCountSQL)) {
                            updateStmt.setString(1, lobbyCode);
                            updateStmt.executeUpdate();
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