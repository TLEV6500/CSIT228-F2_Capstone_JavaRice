package com.example.javarice_capstone.javarice_capstone.datatypes;

import java.lang.reflect.Array;
import java.sql.JDBCType;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLType;

public class Player implements SerializableGameData {
    private int id;
    private String name;
    private GameScore highestScore;
    private final String[] dataFields = new String[]{"name", "highestScore"};

    Player() {
        id = getNextId();
    }
    Player(int id) { this.id = id;}

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
        return dataFields;
    }
}
