package com.example.javarice_capstone.javarice_capstone.data;

public class GameScore implements SerializableGameData {
    private int id;
    @Override
    public int getId() {
        return id;
    }

    private int score;
    private GameRound game;
}
