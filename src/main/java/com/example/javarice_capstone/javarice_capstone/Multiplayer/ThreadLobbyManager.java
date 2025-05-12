package com.example.javarice_capstone.javarice_capstone.Multiplayer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class ThreadLobbyManager {
    private static final String DB_URL = "jdbc:mysql://" + SessionState.LobbyConnection + "/game_data?useSSL=false&connectTimeout=10000";
    private static final String USER = "root";
    private static final String PASS = "";

    public static class PlayerInfo {
        public final String name;
        public final boolean isHost;

        public PlayerInfo(String name, boolean isHost) {
            this.name = name;
            this.isHost = isHost;
        }
    }

    public static List<PlayerInfo> getPlayersInLobby(String lobbyCode) {
        List<PlayerInfo> players = new ArrayList<>();

        String query = "SELECT player, host FROM players_in_lobbies WHERE lobby_code = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, lobbyCode);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                String name = rs.getString("player");
                boolean isHost = rs.getBoolean("host");
                players.add(new PlayerInfo(name, isHost));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return players;
    }
}

