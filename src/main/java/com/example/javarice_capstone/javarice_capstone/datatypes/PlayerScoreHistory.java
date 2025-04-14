package com.example.javarice_capstone.javarice_capstone.data;

public class PlayerScoreHistory implements SerializableGameData {
    private int id;
    private GameScore[] history;

    @Override
    public int getId() {
        return id;
    }
}
