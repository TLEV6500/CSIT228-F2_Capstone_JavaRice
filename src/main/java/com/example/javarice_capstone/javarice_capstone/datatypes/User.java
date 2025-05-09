package com.example.javarice_capstone.javarice_capstone.datatypes;

import java.sql.ResultSet;
import java.sql.SQLException;

public class User implements SerializableGameData {
    private int id;
    private String userName;
    private int avatarIconId;
    private Player[] players;
    private String[] dataFields = new String[]{"userName", "avatarIconId", "players"};
    private int userPlayersId;

    User() {
        id = getNextId();
    }

    @Override
    public int getId() {
        return id;
    }
    private static int ID = 0;
    @Override
    public int getNextId() {
        return ID++;
    }

    @Override
    public String[] getDataFields() {
        return new String[0];
    }

    @Override
    public int extractDataFrom(ResultSet resultSet) {
        int count = 0;
        try {
            userName = resultSet.getString("userName");
            avatarIconId = resultSet.getInt("avatarIconId");
//            userPlayersId = resultSet.getInt("userPlayers");
            players = (Player[]) resultSet.getArray("").getArray();
        } catch (SQLException e) {
            System.err.println("Failed to extract data.");
            System.err.println(e.getMessage());
        }
        return count;
    }
}
