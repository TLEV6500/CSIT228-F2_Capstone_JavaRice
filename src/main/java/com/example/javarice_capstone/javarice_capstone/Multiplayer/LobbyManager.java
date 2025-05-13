package com.example.javarice_capstone.javarice_capstone.Multiplayer;

import java.sql.*;
import java.util.function.Supplier;
import javafx.scene.control.Alert;
import javafx.application.Platform;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import javafx.stage.Modality;

public class LobbyManager {
    private static final String dbName = "game_data";
    private static final String dbUser = "root";
    private static final String dbPass = "";

    // Method to create a new lobby
    public static String createLobby(String lobbyCode) {
        CreateDatabaseIfNeeded();

        String url = "jdbc:mysql://" + SessionState.LobbyConnection + "/" + dbName + "?useSSL=false&connectTimeout=10000";

        try (Connection conn = DriverManager.getConnection(url, dbUser, dbPass)) {
            conn.setAutoCommit(false); // Start transaction
            conn.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE); // Add proper isolation

            try {
                // Check if the lobby already exists
                String checkLobbySQL = "SELECT 1 FROM lobbies WHERE lobby_code = ?";
                try (PreparedStatement ps = conn.prepareStatement(checkLobbySQL)) {
                    ps.setString(1, lobbyCode);
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        System.out.println("⚠️ Lobby already exists: " + lobbyCode);
                        conn.rollback();
                        return null;
                    }
                }

                // Insert into lobbies
                String insertLobbySQL = "INSERT INTO lobbies (lobby_code, status) VALUES (?, 'waiting')";
                try (PreparedStatement ps = conn.prepareStatement(insertLobbySQL)) {
                    ps.setString(1, lobbyCode);
                    ps.executeUpdate();
                }

                // Insert into players_in_lobbies
                String insertPlayerSQL = "INSERT INTO players_in_lobbies (lobby_code) VALUES (?)";
                try (PreparedStatement ps = conn.prepareStatement(insertPlayerSQL)) {
                    ps.setString(1, lobbyCode);
                    ps.executeUpdate();
                }

                conn.commit(); // Commit transaction
                System.out.println("✅ Lobby and player entry created for: " + lobbyCode);
                return lobbyCode;

            } catch (SQLException e) {
                conn.rollback(); // Rollback on error
                System.err.println("❌ Transaction failed. Rolling back.");
                handleDatabaseError(e);
                return null;
            }

        } catch (SQLException e) {
            System.err.println("❌ DB Connection failed.");
            handleDatabaseError(e);
            return null;
        }
    }

    private static void handleDatabaseError(SQLException e) {
        String errorMessage;
        switch (e.getErrorCode()) {
            case 1062: // Duplicate entry
                errorMessage = "Lobby already exists. Please try a different lobby code.";
                break;
            case 2003: // Connection refused
                errorMessage = "Could not connect to the database server. Please check your connection.";
                break;
            case 1045: // Access denied
                errorMessage = "Database access denied. Please check your credentials.";
                break;
            default:
                errorMessage = "An unexpected database error occurred: " + e.getMessage();
        }
        showError("Database Error", errorMessage);
        e.printStackTrace();
    }

    private static void showError(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    public static boolean assignHost(String lobbyCode, String hostPlayer) {
        String url = "jdbc:mysql://" + SessionState.LobbyConnection + "/" + dbName + "?useSSL=false&connectTimeout=10000";

        try (Connection conn = DriverManager.getConnection(url, dbUser, dbPass)) {
            conn.setAutoCommit(false); // Begin transaction

            try {
                // Update host_player in lobbies
                String updateLobbySQL = "UPDATE lobbies SET host_player = ? WHERE lobby_code = ?";
                try (PreparedStatement ps = conn.prepareStatement(updateLobbySQL)) {
                    ps.setString(1, hostPlayer);
                    ps.setString(2, lobbyCode);
                    if (ps.executeUpdate() == 0) throw new SQLException("Lobby update failed");
                }

                // Update players_in_lobbies
                String updatePlayersSQL = "UPDATE players_in_lobbies SET player = ?, host = ? WHERE lobby_code = ?";
                try (PreparedStatement ps = conn.prepareStatement(updatePlayersSQL)) {
                    ps.setString(1, hostPlayer);
                    ps.setBoolean(2, true);
                    ps.setString(3, lobbyCode);
                    if (ps.executeUpdate() == 0) throw new SQLException("Players update failed");
                }

                conn.commit(); // Commit if all successful
                System.out.println("✅ Host assigned to lobby: " + hostPlayer);
                return true;

            } catch (SQLException e) {
                conn.rollback(); // Rollback if any error
                System.err.println("❌ Failed to assign host, transaction rolled back.");
                e.printStackTrace();
                return false;
            }

        } catch (SQLException e) {
            System.err.println("❌ DB connection failed.");
            e.printStackTrace();
            return false;
        }
    }


    public static boolean deleteLobby(String lobbyCode) {
        String url = "jdbc:mysql://" + SessionState.LobbyConnection + "/" + dbName + "?useSSL=false";

        try (Connection conn = DriverManager.getConnection(url, dbUser, dbPass)) {
            conn.setAutoCommit(false); // Start transaction

            try {
                // First, delete from players_in_lobbies
                String deletePlayersSQL = "DELETE FROM players_in_lobbies WHERE lobby_code = ?";
                try (PreparedStatement ps = conn.prepareStatement(deletePlayersSQL)) {
                    ps.setString(1, lobbyCode);
                    ps.executeUpdate();
                }

                // Then delete from lobbies
                String deleteLobbySQL = "DELETE FROM lobbies WHERE lobby_code = ?";
                try (PreparedStatement ps = conn.prepareStatement(deleteLobbySQL)) {
                    ps.setString(1, lobbyCode);
                    int affected = ps.executeUpdate();
                    if (affected == 0) {
                        conn.rollback();
                        System.err.println("⚠️ Lobby not found: " + lobbyCode);
                        return false;
                    }
                }

                conn.commit(); // All successful
                System.out.println("🗑️ Lobby deleted: " + lobbyCode);
                return true;

            } catch (SQLException e) {
                conn.rollback();
                System.err.println("❌ Failed to delete lobby. Transaction rolled back.");
                e.printStackTrace();
                return false;
            }

        } catch (SQLException e) {
            System.err.println("❌ DB connection failed.");
            e.printStackTrace();
            return false;
        }
    }


    public static void CreateDatabaseIfNeeded() {
        String baseUrl = "jdbc:mysql://" + SessionState.LobbyConnection + "/?useSSL=false&connectTimeout=10000";

        try (Connection conn = DriverManager.getConnection(baseUrl, dbUser, dbPass);
             Statement stmt = conn.createStatement()) {

            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS " + dbName);
            System.out.println("✅ Database ensured: " + dbName);

            // Initialize all tables using the new approach
            InitializeDatabase.InitializeAllTables();
            System.out.println("✅ All tables initialized successfully");

        } catch (SQLException e) {
            System.err.println("❌ Failed to initialize database.");
            e.printStackTrace();
        }
    }


    public static void waitFor(Supplier<Boolean> condition) throws InterruptedException {
        int retries = 10;
        while (retries-- > 0) {
            if (condition.get()) return;
            Thread.sleep(300); // short wait before retry
        }
        throw new RuntimeException("Timeout waiting for condition");
    }

    public static boolean checkIfTableExists(String tableName) {
        try (Connection conn = DriverManager.getConnection(
                "jdbc:mysql://" + SessionState.LobbyConnection + "/" + dbName + "?useSSL=false",
                dbUser, dbPass);
             ResultSet rs = conn.getMetaData().getTables(null, null, tableName, null)) {
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }


}
