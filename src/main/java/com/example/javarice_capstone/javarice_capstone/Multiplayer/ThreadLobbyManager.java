package com.example.javarice_capstone.javarice_capstone.Multiplayer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import com.example.javarice_capstone.javarice_capstone.enums.Colors;

public class ThreadLobbyManager {
    private static final String DB_URL = "jdbc:mysql://" + SessionState.LobbyConnection + "/game_data?useSSL=false&connectTimeout=10000";
    private static final String USER = "root";
    private static final String PASS = "";
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public static class PlayerInfo {
        public final String name;
        public final boolean isHost;
        public final boolean isReady;
        public final int handSize;

        public PlayerInfo(String name, boolean isHost, boolean isReady, int handSize) {
            this.name = name;
            this.isHost = isHost;
            this.isReady = isReady;
            this.handSize = handSize;
        }
    }

    public static List<PlayerInfo> getPlayersInLobby(String lobbyCode) {
        List<PlayerInfo> players = new ArrayList<>();

        String query = "SELECT player, host, is_ready, player_cards FROM players_in_lobbies WHERE lobby_code = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, lobbyCode);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                String name = rs.getString("player");
                boolean isHost = rs.getBoolean("host");
                boolean isReady = rs.getBoolean("is_ready");
                int handSize = rs.getInt("player_cards");
                players.add(new PlayerInfo(name, isHost, isReady, handSize));
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

    public static void pushDiscardPile(String lobbyCode, String cardInfo) {
        String query = "UPDATE lobbies SET discard_pile = ? WHERE lobby_code = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, cardInfo);
            ps.setString(2, lobbyCode);
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String fetchDiscardPile(String lobbyCode) {
        String query = "SELECT discard_pile FROM lobbies WHERE lobby_code = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, lobbyCode);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getString("discard_pile");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void updatePlayerHandSize(String lobbyCode, String playerName, int handSize) {
        String query = "UPDATE players_in_lobbies SET player_cards = ? WHERE lobby_code = ? AND player = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, String.valueOf(handSize));
            ps.setString(2, lobbyCode);
            ps.setString(3, playerName);
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void recordGameMove(String lobbyCode, String playerName, String cardPlayed, String action, int turnNumber) {
        String query = "INSERT INTO game_moves (lobby_code, player_name, card_played, action, turn_number) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, lobbyCode);
            ps.setString(2, playerName);
            ps.setString(3, cardPlayed);
            ps.setString(4, action);
            ps.setInt(5, turnNumber);
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static class MoveInfo {
        public final String playerName;
        public final String cardPlayed;
        public final String action;
        public final int turnNumber;
        public MoveInfo(String playerName, String cardPlayed, String action, int turnNumber) {
            this.playerName = playerName;
            this.cardPlayed = cardPlayed;
            this.action = action;
            this.turnNumber = turnNumber;
        }
    }

    public static List<MoveInfo> getGameMoves(String lobbyCode) {
        List<MoveInfo> moves = new ArrayList<>();
        String query = "SELECT player_name, card_played, action, turn_number FROM game_moves WHERE lobby_code = ? ORDER BY turn_number ASC";
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, lobbyCode);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String playerName = rs.getString("player_name");
                String cardPlayed = rs.getString("card_played");
                String action = rs.getString("action");
                int turnNumber = rs.getInt("turn_number");
                moves.add(new MoveInfo(playerName, cardPlayed, action, turnNumber));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return moves;
    }

    public static String getHostPlayerName(String lobbyCode) {
        String query = "SELECT host_player FROM lobbies WHERE lobby_code = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, lobbyCode);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("host_player");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void setCurrentPlayer(String lobbyCode, String playerName) {
        String query = "UPDATE lobbies SET current_player = ? WHERE lobby_code = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, playerName);
            ps.setString(2, lobbyCode);
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getCurrentPlayer(String lobbyCode) {
        String query = "SELECT current_player FROM lobbies WHERE lobby_code = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, lobbyCode);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("current_player");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getLastGameMove(String lobbyCode) {
        String query = "SELECT action FROM game_moves WHERE lobby_code = ? ORDER BY turn_number DESC LIMIT 1";
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, lobbyCode);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getString("action");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    
    public static void updateGameDirection(String lobbyCode, boolean isClockwise) {
        String query = "UPDATE lobbies SET game_direction = ? WHERE lobby_code = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, isClockwise ? "clockwise" : "counterclockwise");
            ps.setString(2, lobbyCode);
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean getGameDirection(String lobbyCode) {
        String query = "SELECT game_direction FROM lobbies WHERE lobby_code = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, lobbyCode);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String direction = rs.getString("game_direction");
                return "clockwise".equalsIgnoreCase(direction);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true; // Default to clockwise if there's an error
    }

    public static void markForcedDrawHandled(String lobbyCode, int turnNumber) {
        String query = "UPDATE game_moves SET forced_draw_handled = TRUE WHERE lobby_code = ? AND turn_number = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, lobbyCode);
            ps.setInt(2, turnNumber);
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean isForcedDrawHandled(String lobbyCode, int turnNumber) {
        String query = "SELECT forced_draw_handled FROM game_moves WHERE lobby_code = ? AND turn_number = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, lobbyCode);
            ps.setInt(2, turnNumber);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getBoolean("forced_draw_handled");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void updateCurrentColor(String lobbyCode, Colors color) {
        String sql = "UPDATE lobbies SET current_color = ? WHERE lobby_code = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             java.sql.PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, color.name());
            pstmt.setString(2, lobbyCode);
            pstmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Colors getCurrentColor(String lobbyCode) {
        String sql = "SELECT current_color FROM lobbies WHERE lobby_code = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             java.sql.PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, lobbyCode);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String colorStr = rs.getString("current_color");
                    if (colorStr != null) {
                        return Colors.valueOf(colorStr);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Colors.WILD; // Default fallback
    }
}

