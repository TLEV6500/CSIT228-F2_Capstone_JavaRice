package com.example.javarice_capstone.javarice_capstone.Multiplayer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class ThreadLobbyManager {
    private static final String DB_URL = "jdbc:mysql://" + SessionState.LobbyConnection + "/game_data?useSSL=false&connectTimeout=10000";
    private static final String USER = "root";
    private static final String PASS = "";
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public static class PlayerInfo {
        public final String name;
        public final boolean isHost;
        public final boolean isReady;

        public PlayerInfo(String name, boolean isHost, boolean isReady) {
            this.name = name;
            this.isHost = isHost;
            this.isReady = isReady;
        }
    }

    public static List<PlayerInfo> getPlayersInLobby(String lobbyCode) {
        List<PlayerInfo> players = new ArrayList<>();

        String query = "SELECT player, host, is_ready FROM players_in_lobbies WHERE lobby_code = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, lobbyCode);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                String name = rs.getString("player");
                boolean isHost = rs.getBoolean("host");
                boolean isReady = rs.getBoolean("is_ready");
                players.add(new PlayerInfo(name, isHost, isReady));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return players;
    }

    public static boolean isLobbyActive(String lobbyCode) {
        String query = "SELECT status FROM lobbies WHERE lobby_code = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, lobbyCode);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String status = rs.getString("status");
                return "waiting".equalsIgnoreCase(status);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void startLobbyUpdates(String lobbyCode, Consumer<List<PlayerInfo>> onUpdate) {
        scheduler.scheduleAtFixedRate(() -> {
            if (!isLobbyActive(lobbyCode)) {
                scheduler.shutdown();
                return;
            }
            
            List<PlayerInfo> players = getPlayersInLobby(lobbyCode);
            onUpdate.accept(players);
        }, 0, 1, TimeUnit.SECONDS);
    }

    public static void stopLobbyUpdates() {
        scheduler.shutdown();
    }

    public static boolean updatePlayerReadyStatus(String lobbyCode, String playerName, boolean isReady) {
        String query = "UPDATE players_in_lobbies SET is_ready = ? WHERE lobby_code = ? AND player = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setBoolean(1, isReady);
            ps.setString(2, lobbyCode);
            ps.setString(3, playerName);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean areAllPlayersReady(String lobbyCode) {
        String query = "SELECT COUNT(*) as total, SUM(CASE WHEN is_ready = 1 THEN 1 ELSE 0 END) as ready " +
                      "FROM players_in_lobbies WHERE lobby_code = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, lobbyCode);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                int total = rs.getInt("total");
                int ready = rs.getInt("ready");
                return total > 0 && total == ready;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void broadcastStartGame(String lobbyCode) {
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
            System.err.println("Error broadcasting start game: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static String checkLobbyStatus(String lobbyCode) {
        String query = "SELECT status FROM lobbies WHERE lobby_code = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, lobbyCode);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getString("status");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "unknown";
    }
}

