package com.example.javarice_capstone.javarice_capstone.Multiplayer;

import java.sql.*;

public class LobbyManager {
    private static final String dbName = "game_data";
    private static final String dbUser = "root";
    private static final String dbPass = "";

    // Method to create a new lobby
    public static String createLobby(String lobbyCode) {
        CreateDatabase();
        // Connect to the target database
        String url = "jdbc:mysql://" + SessionState.LobbyConnection + "/" + dbName + "?useSSL=false&connectTimeout=10000";
        try (Connection conn = DriverManager.getConnection(url, dbUser, dbPass)) {
            // Check if the lobby already exists
            String checkLobbySQL = "SELECT 1 FROM lobbies WHERE lobby_code = ?";
            try (PreparedStatement ps = conn.prepareStatement(checkLobbySQL)) {
                ps.setString(1, lobbyCode);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    System.out.println("⚠️ Lobby already exists: " + lobbyCode);
                    return null;
                }
            }
            Thread.sleep(500);
            // Create the new lobby
            String createLobbySQL = "INSERT INTO lobbies (lobby_code, status) VALUES (?, 'waiting')";
            try (PreparedStatement ps = conn.prepareStatement(createLobbySQL)) {
                ps.setString(1, lobbyCode);
                ps.executeUpdate();
                System.out.println("✅ Lobby created: " + lobbyCode);
                Thread.sleep(500);
            }

            String createPlayersInLobbySQL = "INSERT INTO players_in_lobbies (lobby_code) VALUES (?)";
            try (PreparedStatement ps = conn.prepareStatement(createPlayersInLobbySQL)) {
                ps.setString(1, lobbyCode);
                ps.executeUpdate();
                System.out.println("✅ Players in Lobby created: " + lobbyCode);
                Thread.sleep(500);
            }

            return lobbyCode;

        } catch (Exception e) {
            System.err.println("❌ Failed to create lobby.");
            e.printStackTrace();
            return null;
        }
    }

    public static boolean assignHost(String lobbyCode, String hostPlayer) {
        System.out.println("Connecting to: jdbc:mysql://" + SessionState.LobbyConnection + "/" + dbName);

        try {
            String url = "jdbc:mysql://" + SessionState.LobbyConnection + "/" + dbName + "?useSSL=false&connectTimeout=10000";
            try (Connection conn = DriverManager.getConnection(url, dbUser, dbPass)) {
                Thread.sleep(500);
                // Update host_player in lobbies
                String updateLobbySQL = "UPDATE lobbies SET host_player = ? WHERE lobby_code = ?";
                boolean lobbyUpdated = false;
                try (PreparedStatement ps = conn.prepareStatement(updateLobbySQL)) {
                    ps.setString(1, hostPlayer);
                    ps.setString(2, lobbyCode);
                    lobbyUpdated = ps.executeUpdate() > 0;
                }
                Thread.sleep(500);
                // Update player and host in players_in_lobbies
                String updatePlayersInLobbiesSQL = "UPDATE players_in_lobbies SET player = ?, host = ? WHERE lobby_code = ?";
                boolean playerUpdated = false;
                try (PreparedStatement ps = conn.prepareStatement(updatePlayersInLobbiesSQL)) {
                    ps.setString(1, hostPlayer);
                    ps.setBoolean(2, true);
                    ps.setString(3, lobbyCode);
                    playerUpdated = ps.executeUpdate() > 0;
                }
                Thread.sleep(500);
                return lobbyUpdated && playerUpdated;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean deleteLobby(String lobbyCode) {
        try {
            // Connect to your DB and execute delete query
            String url = "jdbc:mysql://" + SessionState.LobbyConnection + "/" + dbName + "?useSSL=false";
            Connection conn = DriverManager.getConnection(url, dbUser, dbPass);
            PreparedStatement stmt = conn.prepareStatement("DELETE FROM lobbies WHERE lobby_code = ?");
            stmt.setString(1, lobbyCode);
            int rows = stmt.executeUpdate();
            stmt.close();
            conn.close();
            return rows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void CreateDatabase(){
        try (Connection initConn = DriverManager.getConnection(
                "jdbc:mysql://" + SessionState.LobbyConnection + "/?useSSL=false&connectTimeout=10000", dbUser, dbPass);
             Statement initStmt = initConn.createStatement()) {
            initStmt.executeUpdate("CREATE DATABASE IF NOT EXISTS " + dbName);
            System.out.println("✅ Database ensured: " + dbName);
            InitializeDatabase.InitializeLobbies();
            InitializeDatabase.InitializePlayers();
        } catch (Exception e) {
            System.err.println("❌ Failed to create or connect to database.");
            e.printStackTrace();
        }
    }
}
