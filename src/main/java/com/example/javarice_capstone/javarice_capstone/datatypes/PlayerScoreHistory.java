package com.example.javarice_capstone.javarice_capstone.datatypes;

public class PlayerScoreHistory implements SerializableGameData {
    private int id;
    private GameScore[] history;

    PlayerScoreHistory() {
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
}
