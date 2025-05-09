package com.example.javarice_capstone.javarice_capstone.datatypes;

public class GameScore implements SerializableGameData {
    private int id;
    private int score;
    private GameRound game;

    GameScore() {
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
        return new String[] { "id", "score", "game" };
    }
}