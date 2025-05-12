package com.example.javarice_capstone.javarice_capstone.Multiplayer;

import java.sql.*;

public class JoinLobby {
    private static final String dbName = "game_data";
    private static final String dbUser = "root";
    private static final String dbPass = "";

    public static String joinLobby(String playerName, String lobbyCode) {
        String url = "jdbc:mysql://" + SessionState.LobbyConnection + "/" + dbName + "?useSSL=false";

        try (Connection conn = DriverManager.getConnection(url, dbUser, dbPass)) {
            conn.setAutoCommit(false); // Begin transaction

            // Step 1: Check if lobby exists and is joinable
            String checkLobbySQL = "SELECT status FROM lobbies WHERE lobby_code = ?";
            try (PreparedStatement checkStmt = conn.prepareStatement(checkLobbySQL)) {
                checkStmt.setString(1, lobbyCode);
                ResultSet rs = checkStmt.executeQuery();

                if (!rs.next()) {
                    conn.rollback();
                    return "Lobby with the provided code does not exist.";
                }

                String lobbyStatus = rs.getString("status");
                if ("started".equalsIgnoreCase(lobbyStatus)) {
                    conn.rollback();
                    return "The game has already started. Cannot join.";
                }
            }

            // Step 2: Insert player into players_in_lobbies
            String insertPlayerSQL = "INSERT INTO players_in_lobbies (lobby_code, player) VALUES (?, ?)";
            try (PreparedStatement insertStmt = conn.prepareStatement(insertPlayerSQL)) {
                insertStmt.setString(1, lobbyCode);
                insertStmt.setString(2, playerName);
                insertStmt.executeUpdate();
            }

            conn.commit(); // Commit the transaction
            return "✅ Player " + playerName + " successfully joined the lobby with code " + lobbyCode;

        } catch (SQLException e) {
            e.printStackTrace();
            return "❌ An error occurred while joining the lobby.";
        }
    }
}

