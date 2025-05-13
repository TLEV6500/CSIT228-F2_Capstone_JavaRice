package com.example.javarice_capstone.javarice_capstone.Multiplayer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class InitializeDatabase {
    private static final String BASE_URL = "jdbc:mysql://" + SessionState.LobbyConnection;
    private static final String DB_NAME = "game_data";
    private static final String CONNECTION_PARAMS = "?useSSL=false&connectTimeout=10000";
    
    public static void InitializeAllTables() {
        // First create the database
        try (Connection initConn = DriverManager.getConnection(BASE_URL + CONNECTION_PARAMS, "root", "");
             Statement Stmt = initConn.createStatement()) {
            Stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS " + DB_NAME);
            System.out.println("✅ Database ensured: " + DB_NAME);
        } catch (Exception e) {
            System.err.println("❌ Failed to create database.");
            e.printStackTrace();
            return;
        }

        // Then initialize tables in the correct order
        try (Connection conn = DriverManager.getConnection(getConnectionString(), "root", "");
             Statement stmt = conn.createStatement()) {
            
            // Select the database
            stmt.executeUpdate("USE " + DB_NAME);
            
            // Create lobbies table first
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS lobbies ("
                    + "lobby_code VARCHAR(10) PRIMARY KEY, "
                    + "host_player VARCHAR(100), "
                    + "current_player VARCHAR(100), "
                    + "status VARCHAR(20) DEFAULT 'waiting')"
            );
            System.out.println("✅ Table created: lobbies");

            // Create players table
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS players_in_lobbies ("
                    + "id INT AUTO_INCREMENT PRIMARY KEY,"
                    + "lobby_code VARCHAR(10), "
                    + "player VARCHAR(100), "
                    + "host BOOL DEFAULT FALSE, "
                    + "is_ready BOOL DEFAULT FALSE, "
                    + "FOREIGN KEY (lobby_code) REFERENCES lobbies(lobby_code) ON DELETE CASCADE)"
            );
            System.out.println("✅ Table created: players_in_lobbies");

            // Create game_cards table
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS game_cards ("
                    + "id INT AUTO_INCREMENT PRIMARY KEY,"
                    + "lobby_code VARCHAR(10), "
                    + "player_name VARCHAR(100), "
                    + "card_value VARCHAR(10), "
                    + "card_color VARCHAR(10), "
                    + "card_type VARCHAR(20), "
                    + "is_played BOOL DEFAULT FALSE, "
                    + "play_order INT, "
                    + "FOREIGN KEY (lobby_code) REFERENCES lobbies(lobby_code) ON DELETE CASCADE)"
            );
            System.out.println("✅ Table created: game_cards");

            // Create game_moves table
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS game_moves ("
                    + "move_id INT AUTO_INCREMENT PRIMARY KEY,"
                    + "lobby_code VARCHAR(10),"
                    + "player_name VARCHAR(100),"
                    + "card_played VARCHAR(50),"
                    + "turn_number INT,"
                    + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
                    + "FOREIGN KEY (lobby_code) REFERENCES lobbies(lobby_code) ON DELETE CASCADE)"
            );
            System.out.println("✅ Table created: game_moves");

        } catch (Exception e) {
            System.err.println("❌ Failed to create tables.");
            e.printStackTrace();
        }
    }

    private static String getConnectionString() {
        return BASE_URL + "/" + DB_NAME + CONNECTION_PARAMS;
    }
}
