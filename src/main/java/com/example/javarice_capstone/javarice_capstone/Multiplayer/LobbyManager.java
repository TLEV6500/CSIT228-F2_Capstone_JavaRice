package com.example.javarice_capstone.javarice_capstone.Multiplayer;

import java.sql.*;

public class LobbyManager {
    private static final String dbName = "game_data";
    private static final String dbUser = "root";
    private static final String dbPass = "";

    // Method to create a new lobby
    public static String createLobby(String lobbyCode) {
        try {
            // Same setup ...
            String url = "jdbc:mysql://" + SessionState.LobbyConnection + "/" + dbName + "?useSSL=false";
            try (Connection conn = DriverManager.getConnection(url, dbUser, dbPass);
                 Statement stmt = conn.createStatement()) {

                stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS " + dbName);

                ensureLobbiesTable(conn);
                ensurePlayersInLobbyTable(conn);

                // Check if lobby already exists
                String checkLobbySQL = "SELECT * FROM lobbies WHERE lobby_code = ?";
                try (PreparedStatement ps = conn.prepareStatement(checkLobbySQL)) {
                    ps.setString(1, lobbyCode);
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) return null; // already exists
                }

                // Insert new lobby
                String createLobbySQL = "INSERT INTO lobbies (lobby_code, status) VALUES (?, 'waiting')";
                try (PreparedStatement psInsert = conn.prepareStatement(createLobbySQL)) {
                    psInsert.setString(1, lobbyCode);
                    psInsert.executeUpdate();
                }

                return lobbyCode;

            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    // Ensure 'lobbies' table exists
    private static void ensureLobbiesTable(Connection conn) {
        String createTableSQL = """
            CREATE TABLE IF NOT EXISTS lobbies (
                lobby_code VARCHAR(50) PRIMARY KEY,
                status VARCHAR(20) DEFAULT 'waiting',
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """;

        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(createTableSQL);
        } catch (Exception e) {
            System.err.println("⚠️ Failed to create lobbies table:");
            e.printStackTrace();
        }
    }

    // Ensure 'players_in_lobby' table exists
    private static void ensurePlayersInLobbyTable(Connection conn) {
        String createTableSQL = """
        CREATE TABLE IF NOT EXISTS players_in_lobby (
            id INT PRIMARY KEY AUTO_INCREMENT,
            lobby_code VARCHAR(50) NOT NULL,
            player_name VARCHAR(100) NOT NULL,
            FOREIGN KEY (lobby_code) REFERENCES lobbies(lobby_code) ON DELETE CASCADE
        )
    """;

        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(createTableSQL);
        } catch (Exception e) {
            System.err.println("⚠️ Failed to create players_in_lobby table:");
            e.printStackTrace();
        }
    }

    public static boolean assignHost(String lobbyCode, String hostPlayer) {
        try {
            String url = "jdbc:mysql://" + SessionState.LobbyConnection + "/" + dbName + "?useSSL=false";
            try (Connection conn = DriverManager.getConnection(url, dbUser, dbPass)) {
                String updateSQL = "UPDATE lobbies SET host_player = ? WHERE lobby_code = ?";
                try (PreparedStatement ps = conn.prepareStatement(updateSQL)) {
                    ps.setString(1, hostPlayer);
                    ps.setString(2, lobbyCode);
                    return ps.executeUpdate() > 0;
                }
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
}
