package com.example.javarice_capstone.javarice_capstone.Multiplayer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class InitializeDatabase {
    private static final String SQLConnection = "jdbc:mysql://" + SessionState.LobbyConnection + "/game_data?useSSL=false&connectTimeout=10000";
    public static void InitializeGameMoves(){
        try (Connection initConn = DriverManager.getConnection(SQLConnection, "root", "");
             Statement Stmt = initConn.createStatement()) {
            Stmt.executeUpdate("CREATE TABLE game_moves ("
                    + "move_id INT AUTO_INCREMENT PRIMARY KEY,"
                    + "lobby_id INT NOT NULL,"
                    + "player_id INT NOT NULL,"
                    + "card_played VARCHAR(50),"
                    + "turn_number INT,"
                    + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
                    + "FOREIGN KEY (lobby_id) REFERENCES lobbies(lobby_id) ON DELETE CASCADE,"
                    + "FOREIGN KEY (player_id) REFERENCES players(player_id) ON DELETE CASCADE"
            );
        } catch (Exception e) {
            System.err.println("❌ Failed to create or connect to database.");
            e.printStackTrace();
        }
    }

    public static void InitializePlayers(){
        try (Connection initConn = DriverManager.getConnection(SQLConnection, "root", "");
             Statement Stmt = initConn.createStatement()) {
            Stmt.executeUpdate("CREATE TABLE IF NOT EXISTS players_in_lobbies ("
                    + "id INT AUTO_INCREMENT PRIMARY KEY,"
                    + "lobby_code VARCHAR(10), "
                    + "player VARCHAR(100), "
                    + "host BOOL DEFAULT FALSE, "
                    + "is_ready BOOL DEFAULT FALSE)"
            );
        } catch (Exception e) {
            System.err.println("❌ Failed to create or connect to database.");
            e.printStackTrace();
        }
    }

    public static void InitializeLobbies(){
        try (Connection initConn = DriverManager.getConnection(SQLConnection, "root", "");
             Statement Stmt = initConn.createStatement()) {
            Stmt.executeUpdate("CREATE TABLE IF NOT EXISTS lobbies ("
                    + "lobby_code VARCHAR(10) PRIMARY KEY, "
                    + "host_player VARCHAR(100), "
                    + "status VARCHAR(20) DEFAULT 'waiting')"
            );
        } catch (Exception e) {
            System.err.println("❌ Failed to create or connect to database.");
            e.printStackTrace();
        }
    }
}
