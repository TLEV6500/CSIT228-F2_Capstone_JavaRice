package com.example.javarice_capstone.javarice_capstone.Multiplayer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class JoinLobby {
    private static final String dbName = "game_data";
    private static final String dbUser = "root";
    private static final String dbPass = "";

    // Method to join an existing lobby
    public static String joinLobby(String playerName, String lobbyAddress,String lobbyCode) {
        try {
            // Construct the JDBC URL for Remote.it connection
            String url = "jdbc:mysql://" + SessionState.LobbyConnection + "/" + dbName + "?useSSL=false";
            try (Connection conn = DriverManager.getConnection(url, dbUser, dbPass);
                 Statement stmt = conn.createStatement()) {

                // Step 1: Check if the lobby exists
                String checkLobbySQL = "SELECT * FROM lobbies WHERE lobby_code = ?";
                try (PreparedStatement ps = conn.prepareStatement(checkLobbySQL)) {
                    ps.setString(1, lobbyCode);
                    ResultSet rs = ps.executeQuery();
                    if (!rs.next()) {
                        return "Lobby with the provided code does not exist.";
                    }

                    // Step 2: Check if the lobby has already started or is still in "waiting" status
                    String lobbyStatus = rs.getString("status");
                    if ("started".equals(lobbyStatus)) {
                        return "The game has already started. Cannot join.";
                    }

                    // Step 3: Update the lobby status and add the new player
                    String updatePlayerSQL = "INSERT INTO players_in_lobby (lobby_code, player_name) VALUES (?, ?)";
                    try (PreparedStatement psInsert = conn.prepareStatement(updatePlayerSQL)) {
                        psInsert.setString(1, lobbyCode);
                        psInsert.setString(2, playerName);
                        psInsert.executeUpdate();
                    }

                    // Step 4: Optionally, update the lobby status if the lobby reaches a certain number of players
                    // Step 4: Check number of players before starting the game
                    String countPlayersSQL = "SELECT COUNT(*) AS player_count FROM players_in_lobby WHERE lobby_code = ?";
                    try (PreparedStatement psCount = conn.prepareStatement(countPlayersSQL)) {
                        psCount.setString(1, lobbyCode);
                        ResultSet rsCount = psCount.executeQuery();
                        if (rsCount.next()) {
                            int playerCount = rsCount.getInt("player_count");
                            if (playerCount >= 2) { // Only start if there are 2 or more players
                                String updateLobbyStatusSQL = "UPDATE lobbies SET status = 'started' WHERE lobby_code = ? AND status = 'waiting'";
                                try (PreparedStatement psUpdateStatus = conn.prepareStatement(updateLobbyStatusSQL)) {
                                    psUpdateStatus.setString(1, lobbyCode);
                                    psUpdateStatus.executeUpdate();
                                }
                            }
                        }
                    }


                    return "Player " + playerName + " successfully joined the lobby with code " + lobbyCode;
                }

            }
        } catch (Exception e) {
            System.err.println("⚠️ Failed to join the lobby:");
            e.printStackTrace();
            return "An error occurred while joining the lobby.";
        }
    }
}
